from django.db import connection


def ventas_por_cliente(cliente: str) -> list:
    with connection.cursor() as cursor:
        cursor.execute("SELECT * FROM orders WHERE customer_name = %s", [cliente])
        return cursor.fetchall()


def ventas_por_rango(desde: str, hasta: str) -> list:
    with connection.cursor() as cursor:
        cursor.execute("SELECT id, total FROM orders WHERE created_at BETWEEN %s AND %s", [desde, hasta])
        return cursor.fetchall()
