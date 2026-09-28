from django.http import HttpResponse
from django.utils.html import format_html, format_html_join
from django.views.decorators.http import require_GET, require_POST

from reportes.auth import check_credentials, cuenta_bloqueada, iniciar_sesion, registrar_fallo, requiere_sesion
from reportes.db import ventas_por_cliente


@require_POST
def login_reportes(request):
    username = request.POST.get("username", "")
    if cuenta_bloqueada(username):
        return HttpResponse(status=429)
    if not check_credentials(username, request.POST.get("password", "")):
        registrar_fallo(username)
        return HttpResponse(status=401)
    iniciar_sesion(request, username)
    return HttpResponse(status=204)


@require_GET
@requiere_sesion
def reporte_cliente(request):
    cliente = request.GET.get("cliente", "")
    filas = ventas_por_cliente(cliente)
    titulo = format_html("<h1>Reporte de ventas de {}</h1>", cliente)
    cuerpo = format_html_join("", "<tr><td>{}</td><td>{}</td></tr>", ((fila[0], fila[1]) for fila in filas))
    return HttpResponse(format_html("{}<table>{}</table>", titulo, cuerpo))
