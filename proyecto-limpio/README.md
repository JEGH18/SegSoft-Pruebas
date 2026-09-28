# Tienda (v3)

Tercera versión de la tienda en línea, con la remediación de seguridad completa.

- `backend/`: API de la tienda (Spring, Java 17). Maneja usuarios, inicio de sesión, pedidos y pagos.
- `reportes/`: servicio interno de reportes de ventas (Django).
- `frontend/`: interfaz web de la tienda (React + Vite).

Toda la configuración sensible se carga desde variables de entorno. Las dependencias se actualizan de forma automática con Renovate (`renovate.json`).
