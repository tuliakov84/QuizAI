"""
QuizAI Embedding Service

Creates embeddings for quizai questions
"""

from __future__ import annotations

import os
import logging

from sentence_transformers import SentenceTransformer, util

from db_service import DbService

class Embedder:
    """
    Lazy-loads the transformer so Kafka consumers can subscribe immediately.
    First inference may take minutes if the model is downloaded from the network.
    """

    def __init__(
        self,
        model_name: str = "sentence-transformers/paraphrase-multilingual-MiniLM-L12-v2",
    ) -> None:
        self._model_name = model_name
        self._cache_folder = os.getenv("HF_HOME") or os.getenv("TRANSFORMERS_CACHE") or os.getenv("HF_HUB_CACHE")
        self._model: SentenceTransformer | None = None

    @property
    def model(self) -> SentenceTransformer:
        if self._model is None:
            logging.info(
                "Loading SentenceTransformer model %r (first use; download/load can take several minutes)...",
                self._model_name,
            )
            if self._cache_folder:
                self._model = SentenceTransformer(self._model_name, cache_folder=self._cache_folder)
            else:
                self._model = SentenceTransformer(self._model_name)
            logging.info("SentenceTransformer model ready.")
        return self._model

    def get_embedding(self, sentence: str):
        return self.model.encode(sentence, convert_to_tensor=True)

    def encode(self, texts: list[str]) -> list[list[float]]:
        if not texts:
            return []
        vectors = self.model.encode(texts, convert_to_numpy=True, normalize_embeddings=False)
        return [[float(value) for value in row] for row in vectors]

    def cosine_similarity(self, sent1: str, sent2: str) -> float:
        emb1 = self.get_embedding(sent1)
        emb2 = self.get_embedding(sent2)
        similarity = util.cos_sim(emb1, emb2)
        return float(similarity.item())


def parse_question_ids(message_value) -> list[int]:
    if not isinstance(message_value, list):
        raise ValueError("Message must be a JSON array of question ids")
    return [int(question_id) for question_id in message_value]


def generate_and_persist_embeddings_for_ids(
    db_service: DbService,
    embedder: Embedder,
    question_ids: list[int],
) -> list[int]:
    logging.info("Step PY2: loading question texts for embeddings. ids=%s", question_ids)
    question_rows = db_service.get_question_texts(question_ids)
    question_texts = [question_text for _, question_text in question_rows if question_text]
    logging.info(
        "Step PY3: encoding embeddings in batch. requested=%s, with_text=%s",
        len(question_ids),
        len(question_texts),
    )
    vectors = embedder.model.encode(question_texts, convert_to_numpy=True) if question_texts else []

    embeddings_dict: dict[int, list[float]] = {}
    vector_idx = 0
    for db_id, question_text in question_rows:
        if not question_text:
            continue
        embeddings_dict[db_id] = [float(value) for value in vectors[vector_idx]]
        vector_idx += 1

    if embeddings_dict:
        db_service.load_embeddings(embeddings_dict)
        logging.info("Step PY4: stored embeddings in DB. stored_count=%s, ids=%s", len(embeddings_dict), list(embeddings_dict.keys()))
    return list(embeddings_dict.keys())


async def handle_message(db_service: DbService, embedder: Embedder, message_value) -> None:
    question_ids = parse_question_ids(message_value)
    generate_and_persist_embeddings_for_ids(db_service, embedder, question_ids)


if __name__ == "__main__":
    logging.basicConfig(level=logging.INFO, format="%(asctime)s %(levelname)s %(message)s")
    logging.info(
        "embedding_service.py no longer consumes Kafka directly. "
        "Use kafka_question_validation_worker.py for Kafka-driven flow."
    )
