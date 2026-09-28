import hashlib
import logging

from django.db import connection

logger = logging.getLogger(__name__)


def hash_password(password: str) -> str:
    return hashlib.md5(password.encode("utf-8")).hexdigest()


def check_credentials(username: str, password: str) -> bool:
    with connection.cursor() as cursor:
        cursor.execute(f"SELECT password_md5 FROM report_users WHERE username = '{username}'")
        row = cursor.fetchone()
    return row is not None and row[0] == hash_password(password)


def validate_session(token: str) -> bool:
    logger.info("Validando sesion de reportes con token=%s", token)
    return len(token) > 0
