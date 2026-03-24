"""
Generates and persists embeddings for already saved QuizAI questions.
"""

from __future__ import annotations

import argparse
import json
import sys
from pathlib import Path

from question_personalization import DEFAULT_MODEL_NAME, SentenceTransformerEmbedder

try:
    from service.db_service import DbService
except ModuleNotFoundError:
    from db_service import DbService


def _parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(
        description="Persist embeddings for question ids stored in QuizAI database.",
    )
    parser.add_argument(
        "--question-ids-file",
        type=Path,
        required=True,
        help="Path to a JSON array of question ids",
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


def _load_question_ids(path: Path) -> list[int]:
    with path.open("r", encoding="utf-8") as ids_file:
        data = json.load(ids_file)
    if not isinstance(data, list):
        raise ValueError("Question ids file must contain a JSON array")
    return [int(question_id) for question_id in data]


def main() -> int:
    args = _parse_args()
    try:
        question_ids = _load_question_ids(args.question_ids_file)
        embedder = SentenceTransformerEmbedder(
            args.model_name,
            local_files_only=args.local_files_only,
        )

        with DbService() as db_service:
            question_rows = db_service.get_question_texts(question_ids)
            question_texts = [question_text for _, question_text in question_rows if question_text]
            embeddings = embedder.encode(question_texts)

            embeddings_dict: dict[int, list[float]] = {}
            embedding_index = 0
            for question_id, question_text in question_rows:
                if not question_text:
                    continue
                embeddings_dict[question_id] = embeddings[embedding_index]
                embedding_index += 1

            if embeddings_dict:
                db_service.load_embeddings(embeddings_dict)

        print(
            json.dumps(
                {
                    "embedded_question_ids": list(embeddings_dict.keys()),
                    "embedded_count": len(embeddings_dict),
                },
                ensure_ascii=False,
            )
        )
        return 0
    except Exception as exc:
        print(str(exc), file=sys.stderr)
        return 1


if __name__ == "__main__":
    raise SystemExit(main())
