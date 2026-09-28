# ProyectoPruebas

Tres proyectos de prueba para validar SegSoft de extremo a extremo. Son **la misma tienda en línea** (backend Java/Spring, reportes Django y frontend React) en tres momentos de su remediación de seguridad. Así los resultados se pueden comparar directamente.

| Proyecto | Hallazgos esperados | Estado esperado de las políticas |
|---|---|---|
| `proyecto-vulnerable/` | **43**: 16 CRITICAL, 20 HIGH, 7 MEDIUM | Todas las que tienen reglas activas: **NON_COMPLIANT** |
| `proyecto-intermedio/` | **5**: todos MEDIUM | 3 **REQUIRES_REVIEW** y el resto **COMPLIANT** |
| `proyecto-limpio/` | **0** | Todas **COMPLIANT** |

Todos los resultados se obtuvieron ejecutando el motor Python real (`/internal/execute-rule`) con las 23 reglas activas del snapshot de la base de datos (`segsoft-motorpython/fixtures/rules_snapshot.json`), sobre todos los archivos, igual que lo hace el backend.

## Estructura

```
ProyectoPruebas/
├── proyecto-vulnerable/     ← carpetas que se suben a SegSoft (solo código)
├── proyecto-intermedio/
├── proyecto-limpio/
└── docs/                    ← documentación (fuera de los proyectos analizados)
    ├── proyecto-vulnerable.md                     por qué es vulnerable
    ├── proyecto-intermedio.md                     qué se corrigió y qué queda pendiente
    ├── proyecto-limpio.md                         remediación completa y límites del motor
    └── proyecto-*.expected_findings.yml           hallazgos esperados de cada proyecto
```

La documentación está **fuera** de cada proyecto a propósito. SegSoft ejecuta todas las reglas sobre todos los archivos, incluidos los `.md`, así que los fragmentos de código de ejemplo generarían hallazgos falsos.

## Cómo probar un proyecto

1. Comprimir el proyecto:
   ```bash
   cd ProyectoPruebas
   zip -r proyecto-vulnerable.zip proyecto-vulnerable
   zip -r proyecto-intermedio.zip proyecto-intermedio
   zip -r proyecto-limpio.zip proyecto-limpio
   ```
2. En SegSoft, subir el ZIP desde el Dashboard y esperar a que termine el inventario.
3. Seleccionar **todas las políticas activas** y lanzar el análisis.
4. Comparar los hallazgos con `docs/<proyecto>.expected_findings.yml`.
5. Opcional: generar y exportar el reporte PDF (requiere el rol AUDITOR o SECURITY_ADMIN).

## Qué mide cada proyecto

- **Vulnerable:** **recall**, es decir, si el sistema detecta lo que debe. También cubre el estado NON_COMPLIANT. Incluye una lista de fallas reales que el motor no detecta (falsos negativos).
- **Intermedio:** que la clasificación por severidad funcione, con REQUIRES_REVIEW sin NON_COMPLIANT.
- **Limpio:** **precisión**, es decir, si el sistema reporta algo que no debe. Contiene código seguro parecido a los patrones vulnerables. Cualquier hallazgo aquí es un falso positivo.

Las secciones "Decisiones de diseño" de `proyecto-intermedio.md` y `proyecto-limpio.md` documentan las limitaciones de las reglas regex que se encontraron al construir los proyectos. Son útiles para la sección de limitaciones o trabajo futuro del PDG.
