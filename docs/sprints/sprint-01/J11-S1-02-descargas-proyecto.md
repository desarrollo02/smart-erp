# J11-S1-02 — Adenda: descargas dentro del proyecto

- Fecha: 2026-07-23
- Estado: Completado
- Origen: directiva posterior del usuario

## Objetivo

Mantener dentro de `C:\cosme\LogixoneJakarta11` todos los binarios, cachés y dependencias descargados para el proyecto, sin versionarlos ni incluirlos en artefactos.

## Estado inicial

Después de validar `J11-S1-02` existían seis ubicaciones externas bajo `C:\tmp`:

| Contenido | Bytes |
|---|---:|
| JDK 21 ZIP | 205.073.954 |
| Maven 3.9.16 ZIP | 9.395.475 |
| JDK 21 extraído | 343.822.457 |
| Primer caché Maven Wrapper | 10.869.833 |
| Caché Maven Wrapper verificado | 10.869.833 |
| Repositorio Maven de dependencias | 33.619.664 |

Total inventariado: 613.651.216 bytes.

## Layout aprobado

```text
.tools/
├── downloads/
├── jdk/
├── maven-wrapper-home/
├── maven-repository/
├── cache/
│   └── maven-wrapper-bootstrap/
└── tmp/
```

## Criterios de aceptación

- Las seis ubicaciones externas se trasladan sin pérdida a `.tools/`.
- Los ZIP conservan sus SHA-256 verificados.
- Las rutas originales dejan de existir después del traslado exitoso.
- `.tools/` queda ignorado por Git y excluido del contexto Docker mediante `.dockerignore`.
- Maven descarga dependencias en `.tools/maven-repository`.
- El Wrapper usa `.tools/maven-wrapper-home` durante las validaciones del proyecto.
- `mvnw.cmd verify` queda verde usando únicamente las herramientas dentro del proyecto.
- La política queda registrada en `AGENTS.md` y en el runbook.

## Pasos ejecutados

1. Se inventariaron las seis ubicaciones, tipos y tamaños.
2. Se verificó nuevamente el SHA-256 de los ZIP de JDK y Maven.
3. Se definió el layout `.tools/` sin eliminar ni consolidar contenido antes de validar el traslado.
4. Se prepararon las reglas operativas, Git, Maven y el runbook antes de mover archivos.
5. Se instaló la política y se creó `.mvn/maven.config`.
6. Se trasladaron los seis elementos conservando 613.651.216 bytes y se confirmó que las rutas originales dejaron de existir.
7. Se verificaron nuevamente los SHA-256 de los ZIP en su destino.
8. Se ejecutó Maven Wrapper usando el JDK y caché ubicados en `.tools/`.
9. `mvnw.cmd verify` completó 14/14 proyectos y mantuvo el SHA-256 del WAR.
10. Se confirmó que el repositorio Maven efectivo está dentro del proyecto.
11. Se añadió `.dockerignore` y se verificó que excluye `.tools/` del contexto de construcción.

## Validaciones

| Control | Resultado |
|---|---|
| Elementos trasladados | 6/6 |
| Rutas originales restantes | 0 |
| Destinos ausentes | 0 |
| Bytes conservados | 613.651.216 |
| SHA-256 JDK ZIP | Coincide |
| SHA-256 Maven ZIP | Coincide |
| Maven Wrapper | 3.9.16 |
| Java | 21.0.11 |
| Repositorio efectivo | `.tools/maven-repository` |
| Contexto Docker | `.tools/` excluido por `.dockerignore` |
| Reactor | `BUILD SUCCESS`, 14/14 |
| WAR | SHA-256 sin cambios |

## Siguiente paso permitido

La adenda está cerrada. Todas las descargas futuras deben cumplir esta política antes de usarse.
