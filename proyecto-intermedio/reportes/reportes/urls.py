from django.urls import path

from reportes import views

urlpatterns = [
    path("reportes/sesion/", views.login_reportes),
    path("reportes/cliente/", views.reporte_cliente),
]
