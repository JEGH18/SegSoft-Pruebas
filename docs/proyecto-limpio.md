# Proyecto limpio: Tienda v3

`proyecto-limpio/` es la **misma tienda con la remediación completa**. Parte del proyecto intermedio y corrige:

- los 5 hallazgos MEDIUM que quedaban;
- las debilidades residuales que el motor no reportaba.

El objetivo es probar el estado **COMPLIANT**: el sistema no debe reportar nada.

## Resultado esperado

Resultado obtenido al ejecutar el motor real (`/internal/execute-rule` con `fixtures/rules_snapshot.json`) sobre todos los archivos:

- **0 hallazgos** en 31 archivos, **0 errores de regla**.
- Ninguna de las 23 reglas activas encuentra nada.
- **Todas las políticas seleccionadas deben quedar COMPLIANT.**

Si el sistema reporta cualquier hallazgo en este proyecto, es un **falso positivo**, porque el código es correcto. Esto sirve para medir la **precisión** del motor.

---

## Qué se corrigió respecto al proyecto intermedio

### Los 5 hallazgos MEDIUM

| # | Hallazgo en el intermedio | Corrección | Archivo |
|---|---|---|---|
| M1 | `SELECT * FROM users` | Solo las columnas necesarias: `SELECT username, password_hash, role, totp_secret, failed_attempts, locked_until` | `user/UserRepository.java` |
| M2 | `SELECT * FROM orders` | `SELECT id, status, total, created_at`; ya no se exponen otras columnas del pedido | `order/OrderController.java` |
| M3 | `SELECT * FROM orders` (Django) | `SELECT id, total` | `reportes/reportes/db.py` |
| M4 | Sin Content-Security-Policy | Se declara `security.headers.content-security-policy` **y se aplica** en tres lugares (ver la lista siguiente) | `application.yml`, `security/SecurityHeadersFilter.java`, `reportes/middleware.py`, `frontend/index.html` |
| M5 | Sin política de actualización de dependencias | Se declara `dependency.update.maxAgeDays: 30` y se automatiza con Renovate (actualizaciones semanales, alertas de vulnerabilidades y automerge de parches) | `application.yml`, `renovate.json` |

En M4, la CSP no queda solo declarada; se aplica en:

- **Backend:** un filtro que envía la cabecera en cada respuesta, junto con HSTS, `X-Content-Type-Options`, `X-Frame-Options` y `Referrer-Policy`.
- **Reportes:** un middleware de Django.
- **Frontend:** una etiqueta `<meta>`.

### Las debilidades residuales que el motor no detectaba

| Debilidad en el intermedio | Corrección |
|---|---|
| JWT de 8 horas sin revocación | Vida de **15 minutos** y verificación del emisor (`withIssuer`). El endpoint `/api/auth/logout` borra la cookie |
| JWT en `sessionStorage`, legible por JavaScript | El JWT viaja en una **cookie `HttpOnly`, `Secure` y `SameSite=Strict`**. El frontend no toca el token (`withCredentials: true`), así que un XSS ya no puede robarlo. `SameSite=Strict` evita el CSRF que introduce el uso de cookies |
| El backend no validaba el JWT en cada petición | `JwtCookieAuthFilter` exige un JWT válido en todo `/api/**` excepto `/api/auth/**`, y entrega el usuario autenticado como `Principal` |
| El rate limiter era global: un atacante podía agotar el cupo y bloquear a todos | Además del rate limiter, ahora hay un **bloqueo por cuenta**: 5 fallos seguidos bloquean la cuenta 15 minutos (`LoginAttemptService`, columnas `failed_attempts` y `locked_until`). El error siempre es el mismo, "Credenciales invalidas", para no revelar si el usuario existe |
| Un código MFA incorrecto no contaba como intento fallido | Un fallo de contraseña o de TOTP suma al contador de bloqueo |
| El login de reportes no limitaba los intentos | Contador en caché por usuario: 5 intentos, luego un bloqueo de 15 minutos con respuesta HTTP 429 |
| Django no enviaba CSP | `ContentSecurityPolicyMiddleware` más HSTS, `nosniff`, `X-Frame-Options: DENY`, cookie de sesión `SameSite=Strict` y 15 minutos de sesión |

