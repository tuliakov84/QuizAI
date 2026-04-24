import os
from jproperties import Properties
from pathlib import Path
from urllib.parse import urlparse

def _load_optional_properties() -> Properties | None:
    custom_path = os.getenv("QUIZAI_PROPERTIES_PATH")
    config_paths = [
        Path(custom_path) if custom_path else None,
        Path(__file__).resolve().parents[2] / "resources" / "application.properties",
        Path(__file__).resolve().parents[4] / "target" / "classes" / "application.properties",
    ]
    for path in config_paths:
        if path is None:
            continue
        if path.exists() and path.is_file():
            props = Properties()
            with path.open("rb") as config_file:
                props.load(config_file)
            return props
    return None


def _prop_or_default(configs: Properties | None, key: str, default: str) -> str:
    if configs is None:
        return default
    value = configs.get(key)
    if value is None or value.data is None:
        return default
    return value.data


configs = _load_optional_properties()

DB_URL = os.getenv(
    "QUIZAI_DB_URL",
    _prop_or_default(configs, "app.database.url", "jdbc:postgresql://localhost:5432/quizai"),
).replace("jdbc:", "", 1)
DEFAULT_DB_USER = _prop_or_default(configs, "app.database.user", "postgres")
DEFAULT_DB_PASSWORD = _prop_or_default(configs, "app.database.password", "postgres")

parsed = urlparse(DB_URL)
DEFAULT_DB_HOST = parsed.hostname
DEFAULT_DB_PORT = parsed.port
DEFAULT_DB_NAME = parsed.path[1:]

DB_HOST = os.getenv("QUIZAI_DB_HOST", DEFAULT_DB_HOST)
DB_PORT = int(os.getenv("QUIZAI_DB_PORT", str(DEFAULT_DB_PORT)))
DB_NAME = os.getenv("QUIZAI_DB_NAME", DEFAULT_DB_NAME)
DB_USER = os.getenv("QUIZAI_DB_USER", DEFAULT_DB_USER)
DB_PASSWORD = os.getenv("QUIZAI_DB_PASSWORD", DEFAULT_DB_PASSWORD)

KAFKA_BOOTSTRAP_SERVERS = os.getenv(
    "QUIZAI_KAFKA_BOOTSTRAP_SERVERS",
    _prop_or_default(configs, "spring.kafka.bootstrap-servers", "localhost:9092"),
)
PYTHON_VALIDATION_REQUESTS_TOPIC = os.getenv(
    "QUIZAI_PYTHON_VALIDATION_REQUESTS_TOPIC",
    _prop_or_default(configs, "app.kafka.topic.python-validation-requests", "python-validation-requests"),
)
PYTHON_VALIDATION_RESULTS_TOPIC = os.getenv(
    "QUIZAI_PYTHON_VALIDATION_RESULTS_TOPIC",
    _prop_or_default(configs, "app.kafka.topic.python-validation-results", "python-validation-results"),
)
# earliest: do not skip messages if the worker starts after the backend sent the request (new consumer group).
KAFKA_AUTO_OFFSET_RESET = os.getenv("QUIZAI_KAFKA_AUTO_OFFSET_RESET", "earliest")
