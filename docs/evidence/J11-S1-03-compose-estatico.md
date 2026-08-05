# J11-S1-03 — Validación estática de Compose

- Fecha: 2026-07-23
- Estado: Verde
- Alcance: cuarto incremento de `J11-S1-03`
- Plataforma: Docker Desktop, contexto `desktop-linux`, `linux/amd64`
- Docker Engine: 29.6.2
- Docker Compose: 5.3.1

## Objetivo

Declarar la topología `postgres` → `migrator` → `app`, su configuración externa, el secreto por archivo y la persistencia de PostgreSQL 18, y demostrar que la configuración es válida antes de crear recursos Docker.

## Estado inicial y límites del corte

- La imagen local `logixone/app:j11-s1-03` ya existía y había pasado el smoke test del incremento anterior.
- Todavía no existe una imagen ejecutable del migrador.
- No existe deliberadamente el archivo real `.tools/secrets/postgres-password.txt`.
- Este corte autoriza únicamente inspección y `docker compose config`; no autoriza arrancar la composición.

## Archivos incorporados

- `infra/compose/compose.yaml`
- `infra/compose/compose.env.example`
- `docs/runbooks/compose.md`

El ejemplo contiene nombres y valores no sensibles. La contraseña se referencia como secreto por archivo y queda fuera del repositorio bajo `.tools/`.

## Topología validada

| Servicio | Imagen | Condición de entrada | Salud o terminación |
|---|---|---|---|
| `postgres` | PostgreSQL 18.4 Bookworm fijado por digest de `linux/amd64` | Ninguna | `pg_isready` |
| `migrator` | Tag local sustituible por digest de registro | `postgres` saludable | Proceso one-shot, `restart: "no"` |
| `app` | `logixone/app:j11-s1-03`, sustituible por digest | PostgreSQL saludable y migrador terminado con éxito | Respuesta HTTP de WildFly |

PostgreSQL monta el volumen nombrado `postgres-data` en `/var/lib/postgresql`, ruta requerida por el layout de la imagen oficial desde PostgreSQL 18. La red de datos es interna y el puerto HTTP se publica solamente en `127.0.0.1` por defecto.

## Pruebas ejecutadas

Todos los comandos se ejecutaron el 2026-07-23 desde `C:\cosme\LogixoneJakarta11`.

| Prueba | Resultado | Código |
|---|---|---:|
| Inspección de la imagen runtime | `linux/amd64`, usuario `jboss`, ID `sha256:c812ebebc6fc430d152922536bc57fe520661969e76032bbe58ea71407b77b34` | 0 |
| Disponibilidad de `/usr/bin/curl` en el runtime | Encontrado; apto para el health check operativo | 0 |
| `docker compose -f infra/compose/compose.yaml config --quiet` | Configuración predeterminada válida | 0 |
| `docker compose --env-file infra/compose/compose.env.example -f infra/compose/compose.yaml config --quiet` | Archivo de ejemplo válido | 0 |
| Renderizado JSON y 18 aserciones estructurales | Servicios, plataforma, digest, volumen, dependencias, salud, secreto, red y bind correctos | 0 |
| Sustitución de imágenes por `repositorio@sha256:digest` ficticios | `app` y `migrator` conservaron exactamente las referencias inmutables proporcionadas | 0 |
| Inventario Docker con etiqueta de proyecto `logixone` | 0 contenedores, 0 volúmenes y 0 redes | 0 |
| `mvnw.cmd -B verify` con el `JAVA_HOME` heredado | Enforcer detuvo correctamente el build: Java 8 no satisface `[21,22)` | 1 esperado por diagnóstico |
| `mvnw.cmd -B verify` con `.tools/jdk/jdk-21.0.11+10` | Reactor 14/14 y `BUILD SUCCESS` | 0 |
| SHA-256 del WAR después del gate | `23C935CEB30AB75CCDD72A9CF96658E6A0B63B50B4A552AE908D7B7F1488BD39`, sin cambio | 0 |

Las 18 aserciones verificaron:

1. exactamente los servicios `app`, `migrator` y `postgres`;
2. PostgreSQL con la referencia aprobada `docker.io/library/postgres:18.4-bookworm@sha256:16fa100a3a6e92c0556632870455e7f8c6f3df5cefddd67d6b95292732bd7ff0`;
3. plataforma `linux/amd64` en los tres servicios;
4. volumen `postgres-data` montado en `/var/lib/postgresql`;
5. health check de PostgreSQL mediante `pg_isready`;
6. migrador dependiente de PostgreSQL saludable y sin reinicio;
7. aplicación dependiente de PostgreSQL saludable y migrador exitoso;
8. health check HTTP operativo de la aplicación;
9. publicación predeterminada solo en loopback;
10. red interna;
11. ausencia de variables de contraseña en claro;
12. secreto resuelto bajo `.tools/secrets`;
13. ausencia de `latest`;
14. sustitución exacta de las dos imágenes promovibles por digest.

## Seguridad y reproducibilidad

- La contraseña no aparece en Compose, el ejemplo ni la configuración renderizada; solo aparece la ruta `/run/secrets/postgres_password`.
- El secreto real no fue creado, leído ni registrado en la evidencia.
- Las imágenes promovibles aceptan referencias completas por variables externas. Los tags locales son valores de desarrollo hasta que exista un registro OCI; no se consideran identidad de promoción.
- La referencia de PostgreSQL permanece fijada por digest y no usa `latest`.
- `docker compose config` no descargó imágenes ni creó recursos.

## Incidencia encontrada

Una inspección efímera del runtime recibió inicialmente acceso denegado al archivo de configuración y al pipe de Docker por el sandbox. Se repitió el mismo comando con autorización explícita, terminó con código 0 y no cambió el proyecto ni creó recursos persistentes.

El primer gate Maven final heredó `JAVA_HOME=C:\Program Files\Java\jdk1.8.0_202\jre`. Enforcer lo rechazó antes de compilar, como exige el baseline. Se seleccionó explícitamente el JDK Temurin 21.0.11 ya almacenado en `.tools/jdk/`, sin descargar nada ni modificar configuración del sistema, y la repetición terminó 14/14 verde. El fallo y su corrección confirman que el build no acepta silenciosamente una Java incompatible.

## Criterios demostrados en este corte

- `CA-09`: topología Compose válida con los tres servicios.
- `CA-03`: gate Maven final 14/14 con Java 21 y WAR canónico sin cambios.
- `CA-10`: declaración de versión/digest, health check y volumen de PostgreSQL; la persistencia real queda pendiente.
- `CA-12`: orden declarativo correcto; el comportamiento real queda pendiente.
- `CA-16`: configuración externa y ejemplo sin secretos.
- `CA-17`: Compose y configuración renderizada sin secretos; faltan controles de las imágenes futuras.
- `CA-19`: pruebas, ambiente, resultados y códigos registrados.

`CA-11`, `CA-13`, `CA-15`, la parte dinámica de `CA-10`/`CA-12` y el cierre de `CA-20`/`CA-21` siguen abiertos.

## Siguiente paso permitido

Implementar la imagen mínima del migrador, probarla de forma aislada y volver a ejecutar los controles estáticos. Solo después se podrá crear el secreto local y arrancar PostgreSQL para probar orden, idempotencia y fallo seguro.
