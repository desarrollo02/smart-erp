# ADR-0004 — Docker, IaC y promoción por digest

- Estado: Aceptado
- Fecha: 2026-07-23
- Historia: `J11-S1-01`

## Contexto

Desarrollo, pruebas y producción deben ejecutar el mismo software y una topología lógica equivalente. Las etiquetas de imágenes son mutables; por sí solas no identifican de manera inmutable qué bits fueron construidos o desplegados.

## Decisión

### Construcción

- Se utilizará un Dockerfile multi-stage.
- La etapa de build usará Java 21 y Maven Wrapper; ejecutará las pruebas y producirá el WAR.
- La etapa runtime partirá de la imagen oficial `quay.io/wildfly/wildfly:41.0.0.Final-jdk21`.
- Todas las imágenes base se escribirán como `etiqueta@sha256:digest`.
- La etiqueta legible documentará la intención; el digest fijará la identidad real.
- No se utilizarán `latest` ni etiquetas flotantes en archivos ejecutables del proyecto.
- Los digests concretos se resolverán, verificarán y registrarán en `J11-S1-03` porque forman parte del código de infraestructura probado.

### Promoción

- El pipeline construirá una única imagen de aplicación por versión.
- La imagen se probará y se promoverá entre ambientes usando exactamente el digest de esa imagen, sin reconstruir.
- Desarrollo, pruebas y producción usarán las mismas imágenes de aplicación, migrador y base de datos para una versión aprobada.
- Las diferencias de entorno estarán limitadas a configuración, escala, endpoints, almacenamiento y proveedores de secretos declarados.
- Un cambio de digest de una base será un cambio explícito, revisable y sometido a toda la matriz de pruebas.

### Topología declarada

La topología lógica mínima tendrá:

1. un trabajo de migración que termina correctamente;
2. una o más instancias de aplicación;
3. PostgreSQL con almacenamiento persistente;
4. configuración y secretos externos.

Compose será la declaración ejecutable inicial para desarrollo y pruebas. Producción conservará los mismos componentes, imágenes, dependencias y health checks; cualquier diferencia necesaria deberá estar documentada como código y mediante ADR.

La topología de red separa dos responsabilidades:

- `backend` es interna y conecta PostgreSQL, migrador y aplicación;
- `edge` es una red bridge no interna usada únicamente por la aplicación para materializar la entrada HTTP;
- PostgreSQL y migrador no participan de `edge` y no publican puertos;
- el bind HTTP local se limita por defecto a `127.0.0.1` y puede cambiarse únicamente mediante configuración externa.

Esta separación mantiene aislado el plano de datos y evita depender de que Docker publique puertos desde una red marcada como interna.

### Configuración, seguridad y operación

- No se incorporarán credenciales en imágenes, Dockerfile, Compose versionado ni repositorio.
- El contenedor conservará el usuario no privilegiado de la imagen WildFly.
- La aplicación será stateless respecto al filesystem del contenedor.
- PostgreSQL usará un volumen explícito.
- Se implementarán health checks de liveness y readiness.
- Readiness dependerá de configuración válida, base accesible y migraciones aplicadas.
- `.dockerignore` excluirá artefactos locales, VCS, secretos y contenido innecesario.
- Antes de producción se generarán SBOM, metadatos de procedencia y un procedimiento de actualización controlada de digests.

### Recuperación

- El artefacto anterior se conserva por digest y puede redeplegarse.
- La base de datos no se revierte automáticamente; las migraciones deben ser compatibles con el rollback definido o incluir un runbook específico.
- La persistencia debe sobrevivir a la recreación del contenedor de aplicación y de PostgreSQL.

## Alternativas consideradas

### Construir una imagen diferente por ambiente

Se descarta porque impide afirmar que producción ejecuta el artefacto probado.

### Usar solamente etiquetas de versión

Se descarta como identidad canónica porque una etiqueta puede cambiar de objetivo.

### Configuración y secretos dentro de la imagen

