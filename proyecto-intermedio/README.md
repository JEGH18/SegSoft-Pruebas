# Tienda (v2)

Segunda versión de la tienda en línea, después de una primera ronda de remediación de seguridad.

- `backend/`: API de la tienda (Spring, Java 17). Maneja usuarios, inicio de sesión, pedidos y pagos.
- `reportes/`: servicio interno de reportes de ventas (Django).
- `frontend/`: interfaz web de la tienda (React + Vite).

Toda la configuración sensible se carga desde variables de entorno. No hay credenciales en el repositorio.
