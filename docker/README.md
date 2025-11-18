# Docker Setup для QuizAI

Этот каталог содержит конфигурацию Docker для запуска PostgreSQL базы данных.

## Быстрый старт

### 1. Запуск PostgreSQL в Docker

```bash
docker-compose up -d
```

Эта команда:
- Создаст и запустит контейнер PostgreSQL
- Автоматически создаст базу данных `quizai`
- Выполнит SQL скрипт инициализации из `docker/init.sql`
- Откроет порт 5432 для подключения

### 2. Проверка статуса

```bash
docker-compose ps
```

### 3. Просмотр логов

```bash
docker-compose logs -f postgres
```

### 4. Остановка контейнера

```bash
docker-compose down
```

### 5. Остановка с удалением данных

```bash
docker-compose down -v
```

## Подключение к базе данных

После запуска контейнера, приложение Spring Boot автоматически подключится к базе данных используя настройки из `application.properties`:

- **Host**: localhost
- **Port**: 5432
- **Database**: quizai
- **User**: postgres
- **Password**: postgres

## Структура файлов

- `docker-compose.yml` - конфигурация Docker Compose
- `docker/init.sql` - SQL скрипт для инициализации схемы базы данных
- `docker/README.md` - эта инструкция

## Примечания

- Данные PostgreSQL сохраняются в Docker volume `postgres_data`
- При первом запуске скрипт `init.sql` выполнится автоматически
- Для изменения пароля или других настроек отредактируйте `docker-compose.yml`

