from django.urls import path

from reportes import views

urlpatterns = [
    path("reportes/cliente/", views.reporte_cliente),
]
