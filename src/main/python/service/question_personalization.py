"""
Semantic question personalization for QuizAI.

The module solves two related tasks:
1. Filters out semantically repeated questions inside one quiz.
2. Builds a quiz that stays fresh for all current players by comparing
   candidates against the embeddings of questions they have already answered.
"""

from __future__ import annotations

import argparse
import json
import math
import sys
from copy import deepcopy
from dataclasses import dataclass, field
from pathlib import Path
from statistics import fmean
from typing import Any, Mapping, Protocol, Sequence

QuestionPayload = dict[str, Any]
Embedding = Sequence[float]
PlayerHistoryEmbeddings = Mapping[int, Sequence[Embedding]]

DEFAULT_MODEL_NAME = "sentence-transformers/paraphrase-multilingual-MiniLM-L12-v2"


class TextEmbedder(Protocol):
    def encode(self, texts: Sequence[str]) -> list[list[float]]:
        """Returns one embedding per input string."""


class SentenceTransformerEmbedder:
    def __init__(
        self,
        model_name: str = DEFAULT_MODEL_NAME,
        *,
        local_files_only: bool = False,
    ) -> None:
        try:
            from sentence_transformers import SentenceTransformer
        except ModuleNotFoundError as exc:
            raise RuntimeError(
                "Dependency 'sentence-transformers' is not installed for this Python interpreter. "
                "Install project dependencies with 'python3 -m pip install -r requirements.txt' "
                "or run the script with the interpreter where the package is already installed."
            ) from exc

        try:
            self.model = SentenceTransformer(
                model_name,
                local_files_only=local_files_only,
            )
        except Exception as exc:
            hint = (
                "Download the model in advance or pass a local model path. "
                f"Model: {model_name}"
            )
            raise RuntimeError(hint) from exc

    def encode(self, texts: Sequence[str]) -> list[list[float]]:
        if not texts:
            return []

        normalized_texts = []
        for text in texts:
            normalized = self._normalize_text_input(text)
            if normalized:
                normalized_texts.append(normalized)

        if not normalized_texts:
            return []

        try:
            vectors = self.model.encode(
                normalized_texts,
                convert_to_numpy=True,
                normalize_embeddings=False,
            )
        except TypeError as exc:
            sample_types = [type(text).__name__ for text in texts[:3]]
            raise RuntimeError(
                "sentence-transformers rejected the provided text input. "
                f"Sample input types: {sample_types}. "
                f"Normalized samples: {normalized_texts[:3]}"
            ) from exc

        return [[float(value) for value in row] for row in vectors]

    @staticmethod
    def _normalize_text_input(text: Any) -> str:
        if text is None:
            return ""
        if isinstance(text, str):
            return SentenceTransformerEmbedder._sanitize_text(text)
        if isinstance(text, (list, tuple)):
            flattened_parts = [
                SentenceTransformerEmbedder._sanitize_text(str(item).strip())
                for item in text
                if item is not None and str(item).strip()
            ]
            return SentenceTransformerEmbedder._sanitize_text(" ".join(flattened_parts))
        return SentenceTransformerEmbedder._sanitize_text(str(text))

    @staticmethod
    def _sanitize_text(text: str) -> str:
        cleaned = "".join(
            char for char in text
            if not 0xD800 <= ord(char) <= 0xDFFF
        )
        return " ".join(cleaned.split())


@dataclass
class CandidateDecision:
    candidate_index: int
    question_text: str
    decision: str
    score: float
    mean_novelty: float
    min_novelty: float
    max_history_similarity: float
    max_quiz_similarity: float
    reason: str | None = None
    conflicting_player_ids: list[int] = field(default_factory=list)
    conflicting_question: str | None = None
    source_question_id: int | None = None

    def to_dict(self) -> dict[str, Any]:
        return {
            "candidate_index": self.candidate_index,
            "question_text": self.question_text,
            "decision": self.decision,
            "score": round(self.score, 6),
            "mean_novelty": round(self.mean_novelty, 6),
            "min_novelty": round(self.min_novelty, 6),
            "max_history_similarity": round(self.max_history_similarity, 6),
            "max_quiz_similarity": round(self.max_quiz_similarity, 6),
            "reason": self.reason,
            "conflicting_player_ids": self.conflicting_player_ids,
            "conflicting_question": self.conflicting_question,
            "source_question_id": self.source_question_id,
        }


