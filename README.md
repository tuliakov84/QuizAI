# QuizAI

## Быстрый старт

### Требования

- Java 21+
- Maven 3.9+
- Docker
- Python 3.10+ нужен для локального запуска Python-части вне Docker
- Ollama нужен только для реальной генерации вопросов нейросетью

### 1. Поднятие инфраструктуры
В корневом каталоге проекта выполните:
```bash
docker compose up -d
```

Что включается:

- PostgreSQL на `localhost:5432`
- Kafka на `localhost:9092`
- Kafka UI на `http://localhost:8081`
- Python validation worker для семантической проверки вопросов

### 1.1. Локальные секреты и переопределения

Приложение автоматически подключает файл `application-local.properties` из корня проекта:

```properties
spring.config.import=optional:file:./application-local.properties
```

Этот файл не коммитится в Git и нужен для локальных паролей, SMTP/OTP и личных настроек LLM. Быстрый вариант:

```bash
cp application-local.properties.example application-local.properties
```

После этого заполните минимум почтовые настройки для OTP-регистрации:

```properties
OTP_MAIL_USERNAME=your-login@example.com
OTP_MAIL_PASSWORD=your-app-password
OTP_MAIL_FROM=your-login@example.com
```

### 2А. Запуск бэкенда без генерации нейросетью

Для разработки интерфейса и игровой логики можно отключить реальную генерацию вопросов. В этом режиме вопросы берутся из `src/main/resources/ml-answer-example.json`, Ollama не вызывается и компьютер не нагружается генерацией.

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=mock-questions
```

Или настройте в application.properties:

```properties
app.question-generation.mock-enabled=true
```

### 2Б. Запуск без email/OTP регистрации

Если на этапе разработки не нужна отправка писем, включите профиль `otp-off`:

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=otp-off
```

Профили можно комбинировать:

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=mock-questions,otp-off
```

В `otp-off` почта при регистрации игнорируется: аккаунт создается по username/password без отправки OTP, а вход по email отключается.

### 2В. Полноценный режим с генерацией вопросов нейросетью

Если нужен режим с генерацией вопросов:

```bash
ollama serve
ollama pull qwen2.5
mvn spring-boot:run
```  
*Примечание*: `ollama pull qwen2.5` нужно выполнить один раз для загрузки модели, после чего она будет работать офлайн.  
Текущий Java-генератор обращается к Ollama по:

```text
http://localhost:11434/api/chat
```

## Важные настройки

Основные настройки находятся в `src/main/resources/application.properties`.

| Настройка | Значение по умолчанию | Назначение |
| --- | --- | --- |
| `server.port` | `8080` | HTTP-порт Spring Boot приложения |
| `app.database.url` | `jdbc:postgresql://localhost:5432/quizai` | JDBC URL PostgreSQL |
| `app.database.user` | `postgres` | Пользователь PostgreSQL |
| `app.database.password` | `postgres` | Пароль PostgreSQL |
| `spring.flyway.enabled` | `true` | Включает миграции БД при старте |
| `spring.flyway.locations` | `classpath:db/migration` | Путь к SQL-миграциям |
| `spring.kafka.bootstrap-servers` | `localhost:9092` | Адрес Kafka для backend |
| `app.service.role` | `both` | Роль приложения: backend/generator/both |
| `app.kafka.topic.question-generation-requests` | `question-generation-requests` | Топик запросов на генерацию вопросов |
| `app.kafka.topic.question-generation-results` | `ml-question-results` | Топик результатов генерации |
| `app.kafka.topic.python-validation-requests` | `python-validation-requests` | Топик запросов Python-валидации |
| `app.kafka.topic.python-validation-results` | `python-validation-results` | Топик ответов Python-валидации |
| `app.kafka.message.max-regeneration-attempts` | `3` | Максимум попыток перегенерации невалидных вопросов |
| `app.question-generation.mock-enabled` | `false` | Включает липовые вопросы вместо Ollama |
| `app.question-generation.mock-resource` | `ml-answer-example.json` | Файл с mock-вопросами |
| `app.avatar.storage-dir` | `uploads/avatars` | Папка для пользовательских аватарок |
| `spring.servlet.multipart.max-file-size` | `5MB` | Максимальный размер одного загружаемого файла |
| `spring.servlet.multipart.max-request-size` | `5MB` | Максимальный размер multipart-запроса |
| `app.redis.host` | `localhost` | Redis для хранения OTP-кодов |
| `app.redis.port` | `6379` | Порт Redis |
| `app.mail.host` | `smtp.yandex.ru` | SMTP-сервер для OTP-писем |
| `app.mail.username` | пусто | SMTP-логин, обычно задается через `application-local.properties` |
| `app.mail.password` | пусто | SMTP-пароль, задается только локально |
| `app.auth.email-enabled` | `true` | Включает вход по email, email/OTP регистрацию и сброс пароля; профиль `otp-off` ставит `false` |
| `app.otp.ttl-seconds` | `300` | Время жизни OTP-кода |
| `app.otp.resend-cooldown-seconds` | `60` | Пауза перед повторной отправкой OTP |
| `app.llm.url` | `http://localhost:11434/api/chat` | URL Ollama API |
| `app.llm.model` | `qwen2.5` | Модель Ollama для генерации вопросов |
| `app.llm.request-timeout-seconds` | `120` | Таймаут запроса к LLM |
| `app.llm.max-retries-per-question` | `3` | Максимум повторов генерации одного вопроса |

## Приложение

### Проверка сборки

```bash
mvn -q -DskipTests compile
```

### Запуск тестов

```bash
mvn test
```

### Цель проекта

Создать динамичную платформу для проведения
интеллектуальных онлайн-викторин в реальном
времени с вопросами, которые искусственный
интеллект генерирует мгновенно и по запросу.

### Сроки
Проект рассчитан на 2 семестра (2025-2026 учебный год)

### Описание проекта
Это интерактивная игровая платформа, где вместо
заранее заготовленных вопросов используется
мощь генеративных AI-моделей (например, на базе
GPT). Пользователь выбирает тему ("История
Древнего Рима", "Кинематограф 90-х", "Основы
Python"), после чего система в реальном времени
формирует уникальный набор вопросов с
вариантами ответов и одним правильным. Игроки
присоединяются к сессии, где вопросы выводятся
одновременно для всех, а счет ведется в режиме
реального времени (кто быстрее и правильнее
ответит). Ключевые технологии: WebSockets для
мгновенной синхронизации действий между
игроками и сервером, чат для общения во время
игры, система рейтингов и лидербордов, а также
алгоритмы ИИ для валидации сгенерированных
вопросов на точность и сложность. Также введена
система рейтингов и достижений (ачивки),
которые мотивируют пользователей возвращаться.

### Участники:
Софронов Андрей, Либер Анжелика, Фокин Владислав, Туляков Евгений