Se descarta por seguridad, rotación y divergencia entre ambientes.

### Instalación manual del servidor

Se descarta porque introduce estado no declarado e impide reproducibilidad.

## Consecuencias

- Actualizar una base exige obtener un nuevo digest y volver a probar.
- La promoción necesita un registro OCI que permita referenciar digests.
- Los archivos IaC y la matriz de variables son parte del producto.
- La estrategia gana reproducibilidad, pero no recibe parches de base automáticamente; las actualizaciones serán deliberadas y frecuentes.

## Resolución de imágenes para Sprint 1

`J11-S1-03` fijó `linux/amd64` como plataforma ejecutable del Sprint 1. Los archivos Docker usarán la etiqueta exacta junto con el digest del manifiesto de plataforma; el digest del índice multi-arquitectura se conserva en evidencia como procedencia.

| Rol | Etiqueta legible | Digest ejecutable `linux/amd64` |
|---|---|---|
| Builder | `eclipse-temurin:21.0.11_10-jdk-noble` | `sha256:15ec53e5373fbede42fb2c1ec5b7e3ccbe1b0c9ad232ed67a16ed1b8b47070b6` |
| Migrador | `eclipse-temurin:21.0.11_10-jre-noble` | `sha256:c978ac6dd6aa90ec21935eb6c848f50ebf9b783160eefdd6accd8d2fd9ebeef1` |
| Aplicación | `quay.io/wildfly/wildfly:41.0.0.Final-jdk21` | `sha256:7131f9e6b0d9d2e22caa57ddc729966f552cd7771432a9d4cefc72fbaec8d7d0` |
| Base de datos | `postgres:18.4-bookworm` | `sha256:16fa100a3a6e92c0556632870455e7f8c6f3df5cefddd67d6b95292732bd7ff0` |

PostgreSQL 18.4 es la versión menor vigente del major 18 en la fecha de selección. Compose persistirá `/var/lib/postgresql` para respetar el layout introducido por PostgreSQL 18. Cambiar plataforma, etiqueta o digest exige una modificación explícita de infraestructura y repetir G4.

La trazabilidad completa de índices OCI, comandos y fuentes está en [J11-S1-03 — Engine e imágenes base](../evidence/J11-S1-03-engine-imagenes-base.md).

## Resolución de health para Sprint 1

`J11-S1-06` materializó los health checks previstos sin cambiar la topología:

- `GET /logixone/health/live` indica que el WAR y Jakarta REST pueden responder y no depende de servicios externos.
- `GET /logixone/health/ready` exige catálogo CDI inicializado, configuración válida, PostgreSQL accesible y migración `core` aplicada.
- Readiness valida `core.flyway_schema_history` y `core.system_metadata`; el WAR no ejecuta Flyway ni modifica el esquema.
- Los fallos devuelven `503 DOWN` con nombres y estados controlados. Configuración, credenciales, rutas y mensajes de excepción permanecen fuera de la respuesta.
- El health check de Compose usa `curl --fail` sobre readiness, timeout de 8 segundos y probes JDBC acotados a un segundo por operación.
- Una caída real de PostgreSQL dejó liveness en `200 UP`, readiness en `503 DOWN` y permitió recuperación a `200 UP` sin reiniciar la aplicación.

## Verificación

`J11-S1-03` debe demostrar:

- build multi-stage desde un checkout limpio;
- bases fijadas por digest y sin etiquetas flotantes ejecutables;
- ausencia de secretos versionados o embebidos;
- validación de Compose;
- migración anterior a readiness;
- arranque y health checks;
- recreación de contenedores conservando los datos;
- identidad de la imagen final registrada por digest.

## Fuentes

- [Docker build best practices](https://docs.docker.com/build/building/best-practices/)
- [WildFly Images](https://docs.wildfly.org/wildfly-container/)
- [WildFly 41 release notes](https://www.wildfly.org/news/2026/07/16/WildFly-41-is-released/)