## Controles de seguridad del proyecto limpio, por categoría

| Categoría | Controles |
|---|---|
| SQL Injection | Consultas 100 % parametrizadas (`?` y `%s`), columnas explícitas, lista blanca con enum (`OrderStatus`) y consultas limitadas al usuario autenticado |
| XSS | React escapa `{...}`, se usa `textContent` en el DOM y `format_html` en Django. CSP en backend, reportes y frontend |
| Autenticación | Argon2, mínimo de 12 caracteres, MFA TOTP para ADMIN, JWT de 15 minutos en cookie HttpOnly, rate limiting más bloqueo de cuenta y mensajes de error que no revelan si el usuario existe |
| Manejo de datos | Secretos en variables de entorno, AES-256-GCM con IV aleatorio para tarjetas, TLS obligatorio, HSTS y logs sin credenciales ni tokens |
| Dependencias | Versiones sin CVE conocidos, verificación de checksums declarada y actualización automática con Renovate |

## Código seguro que se parece a código vulnerable (para medir precisión)

El proyecto tiene a propósito líneas **seguras** que comparten palabras clave con los patrones vulnerables. Si el motor las marca, es un falso positivo:

| Línea segura | Se parece a la regla... |
|---|---|
| `cursor.execute("SELECT password_hash FROM report_users WHERE username = %s", [username])` | SQL con f-string (no es un f-string) y "password" en la consulta |
| `rawPassword.length() < MIN_PASSWORD_LENGTH` | Contraseña de longitud menor a 12 (la constante vale 12) |
| `@Column(name = "password_hash", nullable = false)` | Contraseña en texto plano (es un hash Argon2) |
| `Cipher.getInstance("AES/GCM/NoPadding")` | DES/ECB |
| `JWT.create().withIssuedAt(now).withExpiresAt(...)` | JWT sin expiración |
| `password: ${DB_PASSWORD}` (application.yml) | Secreto en el código (viene del entorno) |
| `"PASSWORD": os.environ["DB_PASSWORD"]` (settings.py) | Secreto en el código |
| `format_html("<h1>Reporte de ventas de {}</h1>", cliente)` | HTML generado con datos del usuario (se escapa) |
| `log.warn("Inicio de sesion fallido usuario={}", username)` | Datos sensibles en logs (solo registra el usuario) |

## Decisiones de diseño condicionadas por las reglas

Son las mismas que se explican en `proyecto-intermedio.md`, más una nueva:

1. **`renovate.json` en lugar de `.github/dependabot.yml`.** Dependabot habría sido igual de válido, pero su archivo es `.yml`. Las reglas CONFIG_CHECK con `must_exist` le exigirían `mfa.required.roles`, `artifact.checksum.verify`, la CSP y `dependency.update.maxAgeDays`, lo que produciría **4 falsos positivos (1 CRITICAL, 1 HIGH, 2 MEDIUM)** y dejaría el proyecto NON_COMPLIANT. Lo mismo pasaría con un `docker-compose.yml` o con un workflow de GitHub Actions.
2. **`@RateLimiter` va en la misma línea que `@PostMapping("/login")`**, porque la regla CWE-307 solo mira esa línea.
3. **Nombres `rawPassword` y `password_hash`**, para no activar las reglas CWE-312, que marcan cualquier `String password =` o columna `password`.

## Limitación importante: 0 hallazgos no significa 0 vulnerabilidades

El motor solo sabe buscar los patrones de sus 23 reglas. El proyecto limpio es seguro **porque se diseñó así**, no porque el sistema lo haya demostrado. En particular, SegSoft no verifica:

- la lógica de autorización, como los IDOR;
- que las claves de configuración declaradas (MFA, checksum) se usen de verdad en el código;
- la calidad de los valores declarados: cualquier valor de CSP cumple, incluso `default-src *`.
