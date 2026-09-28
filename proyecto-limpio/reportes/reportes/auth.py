import logging
from functools import wraps

from django.conf import settings
from django.contrib.auth.hashers import check_password, make_password
from django.core.cache import cache
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


def _attempts_key(username: str) -> str:
    return f"reportes-login-intentos:{username}"


def cuenta_bloqueada(username: str) -> bool:
    return cache.get(_attempts_key(username), 0) >= settings.LOGIN_MAX_ATTEMPTS


def registrar_fallo(username: str) -> None:
    key = _attempts_key(username)
    cache.add(key, 0, timeout=settings.LOGIN_LOCKOUT_SECONDS)
    cache.incr(key)
    logger.warning("Inicio de sesion de reportes fallido usuario=%s", username)


def iniciar_sesion(request, username: str) -> None:
    cache.delete(_attempts_key(username))
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
