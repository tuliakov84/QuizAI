#!/usr/bin/env bash
set -euo pipefail

# Создаёт топики QuizAI в Kafka (docker: kafka:29092, хост: localhost:9092).
BOOTSTRAP="${KAFKA_BOOTSTRAP_SERVERS:-kafka:29092}"

echo "Waiting for Kafka at ${BOOTSTRAP}..."
for i in $(seq 1 60); do
  if kafka-topics --bootstrap-server "${BOOTSTRAP}" --list >/dev/null 2>&1; then
    echo "Kafka is reachable."
    break
  fi
  if [ "${i}" -eq 60 ]; then
    echo "Timeout waiting for Kafka."
    exit 1
  fi
  sleep 2
done

create_topic() {
  local name="$1"
  local partitions="${2:-3}"
  local replication="${3:-1}"
  kafka-topics --create --if-not-exists --bootstrap-server "${BOOTSTRAP}" \
    --replication-factor "${replication}" --partitions "${partitions}" --topic "${name}"
}

# Java: запросы генерации и результаты (валидация)
create_topic "question-generation-requests"
create_topic "ml-question-results"
# Python embedding_service: массив id вопросов
create_topic "ml-question-requests"
# Python validation worker: запрос/результат валидации и регенерации
create_topic "python-validation-requests"
create_topic "python-validation-results"

echo "QuizAI Kafka topics are ready."
