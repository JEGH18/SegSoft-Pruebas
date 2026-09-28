from django.http import HttpResponse
from django.utils.safestring import mark_safe

from reportes.auth import validate_session
from reportes.db import ventas_por_cliente


def reporte_cliente(request):
    if not validate_session(request.GET.get("token", "")):
        return HttpResponse(status=401)

    cliente = request.GET.get("cliente", "")
    filas = ventas_por_cliente(cliente)
    titulo = mark_safe(f"<h1>Reporte de ventas de {cliente}</h1>")
    cuerpo = "".join(f"<tr><td>{fila[0]}</td><td>{fila[1]}</td></tr>" for fila in filas)
    return HttpResponse(f"{titulo}<table>{cuerpo}</table>")
