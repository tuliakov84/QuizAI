"""
QuizAI Embedding Service

Creates embeddings for quizai questions
"""

from __future__ import annotations

import asyncio
import json
import logging

from aiokafka import AIOKafkaConsumer
from sentence_transformers import SentenceTransformer, util

from db_service import DbService

EMBEDDING_REQUEST_TOPIC = "ml-question-requests"
KAFKA_BOOTSTRAP_SERVERS = "localhost:9092"


class Embedder:
    def __init__(
        self,
        model_name: str = "sentence-transformers/paraphrase-multilingual-MiniLM-L12-v2",
    ) -> None:
        self.model = SentenceTransformer(model_name)

    def get_embedding(self, sentence: str):
        return self.model.encode(sentence, convert_to_tensor=True)

    def cosine_similarity(self, sent1: str, sent2: str) -> float:
        emb1 = self.get_embedding(sent1)
        emb2 = self.get_embedding(sent2)
        similarity = util.cos_sim(emb1, emb2)
        return float(similarity.item())


def parse_question_ids(message_value) -> list[int]:
    if not isinstance(message_value, list):
        raise ValueError("Message must be a JSON array of question ids")
    return [int(question_id) for question_id in message_value]


async def handle_message(db_service: DbService, embedder: Embedder, message_value) -> None:
    question_ids = parse_question_ids(message_value)
    question_rows = db_service.get_question_texts(question_ids)

    embeddings_dict: dict[int, list[float]] = {}
    for db_id, question_text in question_rows:
        if not question_text:
            continue
        embeddings_dict[db_id] = embedder.get_embedding(question_text).tolist()

    if embeddings_dict:
        db_service.load_embeddings(embeddings_dict)
        logging.info("Stored embeddings for %s questions", len(embeddings_dict))


async def consume() -> None:
    logging.basicConfig(level=logging.INFO, format="%(asctime)s %(levelname)s %(message)s")
    consumer = AIOKafkaConsumer(
        EMBEDDING_REQUEST_TOPIC,
        bootstrap_servers=KAFKA_BOOTSTRAP_SERVERS,
        group_id="quizai-embedding-service",
        value_deserializer=lambda payload: json.loads(payload.decode("utf-8")),
        auto_offset_reset="earliest",
        enable_auto_commit=True,
    )
    db_service = DbService()
    embedder = Embedder()

    await consumer.start()
    try:
        async for message in consumer:
            logging.info("Received embedding request: %s", message.value)
            await handle_message(db_service, embedder, message.value)
    except Exception as exc:
        logging.exception("Embedding service failed: %s", exc)
        raise
    finally:
        await consumer.stop()
        db_service.close()


if __name__ == "__main__":
    asyncio.run(consume())
