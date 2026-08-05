# J11-S1-03 — Engine e imágenes base fijadas por digest

- Fecha: 2026-07-23
- Estado: Completado para el incremento de Engine y selección de bases
- Ambiente: Windows 11, Docker Desktop con backend WSL 2
- Plataforma objetivo de Sprint 1: `linux/amd64`
- Alcance: validación del Engine y consulta de manifiestos OCI; sin `pull`, build ni creación de contenedores

## Gate del Engine

Se repitió el diagnóstico después de iniciar Docker Desktop.

| Control | Resultado | Salida |
|---|---|---:|
| `docker version` | Cliente y Engine 29.6.2; servidor Linux amd64 | 0 |
| `docker compose version` | v5.3.1 | 0 |
| `docker buildx version` | v0.35.0-desktop.2 | 0 |
| `docker context show` | `desktop-linux` | 0 |
| `docker info` | Engine Linux, `overlayfs`, 8 CPU y 8.193.798.144 bytes de memoria | 0 |
| BuildKit del contexto activo | v0.31.2, estado `running` | 0 |
| `docker system df` | Consulta correcta | 0 |

El builder preexistente `logixone-s1-03-builder` permaneció inactivo y no se modificó. El contexto activo dispone del driver integrado `docker`, suficiente para consultar manifiestos y para el primer build local.

## Criterio de identidad

Las referencias ejecutables de esta historia fijarán simultáneamente:

1. una etiqueta legible y exacta;
2. el digest SHA-256 del manifiesto `linux/amd64`;
3. `platform: linux/amd64` o su equivalente en el comando de build.

El digest del índice OCI multi-arquitectura también se registra como procedencia. No será la identidad ejecutable de Sprint 1 porque plataformas diferentes pueden resolver hijos binariamente distintos bajo el mismo índice.

## Manifiestos verificados

| Rol | Etiqueta consultada | Digest del índice OCI | Digest `linux/amd64` |
|---|---|---|---|
| Builder | `eclipse-temurin:21.0.11_10-jdk-noble` | `sha256:35685c7e23352983a48882d97cd9875f5284c228db71d1e2476e5e6c1bab1080` | `sha256:15ec53e5373fbede42fb2c1ec5b7e3ccbe1b0c9ad232ed67a16ed1b8b47070b6` |
| Runtime del migrador | `eclipse-temurin:21.0.11_10-jre-noble` | `sha256:373787d1d45a87f084fda43e7de0e9acf5eedee049446efac738f13587ec4c64` | `sha256:c978ac6dd6aa90ec21935eb6c848f50ebf9b783160eefdd6accd8d2fd9ebeef1` |
| Runtime de aplicación | `quay.io/wildfly/wildfly:41.0.0.Final-jdk21` | `sha256:4c49269e21c8dd0650e575ba844ec6031637da4a3ec05d31e289fc026e7e3f13` | `sha256:7131f9e6b0d9d2e22caa57ddc729966f552cd7771432a9d4cefc72fbaec8d7d0` |
| Base de datos | `postgres:18.4-bookworm` | `sha256:1961f96e6029a02c3812d7cb329a3b03a3ac2bb067058dec17b0f5596aca9296` | `sha256:16fa100a3a6e92c0556632870455e7f8c6f3df5cefddd67d6b95292732bd7ff0` |

Referencias ejecutables aprobadas para los siguientes incrementos:

```text
docker.io/library/eclipse-temurin:21.0.11_10-jdk-noble@sha256:15ec53e5373fbede42fb2c1ec5b7e3ccbe1b0c9ad232ed67a16ed1b8b47070b6
docker.io/library/eclipse-temurin:21.0.11_10-jre-noble@sha256:c978ac6dd6aa90ec21935eb6c848f50ebf9b783160eefdd6accd8d2fd9ebeef1
quay.io/wildfly/wildfly:41.0.0.Final-jdk21@sha256:7131f9e6b0d9d2e22caa57ddc729966f552cd7771432a9d4cefc72fbaec8d7d0
docker.io/library/postgres:18.4-bookworm@sha256:16fa100a3a6e92c0556632870455e7f8c6f3df5cefddd67d6b95292732bd7ff0
```

## Procedimiento de verificación

Para cada etiqueta se ejecutaron dos consultas independientes:

```powershell
docker buildx imagetools inspect --raw <etiqueta>
docker buildx imagetools inspect <etiqueta>
docker buildx imagetools inspect --raw <etiqueta>@<digest-indice>
```

Se comprobó que:

- la etiqueta resolvía exactamente el digest de índice registrado;
- el índice contenía un único manifiesto `linux/amd64`;
- ese manifiesto coincidía con el digest de plataforma registrado;
- la referencia fijada por digest seguía siendo consultable.

Resultado integrado: **4/4 imágenes verificadas**.

## Motivos de selección

- Temurin 21.0.11+10 coincide con el JDK local validado en `J11-S1-02`; Noble es una base Ubuntu LTS explícita.
- WildFly 41.0.0.Final con JDK 21 es el runtime aprobado por el baseline.
- PostgreSQL 18.4 era la versión menor vigente del major 18 en la fecha de verificación y el major recibe soporte hasta noviembre de 2030.
- `bookworm` evita que una etiqueta genérica cambie implícitamente de distribución base.
- PostgreSQL 18 cambió su `PGDATA` interno; Compose deberá persistir el directorio `/var/lib/postgresql`, no asumir el layout histórico de versiones anteriores.

## Ausencia de descargas de capas

Antes y después de consultar los manifiestos, `docker system df` informó:

- imágenes: 11;
- imágenes activas: 0;
- tamaño: 8.782 GB.

No se ejecutó `docker pull`. Las consultas descargaron únicamente metadatos de registro administrados por Docker. `.tools/` permaneció en 618.099.891 bytes y no se crearon archivos externos del proyecto.

## Fuentes primarias

- [Catálogo oficial de Eclipse Temurin](https://github.com/docker-library/official-images/blob/master/library/eclipse-temurin)
- [Soporte de Eclipse Temurin](https://adoptium.net/support/)
- [Imágenes oficiales de WildFly](https://docs.wildfly.org/wildfly-container/)
- [Política de versiones de PostgreSQL](https://www.postgresql.org/support/versioning/)
- [Imagen oficial de PostgreSQL](https://github.com/docker-library/docs/blob/master/postgres/README.md)

## Siguiente gate

Implementar el primer Dockerfile multi-stage con las referencias aprobadas, construir únicamente el target builder y demostrar que ejecuta Maven Wrapper, completa `verify` y genera el WAR sin utilizar `.tools/` ni `target/` del host.

