"""
Kafka worker for validation loop:
1) receives game id from Java backend;
2) generates and stores embeddings for current game questions;
3) runs semantic personalization checks;
4) sends back either VALIDATED or REGENERATE(questionIdsToRegenerate).
"""

from __future__ import annotations

import asyncio
import json
import logging

from aiokafka import AIOKafkaConsumer, AIOKafkaProducer

import config
from db_service import DbService
from embedding_service import Embedder, generate_and_persist_embeddings_for_ids
from question_personalization import QuestionPersonalizer


def process_validation_request(db_service: DbService, embedder: Embedder, payload: dict) -> dict:
    game_id = int(payload["gameId"])
    request_id = str(payload.get("requestId", game_id))
    attempt = int(payload.get("attempt", 0))
    requested_question_ids = [int(question_id) for question_id in payload.get("questionIds", [])]
    logging.info(
        "Step PY1: processing validation request. gameId=%s, requestId=%s, attempt=%s, requestedQuestionIds=%s",
        game_id,
        request_id,
        attempt,
        requested_question_ids,
    )

    question_ids = requested_question_ids or db_service.get_game_question_ids(game_id)
    logging.info("Step PY1.1: resolved validation scope. gameId=%s, questionIds=%s", game_id, question_ids)
    embedded_question_ids = generate_and_persist_embeddings_for_ids(db_service, embedder, question_ids)

    game_questions = db_service.get_questions_by_ids(question_ids)
    logging.info("Step PY5: loaded questions for semantic validation. gameId=%s, count=%s", game_id, len(game_questions))
    personalizer = QuestionPersonalizer(embedder)
    player_history = {
        user_id: [embedding for _, embedding in db_service.get_last_answers(user_id) if embedding]
        for user_id in db_service.get_current_game_user_ids(game_id)
    }

    result = personalizer.select_questions(
        game_questions,
        player_history,
        target_count=len(game_questions),
    )
    logging.info(
        "Step PY6: personalization done. gameId=%s, selected=%s, rejected=%s, backup=%s",
        game_id,
        len(result.selected_questions),
        len(result.rejected_candidates),
        len(result.backup_candidates),
    )
    question_ids_to_regenerate: list[int] = []
    for decision in result.rejected_candidates:
        if decision.source_question_id is not None:
            question_ids_to_regenerate.append(int(decision.source_question_id))
    question_ids_to_regenerate = list(dict.fromkeys(question_ids_to_regenerate))

    response = {
        "gameId": game_id,
        "requestId": request_id,
        "attempt": attempt,
        "embeddedQuestionIds": embedded_question_ids,
    }
    if question_ids_to_regenerate:
        response["status"] = "REGENERATE"
        response["questionIdsToRegenerate"] = question_ids_to_regenerate
        logging.info(
            "Step PY7: regeneration required. gameId=%s, requestId=%s, ids=%s",
            game_id,
            request_id,
            question_ids_to_regenerate,
        )
    else:
        response["status"] = "VALIDATED"
        logging.info("Step PY7: validation passed. gameId=%s, requestId=%s", game_id, request_id)
    return response


async def consume_and_validate() -> None:
    logging.basicConfig(level=logging.INFO, format="%(asctime)s %(levelname)s %(message)s")
    consumer = AIOKafkaConsumer(
        config.PYTHON_VALIDATION_REQUESTS_TOPIC,
        bootstrap_servers=config.KAFKA_BOOTSTRAP_SERVERS,
        group_id="quizai-question-validation-worker",
        value_deserializer=lambda payload: json.loads(payload.decode("utf-8")),
        auto_offset_reset=config.KAFKA_AUTO_OFFSET_RESET,
        enable_auto_commit=True,
    )
    producer = AIOKafkaProducer(
        bootstrap_servers=config.KAFKA_BOOTSTRAP_SERVERS,
        value_serializer=lambda value: json.dumps(value, ensure_ascii=False).encode("utf-8"),
    )

    await consumer.start()
    await producer.start()
    logging.info(
        "Kafka connected (consumer subscribed). consumeTopic=%s, produceTopic=%s, bootstrap=%s, auto_offset_reset=%s",
        config.PYTHON_VALIDATION_REQUESTS_TOPIC,
        config.PYTHON_VALIDATION_RESULTS_TOPIC,
        config.KAFKA_BOOTSTRAP_SERVERS,
        config.KAFKA_AUTO_OFFSET_RESET,
    )
    db_service = DbService()
    embedder = Embedder()
    logging.info(
        "DB + embedder handles ready (SentenceTransformer loads on first message, not at startup)."
    )
    try:
        async for message in consumer:
            payload = message.value
            logging.info("Kafka message received from validation requests topic: %s", payload)
            if not isinstance(payload, dict) or "gameId" not in payload:
                logging.warning("Skip malformed payload: %s", payload)
                continue

            try:
                response = process_validation_request(db_service, embedder, payload)
                await producer.send_and_wait(
                    config.PYTHON_VALIDATION_RESULTS_TOPIC,
                    response,
                    key=str(response["gameId"]).encode("utf-8"),
                )
                logging.info("Kafka message sent to validation results topic: %s", response)
            except Exception as message_exc:
                logging.exception("Failed to process message %s: %s", payload, message_exc)
    except Exception as exc:
        logging.exception("Validation worker failed: %s", exc)
        raise
    finally:
        await consumer.stop()
        await producer.stop()
        db_service.close()


if __name__ == "__main__":
    asyncio.run(consume_and_validate())
