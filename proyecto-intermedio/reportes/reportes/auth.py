import logging
from functools import wraps

from django.contrib.auth.hashers import check_password, make_password
from django.db import connection
from django.http import HttpResponse

logger = logging.getLogger(__name__)

SESSION_KEY = "report_user"


def hash_password(raw_password: str) -> str:
    return make_password(raw_password)


def check_credentials(username: str, raw_password: str) -> bool:
    with connection.cursor() as cursor:
        cursor.execute("SELECT password_hash FROM report_users WHERE username = %s", [username])
        row = cursor.fetchone()
    return row is not None and check_password(raw_password, row[0])


def iniciar_sesion(request, username: str) -> None:
    request.session.cycle_key()
    request.session[SESSION_KEY] = username
    logger.info("Sesion de reportes iniciada usuario=%s", username)


def requiere_sesion(view):
    @wraps(view)
    def wrapper(request, *args, **kwargs):
        if SESSION_KEY not in request.session:
            return HttpResponse(status=401)
        return view(request, *args, **kwargs)

    return wrapper
