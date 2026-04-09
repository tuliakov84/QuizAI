import os
from jproperties import Properties
from pathlib import Path
from urllib.parse import urlparse

configs = Properties()

CONFIG_PATHS = [
    Path(__file__).resolve().parents[2] / "resources" / "application.properties",
    Path(__file__).resolve().parents[4] / "target" / "classes" / "application.properties",
]

config_path = next((path for path in CONFIG_PATHS if path.exists()), None)
if config_path is None:
    searched = ", ".join(str(path) for path in CONFIG_PATHS)
    raise FileNotFoundError(f"application.properties not found. Looked in: {searched}")

with config_path.open("rb") as config_file:
    configs.load(config_file)

DB_URL = configs.get("app.database.url").data.replace("jdbc:", "", 1)
DEFAULT_DB_USER = configs.get("app.database.user").data
DEFAULT_DB_PASSWORD = configs.get("app.database.password").data

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
    configs.get("spring.kafka.bootstrap-servers").data,
)
PYTHON_VALIDATION_REQUESTS_TOPIC = configs.get("app.kafka.topic.python-validation-requests").data
PYTHON_VALIDATION_RESULTS_TOPIC = configs.get("app.kafka.topic.python-validation-results").data
# earliest: do not skip messages if the worker starts after the backend sent the request (new consumer group).
KAFKA_AUTO_OFFSET_RESET = os.getenv("QUIZAI_KAFKA_AUTO_OFFSET_RESET", "earliest")
