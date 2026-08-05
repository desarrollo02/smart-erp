# J11-S1-03 — Diagnóstico inicial de Docker

- Fecha: 2026-07-23
- Estado: Completado para el incremento de diagnóstico
- Ambiente: Windows 11, PowerShell, Docker Desktop con backend WSL 2
- Alcance: inspección no destructiva; sin pull, build, arranque ni descarga de imágenes

## Comandos ejecutados

```powershell
Get-Command docker
docker version
docker compose version
docker buildx version
docker context show
docker info --format '...'
docker buildx ls
docker system df
Get-Service com.docker.service
Get-Process | Where-Object ProcessName -match 'docker|com\.docker'
docker context inspect desktop-linux
wsl.exe --status
wsl.exe --list --verbose
```

## Herramientas detectadas

| Componente | Resultado |
|---|---|
| Docker CLI | 29.6.2, API cliente 1.55, Windows amd64 |
| Docker Compose | v5.3.1 |
| Docker Buildx | v0.35.0-desktop.2 |
| Docker Desktop | 4.83.0.234302 |
| Contexto activo | `desktop-linux` |
| Endpoint esperado | `npipe:////./pipe/dockerDesktopLinuxEngine` |
| Backend WSL | versión predeterminada 2 |
| Distribución WSL | `docker-desktop`, detenida |

## Estado del Engine

Docker Engine no estaba disponible durante el diagnóstico:

- `docker version`: salida 1; mostró el cliente y falló al conectar con el servidor.
- `docker info`: salida 1; la tubería `dockerDesktopLinuxEngine` no existía.
- `docker system df`: salida 1 por la misma indisponibilidad.
- servicio `com.docker.service`: instalado, inicio manual y estado `Stopped`.
- procesos Docker Desktop/backend: ninguno activo.
- distribución WSL `docker-desktop`: `Stopped`, versión 2.

El mensaje común fue que no era posible conectar con la API Docker porque no se encontraba la tubería del Engine Linux. La causa se clasifica como **entorno detenido**, no como defecto del proyecto.

## Buildx

`docker buildx ls` terminó con salida 0, pero no pudo consultar los builders porque el Engine estaba detenido:

- `logixone-s1-03-builder`: estado `error`;
- `default`: estado `error`;
- `desktop-linux`: estado `error`.

El diagnóstico no creó, eliminó ni modificó builders.

## Controles de seguridad y alcance

- No se inició Docker Desktop automáticamente.
- No se descargaron imágenes ni archivos.
- No se construyeron imágenes ni contenedores.
- No se modificaron contextos, builders, servicios, WSL ni configuración de Docker.
- No se mostraron credenciales ni variables sensibles.
- El contenido existente de `.tools/` permaneció como baseline local del proyecto.

## Conclusión

Las herramientas cliente cumplen el prerrequisito inicial. El Engine debe iniciarse antes de verificar versión de servidor, BuildKit, plataformas, almacenamiento o digests de imágenes. Después del arranque se repetirán `docker version`, `docker info` y `docker buildx ls`; ese resultado será un gate obligatorio antes de cualquier `pull` o build.