@dataclass
class PersonalizationResult:
    requested_questions: int
    selected_questions: list[QuestionPayload]
    selected_decisions: list[CandidateDecision]
    rejected_candidates: list[CandidateDecision]
    backup_candidates: list[CandidateDecision]

    @property
    def regeneration_needed(self) -> int:
        return max(0, self.requested_questions - len(self.selected_questions))

    def to_dict(self) -> dict[str, Any]:
        return {
            "requested_questions": self.requested_questions,
            "selected_count": len(self.selected_questions),
            "regeneration_needed": self.regeneration_needed,
            "selected_questions": self.selected_questions,
            "selected_decisions": [decision.to_dict() for decision in self.selected_decisions],
            "rejected_candidates": [decision.to_dict() for decision in self.rejected_candidates],
            "backup_candidates": [decision.to_dict() for decision in self.backup_candidates],
            "regeneration_context": {
                "selected_question_texts": [
                    question["question_text"]
                    for question in self.selected_questions
                    if question.get("question_text")
                ],
                "banned_question_texts": [
                    decision.question_text for decision in self.rejected_candidates
                ],
            },
        }


@dataclass
class _PreparedCandidate:
    candidate_index: int
    payload: QuestionPayload
    question_text: str
    embedding: list[float]
    mean_novelty: float
    min_novelty: float
    max_history_similarity: float
    conflicting_player_ids: list[int]
    source_question_id: int | None

    @property
    def score(self) -> float:
        # Minimum novelty keeps the quiz fair for the least-covered player.
        return self.min_novelty * 0.65 + self.mean_novelty * 0.35


@dataclass
class _PreparedExistingQuestion:
    question_text: str
    embedding: list[float]


