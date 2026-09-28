# Proyecto intermedio: Tienda v2

`proyecto-intermedio/` es la **misma tienda del proyecto vulnerable después de una primera ronda de remediación**. Mantiene los mismos tres módulos (backend Java/Spring, reportes Django y frontend React) y las mismas funcionalidades.

- **Corregido:** todas las vulnerabilidades graves (CRITICAL y HIGH).
- **Pendiente:** fallas de **defensa en profundidad** de severidad media. No son explotables por sí solas, pero agravan otro fallo si llega a ocurrir.

El objetivo es probar el estado **REQUIRES_REVIEW** del sistema: hay hallazgos, pero ninguno HIGH o CRITICAL.

## Resultado esperado

Resultado obtenido al ejecutar el motor real (`/internal/execute-rule` con `fixtures/rules_snapshot.json`) sobre todos los archivos:

- **5 hallazgos, todos MEDIUM**, en 24 archivos, sin errores de regla.
- **0 CRITICAL y 0 HIGH.**
- Se activan solo las **3 reglas MEDIUM** que existen en el sistema. Las 20 reglas CRITICAL y HIGH no encuentran nada.

### Estado esperado por política

En los datos iniciales, cada regla pertenece a una política distinta (migraciones V14 y V29):

| Política | Regla | Hallazgos | Estado |
|---|---|---|---|
| Control de acceso a datos por rol | `SELECT *` (CWE-89) | 3 MEDIUM | **REQUIRES_REVIEW** |
| Content Security Policy obligatoria | Falta la cabecera CSP (CWE-693) | 1 MEDIUM | **REQUIRES_REVIEW** |
| Actualización periódica de dependencias | Falta `dependency.update.maxAgeDays` (CWE-1104) | 1 MEDIUM | **REQUIRES_REVIEW** |
| Todas las demás | — | 0 | **COMPLIANT** |

Ninguna política debe quedar **NON_COMPLIANT**. Si alguna aparece así, el sistema está marcando algo que no debería.

---

## Hallazgos residuales (lo que falta corregir)

| # | Ubicación | Regla | Severidad |
|---|---|---|---|
| M1 | `backend/.../user/UserRepository.java:19` | `SELECT * FROM users` | MEDIUM |
| M2 | `backend/.../order/OrderController.java:27` | `SELECT * FROM orders` | MEDIUM |
| M3 | `reportes/reportes/db.py:6` | `SELECT * FROM orders` | MEDIUM |
| M4 | `backend/src/main/resources/application.yml` | Falta `security.headers.content-security-policy` | MEDIUM |
| M5 | `backend/src/main/resources/application.yml` | Falta `dependency.update.maxAgeDays` | MEDIUM |

**Por qué siguen siendo un problema y por qué son de severidad media:**

- **M1, M2 y M3 (`SELECT *`):** las consultas **ya están parametrizadas** (`?` en JDBC y `%s` con lista de parámetros en Django), así que **no hay inyección SQL**. El problema es que traen más columnas de las necesarias:
  - `findByUsername` trae `password_hash`, `totp_secret` y `card_encrypted` cada vez que busca un usuario.
  - Si algún día la respuesta se serializa completa, o aparece una inyección en otro punto, esos datos quedan expuestos.
  - **Corrección:** listar solo las columnas necesarias, por ejemplo `SELECT username, password_hash, role, totp_secret FROM users WHERE username = ?`.
- **M4 (sin CSP):** el XSS ya se corrigió en el código (no hay `innerHTML`, `dangerouslySetInnerHTML` ni `mark_safe`). Sin embargo, sin Content-Security-Policy no hay una segunda barrera si mañana alguien introduce un XSS nuevo.
  - **Corrección:** declarar `security.headers.content-security-policy: "default-src 'self'"` y aplicarla con un filtro o con Spring Security.
- **M5 (sin política de actualización):** las dependencias están al día **hoy**, pero no hay nada que evite que vuelvan a quedar desactualizadas.
  - **Corrección:** declarar `dependency.update.maxAgeDays: 30` y automatizar las actualizaciones con Dependabot o Renovate.

## Qué se corrigió respecto al proyecto vulnerable

