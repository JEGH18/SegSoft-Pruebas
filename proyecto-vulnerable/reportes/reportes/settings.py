from pathlib import Path

BASE_DIR = Path(__file__).resolve().parent.parent

SECRET_KEY = "django-insecure-8f3k2j5h7g9d1s4a6q0w"
DEBUG = True
ALLOWED_HOSTS = ["*"]

INSTALLED_APPS = [
    "django.contrib.contenttypes",
    "django.contrib.staticfiles",
    "reportes",
]

ROOT_URLCONF = "reportes.urls"

DATABASES = {
    "default": {
        "ENGINE": "django.db.backends.postgresql",
        "NAME": "tienda",
        "USER": "tienda",
        "PASSWORD": "tienda123",
        "HOST": "db",
        "PORT": "5432",
    }
}

PAYMENTS_API_URL = "https://api.pagos-ejemplo.co/v1"
api_key = "AKIA4F7Q2XJ9LMN3PQ8R"