class QuestionPersonalizer:
    def __init__(
        self,
        embedder: TextEmbedder,
        *,
        history_similarity_threshold: float = 0.72,
        quiz_similarity_threshold: float = 0.80,
    ) -> None:
        self.embedder = embedder
        self.history_similarity_threshold = history_similarity_threshold
        self.quiz_similarity_threshold = quiz_similarity_threshold

    def encode_history_texts(
        self,
        player_history_texts: Mapping[int, Sequence[str]],
    ) -> dict[int, list[list[float]]]:
        result: dict[int, list[list[float]]] = {}
        for player_id, texts in player_history_texts.items():
            cleaned_texts = [text.strip() for text in texts if text and text.strip()]
            result[player_id] = self.embedder.encode(cleaned_texts)
        return result

    def select_questions(
        self,
        candidates: Sequence[QuestionPayload | str],
        player_history_embeddings: PlayerHistoryEmbeddings | None = None,
        *,
        target_count: int | None = None,
        existing_quiz_questions: Sequence[QuestionPayload | str] | None = None,
    ) -> PersonalizationResult:
        prepared_existing = self._prepare_existing_questions(existing_quiz_questions or [])
        prepared_candidates = self._prepare_candidates(
            candidates,
            player_history_embeddings or {},
        )
        requested_questions = target_count if target_count is not None else len(prepared_candidates)

        selected_questions: list[QuestionPayload] = []
        selected_decisions: list[CandidateDecision] = []
        rejected_candidates: list[CandidateDecision] = []
        backup_candidates: list[CandidateDecision] = []
        selected_references = list(prepared_existing)

        prepared_candidates.sort(
            key=lambda item: (item.score, item.min_novelty, item.mean_novelty),
            reverse=True,
        )

        for candidate in prepared_candidates:
            max_quiz_similarity, conflicting_question = self._max_similarity_to_questions(
                candidate.embedding,
                selected_references,
            )

            if candidate.conflicting_player_ids:
                rejected_candidates.append(
                    CandidateDecision(
                        candidate_index=candidate.candidate_index,
                        question_text=candidate.question_text,
                        decision="rejected",
                        score=candidate.score,
                        mean_novelty=candidate.mean_novelty,
                        min_novelty=candidate.min_novelty,
                        max_history_similarity=candidate.max_history_similarity,
                        max_quiz_similarity=max_quiz_similarity,
                        reason="similar_to_player_history",
                        conflicting_player_ids=candidate.conflicting_player_ids,
                        source_question_id=candidate.source_question_id,
                    )
                )
                continue

            if max_quiz_similarity >= self.quiz_similarity_threshold:
                rejected_candidates.append(
                    CandidateDecision(
                        candidate_index=candidate.candidate_index,
                        question_text=candidate.question_text,
                        decision="rejected",
                        score=candidate.score,
                        mean_novelty=candidate.mean_novelty,
                        min_novelty=candidate.min_novelty,
                        max_history_similarity=candidate.max_history_similarity,
                        max_quiz_similarity=max_quiz_similarity,
                        reason="similar_to_quiz_question",
                        conflicting_question=conflicting_question,
                        source_question_id=candidate.source_question_id,
                    )
                )
                continue

            decision_name = "selected" if len(selected_questions) < requested_questions else "backup"
            decision = CandidateDecision(
                candidate_index=candidate.candidate_index,
                question_text=candidate.question_text,
                decision=decision_name,
                score=candidate.score,
                mean_novelty=candidate.mean_novelty,
                min_novelty=candidate.min_novelty,
                max_history_similarity=candidate.max_history_similarity,
                max_quiz_similarity=max_quiz_similarity,
                source_question_id=candidate.source_question_id,
            )

            if decision_name == "selected":
                normalized_payload = self._renumber_question(
                    candidate.payload,
                    len(selected_references) + 1,
                )
                selected_questions.append(normalized_payload)
                selected_decisions.append(decision)
                selected_references.append(
                    _PreparedExistingQuestion(
                        question_text=candidate.question_text,
                        embedding=candidate.embedding,
                    )
                )
            else:
                backup_candidates.append(decision)

        return PersonalizationResult(
            requested_questions=requested_questions,
            selected_questions=selected_questions,
            selected_decisions=selected_decisions,
            rejected_candidates=rejected_candidates,
            backup_candidates=backup_candidates,
        )

    def _prepare_existing_questions(
        self,
        questions: Sequence[QuestionPayload | str],
    ) -> list[_PreparedExistingQuestion]:
        cleaned_questions: list[tuple[str, QuestionPayload | str]] = []
        for raw_question in questions:
            text = self._extract_question_text(raw_question)
            if text:
                cleaned_questions.append((text, raw_question))

        embeddings = self.embedder.encode([text for text, _ in cleaned_questions])
        return [
            _PreparedExistingQuestion(question_text=text, embedding=embedding)
            for (text, _), embedding in zip(cleaned_questions, embeddings)
        ]

    def _prepare_candidates(
        self,
        candidates: Sequence[QuestionPayload | str],
        player_history_embeddings: PlayerHistoryEmbeddings,
    ) -> list[_PreparedCandidate]:
        cleaned_candidates: list[tuple[int, QuestionPayload, str]] = []
        for candidate_index, raw_candidate in enumerate(candidates):
            payload = self._normalize_payload(raw_candidate)
            question_text = self._extract_question_text(payload)
            if not question_text:
                continue
            cleaned_candidates.append((candidate_index, payload, question_text))

        embeddings = self.embedder.encode(
            [question_text for _, _, question_text in cleaned_candidates]
        )

        prepared: list[_PreparedCandidate] = []
        for (candidate_index, payload, question_text), embedding in zip(
            cleaned_candidates,
            embeddings,
        ):
            mean_novelty, min_novelty, max_history_similarity, conflicting_player_ids = (
                self._score_against_history(embedding, player_history_embeddings)
            )
            prepared.append(
                _PreparedCandidate(
                    candidate_index=candidate_index,
                    payload=payload,
                    question_text=question_text,
                    embedding=embedding,
                    mean_novelty=mean_novelty,
                    min_novelty=min_novelty,
                    max_history_similarity=max_history_similarity,
                    conflicting_player_ids=conflicting_player_ids,
                    source_question_id=(
                        int(payload.get("id")) if payload.get("id") is not None else None
                    ),
                )
            )
        return prepared

    def _score_against_history(
        self,
        candidate_embedding: Embedding,
        player_history_embeddings: PlayerHistoryEmbeddings,
    ) -> tuple[float, float, float, list[int]]:
        if not player_history_embeddings:
            return 1.0, 1.0, 0.0, []

        novelties: list[float] = []
        conflicting_player_ids: list[int] = []
        max_history_similarity = 0.0

        for player_id, history_embeddings in player_history_embeddings.items():
            player_max_similarity = 0.0
            for history_embedding in history_embeddings:
                similarity = cosine_similarity(candidate_embedding, history_embedding)
                if similarity > player_max_similarity:
                    player_max_similarity = similarity
            novelties.append(max(0.0, 1.0 - player_max_similarity))
            max_history_similarity = max(max_history_similarity, player_max_similarity)
            if player_max_similarity >= self.history_similarity_threshold:
                conflicting_player_ids.append(player_id)

        return fmean(novelties), min(novelties), max_history_similarity, conflicting_player_ids

    def _max_similarity_to_questions(
        self,
        candidate_embedding: Embedding,
        questions: Sequence[_PreparedExistingQuestion],
    ) -> tuple[float, str | None]:
        best_similarity = 0.0
        best_question: str | None = None
        for question in questions:
            similarity = cosine_similarity(candidate_embedding, question.embedding)
            if similarity > best_similarity:
                best_similarity = similarity
                best_question = question.question_text
        return best_similarity, best_question

    @staticmethod
    def _normalize_payload(raw_question: QuestionPayload | str) -> QuestionPayload:
        if isinstance(raw_question, str):
            return {"question_text": raw_question}

        payload = deepcopy(raw_question)
        if "available_answers" not in payload:
            answers = []
            for index in range(1, 5):
                key = f"answer{index}"
                if key in payload:
                    answers.append({"index": index, "answer": payload[key]})
            if answers:
                payload["available_answers"] = answers
        return payload

    @staticmethod
    def _extract_question_text(raw_question: QuestionPayload | str) -> str:
        if isinstance(raw_question, str):
            return " ".join(raw_question.split())
        question_text = raw_question.get("question_text")
        if question_text is None:
            return ""
        return " ".join(str(question_text).split())

    @staticmethod
    def _renumber_question(question: QuestionPayload, question_number: int) -> QuestionPayload:
        payload = deepcopy(question)
        payload["question_number"] = question_number
        return payload


