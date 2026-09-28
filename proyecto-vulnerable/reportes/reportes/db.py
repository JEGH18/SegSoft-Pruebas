from django.db import connection


def ventas_por_cliente(cliente: str) -> list:
    with connection.cursor() as cursor:
        cursor.execute(f"SELECT * FROM orders WHERE customer_name = '{cliente}'")
        return cursor.fetchall()


def ventas_por_rango(desde: str, hasta: str) -> list:
    with connection.cursor() as cursor:
        cursor.execute(f"SELECT id, total FROM orders WHERE created_at BETWEEN '{desde}' AND '{hasta}'")
        return cursor.fetchall()
