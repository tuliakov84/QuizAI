# Repo split plan (backend + question-generator + python-validation-worker)

## Что уже сделано в этом репозитории

1. **Отвязал Java generator от БД backend в Kafka-контракте**
   - Файл: `src/main/java/com/mipt/service/QuestionGenerationRequestConsumerService.java`
   - Теперь generator читает `topicName` и `existingQuestions` прямо из Kafka payload.
   - Удалена прямая зависимость `QuestionGenerationRequestConsumerService -> DbService`.

2. **Расширил producer payload на backend стороне**
   - Файл: `src/main/java/com/mipt/service/MlQuestionRequestProducerService.java`
   - Добавлены поля `topicName` и `existingQuestions` в сообщения для `question-generation-requests`.
   - Обновлен вызов `sendRegenerationRequest(...)`.

3. **Обновил orchestration регенерации на backend стороне**
   - Файл: `src/main/java/com/mipt/service/QuestionGenerationConsumerService.java`
   - Backend теперь сам готовит контекст для регенерации (topicName + existingQuestions) и отправляет его генератору.

4. **Обновил загрузку вопросов (initial flow)**
   - Файл: `src/main/java/com/mipt/service/QuestionLoadingService.java`
   - При первичной генерации backend отправляет в Kafka `topicName`.

5. **Сделал Python worker автономным от backend-репо**
   - Файл: `src/main/python/service/config.py`
   - Конфиг теперь: env first, optional `application.properties` second.
   - Добавлен `QUIZAI_PROPERTIES_PATH` как опциональный fallback.

6. **Подготовил артефакты для multi-repo запуска**
   - Файл: `docker-compose.multi-repo.yml`
   - Файл: `python-worker.env.example`
   - Можно подставлять готовые images каждого сервиса из отдельных репозиториев.

## Целевая структура репозиториев

### 1) `quizai-backend`
- REST API (`ApiController`, auth/otp, game flow, leaderboard, achievements)
- DB migrations + JPA + `DbService`
- Kafka producers/consumers, кроме кода LLM генерации
- Статические ресурсы и текущий фронт (Thymeleaf + JS)

### 2) `quizai-question-generator`
- `QuestionGenerator`
- `QuestionGenerationRequestConsumerService`
- Kafka producer в `question-generation-results`
- Никаких подключений к PostgreSQL

### 3) `quizai-python-validation-worker`
- `src/main/python/service/*`
- Kafka consumer `python-validation-requests`
- Kafka producer `python-validation-results`
- Доступ к PostgreSQL только для embedding/personalization логики

## Kafka-контракты после правок

### Topic `question-generation-requests` (backend -> generator)
Минимальный payload:

```json
{
  "requestId": "uuid-or-gameId",
  "status": "REQUESTED",
  "gameId": 123,
  "topicId": 7,
  "topicName": "История Древнего Рима",
  "numberOfQuestions": 10,
  "levelDifficulty": 2,
  "attempt": 0,
  "isRegeneration": false
}
```

Для регенерации дополнительно:
- `questionNumbersToRegenerate: number[]`
- `questionIdsToReplace: number[]`
- `existingQuestions: string[]`

### Topic `question-generation-results` (generator -> backend)
- `status = GENERATED`
- `questions = [...]`
- остальные correlation-поля пробрасываются обратно (`gameId`, `requestId`, `attempt`, ...)

### Topic `python-validation-requests` (backend -> python)
- `status = VALIDATION_REQUESTED`
- `gameId`, `requestId`, `attempt`, `questionIds[]`

### Topic `python-validation-results` (python -> backend)
- `status = VALIDATED` или `REGENERATE`
- `gameId`, `requestId`, `attempt`
- при `REGENERATE`: `questionIdsToRegenerate[]`

## Что тебе останется сделать (пошагово)

1. **Создать 3 репозитория**
   - `quizai-backend`
   - `quizai-question-generator`
   - `quizai-python-validation-worker`

2. **Перенести код по границам сервисов**
   - Backend: весь Java код, кроме чисто генераторных entrypoints (можно оставить общие DTO/утилиты).
   - Generator: минимальный Spring Boot app + Kafka + `QuestionGenerator` + consumer.
   - Python worker: весь `src/main/python/service` + `requirements-python-worker.txt`.

3. **Собрать независимые Dockerfile в каждом репо**
   - Backend image
   - Generator image
   - Python worker image

4. **Настроить CI/CD отдельно для каждого репо**
   - Build + unit tests
   - Publish image (например, GHCR)
   - Теги: `main`, semver, commit-sha

5. **Вынести общий контракт в отдельный пакет/репо (рекомендуется)**
   - Вариант A: `quizai-contracts` (JSON Schema / AsyncAPI)
   - Вариант B: держать контракт в backend и подключать git-submodule в generator/python

6. **Синхронизировать конфигурации через env**
   - Нормализовать имена env-переменных для всех сервисов.
   - Убрать зависимость от shared `application.properties` между репо.

7. **Проверить end-to-end в docker compose**
   - Использовать `docker-compose.multi-repo.yml`.
   - Подставить реальные image names через:
	 - `QUIZAI_BACKEND_IMAGE`
	 - `QUIZAI_GENERATOR_IMAGE`
	 - `QUIZAI_PYTHON_WORKER_IMAGE`

8. **Стабилизировать совместимость версий Spring**
   - В backend/generator не использовать milestone `spring-kafka` при `spring-boot 3.2.x`.
   - Зафиксировать совместимые версии через BOM Spring Boot.

9. **Сделать rollout-порядок**
   - Сначала деплой generator + python-worker (backward-compatible)
   - Потом backend с новым payload (`topicName`, `existingQuestions`)
   - Потом отключить legacy fallback окончательно

10. **Добавить contract tests между сервисами**
	- Проверка сериализации/десериализации Kafka payload
	- Проверка mandatory полей (`topicName`, `requestId`, `status`)

## Быстрый smoke-check после split

1. Создать игру через backend API.
2. Убедиться, что backend отправил `question-generation-requests`.
3. Убедиться, что generator отправил `GENERATED` в `question-generation-results`.
4. Проверить отправку `python-validation-requests`.
5. Проверить ответ `VALIDATED`/`REGENERATE` из Python worker.
6. Убедиться, что игра переходит в ready/start без ручных фиксов.