class DbBackedQuestionPersonalizer:
    def __init__(self, db_service: Any, personalizer: QuestionPersonalizer) -> None:
        self.db_service = db_service
        self.personalizer = personalizer

    def load_player_history_embeddings(self, game_id: int) -> dict[int, list[list[float]]]:
        player_ids = self.db_service.get_current_game_user_ids(game_id)
        history: dict[int, list[list[float]]] = {}
        for player_id in player_ids:
            history[player_id] = [
                embedding
                for _, embedding in self.db_service.get_last_answers(player_id)
                if embedding
            ]
        return history

    def personalize_game_candidates(
        self,
        game_id: int,
        candidates: Sequence[QuestionPayload | str],
        *,
        target_count: int | None = None,
        existing_quiz_questions: Sequence[QuestionPayload | str] | None = None,
        include_existing_game_questions: bool = False,
    ) -> PersonalizationResult:
        player_history = self.load_player_history_embeddings(game_id)
        resolved_existing_questions: list[QuestionPayload | str] = []
        if include_existing_game_questions:
            resolved_existing_questions.extend(self.db_service.get_game_questions(game_id))
        if existing_quiz_questions:
            resolved_existing_questions.extend(existing_quiz_questions)
        return self.personalizer.select_questions(
            candidates,
            player_history,
            target_count=target_count,
            existing_quiz_questions=resolved_existing_questions,
        )