| Falla en el proyecto vulnerable | Corrección en el intermedio | Archivo |
|---|---|---|
| SQL con f-strings (CRITICAL) | Parámetros vinculados `%s` | `reportes/reportes/auth.py`, `db.py` |
| Filtro HTTP que se pega en la query (HIGH) | `@RequestParam` tipado con un enum (`OrderStatus`) como lista blanca, y consulta limitada a los pedidos del usuario autenticado (`Principal`) | `order/OrderController.java` |
| Concatenación en `findByUsername` y `save` | `jdbcTemplate` con `?` | `user/UserRepository.java` |
| `dangerouslySetInnerHTML` e `innerHTML` (HIGH) | Renderizado como texto (`{review.comment}` y `textContent`) | `ProductReview.jsx`, `notifications.js` |
| `mark_safe` con datos del usuario (HIGH) | `format_html` y `format_html_join`, que escapan | `reportes/reportes/views.py` |
| JWT sin expiración (CRITICAL) | `withIssuedAt` y `withExpiresAt` (8 h) | `auth/JwtService.java` |
| Login sin rate limiting (HIGH) | `@RateLimiter` de Resilience4j (5 intentos por minuto) | `auth/AuthController.java` y `application.yml` |
| Contraseña mínima de 6 caracteres (HIGH) | Mínimo de 12 caracteres | `auth/AuthController.java` |
| MD5 para contraseñas (CRITICAL) | Argon2 (`Argon2PasswordEncoder` en Java y `Argon2PasswordHasher` en Django) | `config/SecurityBeans.java`, `reportes/auth.py` y `settings.py` |
| Sin MFA (CRITICAL) | TOTP obligatorio para los roles de `mfa.required.roles` | `auth/MfaPolicy.java` y `application.yml` |
| Secretos en el código (CRITICAL) | Variables de entorno (`${JWT_SIGNING_KEY}` y `os.environ[...]`); la API key se quitó del frontend | `JwtService.java`, `settings.py`, `client.js` |
| Contraseña en texto plano (CRITICAL) | La columna `password_hash` guarda un hash Argon2 | `user/User.java` |
| DES/ECB con clave fija (CRITICAL) | `AES/GCM/NoPadding` con IV aleatorio y la clave en una variable de entorno | `payment/CardEncryptor.java` |
| Contraseñas y tokens en los logs (HIGH) | Los logs solo registran el usuario | `AuthController.java`, `reportes/auth.py` |
| TLS desactivado (HIGH) | `server.ssl.enabled: true` con keystore PKCS12 | `application.yml` |
| Sin verificación de checksums (HIGH) | `artifact.checksum.verify: true` | `application.yml` |
| Dependencias con CVE (CRITICAL y HIGH) | log4j 2.23.1, spring 5.3.39, lodash 4.17.21, minimist 1.2.8, django 4.2.16, jinja2 3.1.4, requests 2.32.3 y pyyaml 6.0.2; Struts se eliminó | `pom.xml`, `package.json`, `requirements.txt` |
| Django con `DEBUG=True`, `SECRET_KEY` fijo y `ALLOWED_HOSTS=*` | Todo sale del entorno, con `DEBUG` en false por defecto, cookies `Secure`/`HttpOnly` y redirección a HTTPS | `reportes/settings.py` |

## Debilidades residuales que el motor no reporta

Para ser realista, un proyecto intermedio conserva algunas debilidades menores que las reglas actuales no cubren. Sirven como falsos negativos esperados:

| Ubicación | Debilidad | Severidad real estimada |
|---|---|---|
| `auth/JwtService.java` | El token vive 8 horas y no hay refresh token ni revocación: un token robado sigue siendo válido hasta que expira | Media |
| `frontend/src/api/client.js` | El JWT se guarda en `sessionStorage`. Es mejor que `localStorage`, porque se borra al cerrar la pestaña, pero JavaScript sigue pudiendo leerlo. Lo ideal es una cookie `HttpOnly` | Media-baja |
| `reportes/reportes/views.py` | `login_reportes` no limita los intentos de inicio de sesión | Media |
| `reportes/reportes/settings.py` | Django no envía cabecera CSP (falta `django-csp`) | Media-baja |
| `application.yml` | El rate limiter es global por instancia, no por IP ni por usuario. Un atacante puede agotar el cupo e impedir que otros inicien sesión | Baja |

---

## Decisiones de diseño (importante para interpretar los resultados)

Las reglas del sistema son **regex por línea** y **CONFIG_CHECK por archivo**. Algunas decisiones del código dependen de ese funcionamiento, y conviene conocerlas al analizar los resultados en el PDG:

1. **`@PostMapping("/login") @RateLimiter(name = "auth")` en la misma línea.** La regla CWE-307 busca `RateLimiter`, `Bucket4j` o `throttle` **en la misma línea** del `@PostMapping`, y además **después** de la palabra `login`:
   - Con `@RateLimiter` en la línea siguiente, que es el estilo más común, la regla da un **falso positivo HIGH**.
   - Con `@RateLimiter(name = "login")` también, porque la regex toma la última aparición de `login` y ya no ve `RateLimiter` después.
2. **La variable se llama `rawPassword`, no `password`.** La regla CWE-312 (`String\s+password\s*=`) marcaría como CRITICAL cualquier variable local `String password = ...`, aunque la contraseña nunca se guarde en claro.
3. **La columna se llama `password_hash`.** La regla `@Column.*\bpassword\b` marca cualquier columna llamada exactamente `password`, aunque guarde un hash.
4. **No hay `docker-compose.yml`.** Las reglas CONFIG_CHECK con `must_exist` se evalúan **en cada archivo** `.yml`, `.yaml`, `.properties`, etc. Un `docker-compose.yml` también tendría que declarar `mfa.required.roles` y `artifact.checksum.verify`, o generaría hallazgos CRITICAL y HIGH. En la práctica, un proyecto correcto con un `docker-compose.yml` saldría NON_COMPLIANT.
5. **Las claves `mfa.required.roles` y `artifact.checksum.verify` son un contrato con SegSoft.** El motor no puede verificar que exista MFA en el código: solo revisa que la clave esté declarada. Aquí la clave sí se usa (`MfaPolicy` lee `mfa.required.roles`), pero el sistema aceptaría la clave aunque nadie la usara.

Estos puntos muestran limitaciones del enfoque basado en regex. Son material útil para la sección de limitaciones o trabajo futuro del PDG, por ejemplo usar reglas AST, analizar varias líneas o aplicar los CONFIG_CHECK solo al archivo de configuración principal.
