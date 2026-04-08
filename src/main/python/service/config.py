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
DB_USER = configs.get("app.database.user").data
DB_PASSWORD = configs.get("app.database.password").data

parsed = urlparse(DB_URL)
DB_HOST = parsed.hostname
DB_PORT = parsed.port
DB_NAME = parsed.path[1:]
