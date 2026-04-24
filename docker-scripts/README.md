<h1>Docker scripts</h1>

## Monorepo mode

- Запуск: `docker-scripts/docker-start.sh`
- Остановка: `docker-scripts/docker-stop.sh`
- Этот режим использует `docker-compose.yml`

## Split-repo mode

- Для разнесённой архитектуры используйте `docker-compose.multi-repo.yml`
- Пример запуска:

```bash
docker compose -f docker-compose.multi-repo.yml up -d
```

- В этом режиме backend, question-generator и python-validation-worker запускаются как отдельные image