def cosine_similarity(first: Embedding, second: Embedding) -> float:
    if not first or not second:
        return 0.0

    dot_product = sum(float(a) * float(b) for a, b in zip(first, second))
    first_norm = math.sqrt(sum(float(value) * float(value) for value in first))
    second_norm = math.sqrt(sum(float(value) * float(value) for value in second))
    if first_norm == 0.0 or second_norm == 0.0:
        return 0.0
    return dot_product / (first_norm * second_norm)


def _load_candidates(path: Path) -> list[QuestionPayload]:
    with path.open("r", encoding="utf-8") as candidate_file:
        data = json.load(candidate_file)

    if not isinstance(data, list):
        raise ValueError("Candidates file must contain a JSON array")
    return [item for item in data if isinstance(item, dict)]


def _load_optional_questions(path: Path | None) -> list[QuestionPayload]:
    if path is None:
        return []
    return _load_candidates(path)


def _parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(
        description="Filter semantic duplicates in a quiz and personalize it for current players.",
    )
    parser.add_argument("--game-id", type=int, required=True, help="Current QuizAI game id")
    parser.add_argument(
        "--candidates-file",
        type=Path,
        required=True,
        help="Path to a JSON array of question candidates",
    )
    parser.add_argument(
        "--existing-questions-file",
        type=Path,
        default=None,
        help="Optional JSON array of questions already selected for this quiz",
    )
    parser.add_argument(
        "--target-count",
        type=int,
        default=None,
        help="Desired number of questions after filtering",
    )
    parser.add_argument(
        "--history-threshold",
        type=float,
        default=0.72,
        help="Reject candidate if it is too close to any player's history",
    )
    parser.add_argument(
        "--quiz-threshold",
        type=float,
        default=0.80,
        help="Reject candidate if it is too close to another question in the same quiz",
    )
    parser.add_argument(
        "--include-existing-game-questions",
        action="store_true",
        help="Also compare with questions already stored for this game",
    )
    parser.add_argument(
        "--model-name",
        default=DEFAULT_MODEL_NAME,
        help="sentence-transformers model to use",
    )
    parser.add_argument(
        "--local-files-only",
        action="store_true",
        help="Do not try to download the model from the network",
    )
    return parser.parse_args()


def main() -> int:
    args = _parse_args()
    try:
        candidates = _load_candidates(args.candidates_file)
        existing_questions = _load_optional_questions(args.existing_questions_file)

        try:
            from service.db_service import DbService
        except ModuleNotFoundError:
            from db_service import DbService

        embedder = SentenceTransformerEmbedder(
            args.model_name,
            local_files_only=args.local_files_only,
        )

        personalizer = QuestionPersonalizer(
            embedder,
            history_similarity_threshold=args.history_threshold,
            quiz_similarity_threshold=args.quiz_threshold,
        )

        with DbService() as db_service:
            db_personalizer = DbBackedQuestionPersonalizer(db_service, personalizer)
            result = db_personalizer.personalize_game_candidates(
                args.game_id,
                candidates,
                target_count=args.target_count,
                existing_quiz_questions=existing_questions,
                include_existing_game_questions=args.include_existing_game_questions,
            )

        print(json.dumps(result.to_dict(), ensure_ascii=False, indent=2))
        return 0
    except Exception as exc:
        print(str(exc), file=sys.stderr)
        return 1


if __name__ == "__main__":
    raise SystemExit(main())
