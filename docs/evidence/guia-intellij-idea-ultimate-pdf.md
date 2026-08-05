# Evidencia — Guía para levantar Logixone con IntelliJ IDEA Ultimate

- Fecha: 2026-07-30
- Tipo de cambio: documentación operativa y onboarding
- Estado: verificada
- Fuente canónica:
  `docs/runbooks/levantar-logixone-intellij-idea-ultimate.md`
- Generador:
  `tools/generate_intellij_setup_pdf.py`
- Artefacto:
  `docs/output/pdf/guia-levantar-logixone-intellij-idea-ultimate.pdf`

## Alcance

Se creó una guía paso a paso para abrir el reactor en IntelliJ IDEA Ultimate,
configurar Java 21 y Maven Wrapper, preparar configuración/secretos, construir las
imágenes verificadas, operar Compose, validar health, acceder a la interfaz,
entender menú/permisos, conservar volúmenes y diagnosticar fallos.

La guía mantiene Docker/Compose como ejecución oficial. La integración local de
WildFly de IntelliJ queda documentada como opcional y no canónica hasta que exista
una configuración reproducible equivalente a la imagen.

## Verificación de versión y documentación oficial

Consultadas el 2026-07-30:

- JetBrains publicó IntelliJ IDEA 2026.2 como versión estable vigente:
  <https://blog.jetbrains.com/idea/2026/07/whats-fixed-intellij-idea-2026-2/>.
- Importación de proyecto:
  <https://www.jetbrains.com/help/idea/import-project-or-module-wizard.html>.
- Importación y ejecución Maven:
  <https://www.jetbrains.com/help/idea/maven-importing.html> y
  <https://www.jetbrains.com/help/idea/maven-support.html>.
- Servidores de aplicación y WildFly:
  <https://www.jetbrains.com/help/idea/configuring-and-managing-application-server-integration.html>
  y
  <https://www.jetbrains.com/help/idea/run-debug-configuration-jboss-server.html>.
- Docker y Docker Compose:
  <https://www.jetbrains.com/help/idea/docker-run-configurations.html>.

## Comandos del baseline comprobados

`mvnw.cmd --version`:

- Maven: 3.9.16;
- Java: 21.0.11 Eclipse Adoptium;
- Maven home: `.tools/maven-wrapper-home/...`;
- plataforma: Windows 11 amd64;
- resultado: código `0`.

`docker compose --env-file infra/compose/compose.env.local -f
infra/compose/compose.yaml config --quiet`:

- resultado: código `0`;
- no creó contenedores, redes ni volúmenes.

Tags locales de solo lectura:

- `logixone/app:j11-s6-07-closing`:
  `sha256:12e874125851bd304b41369a6b4d38f537014d4d398c7313bee8efbdc57b533d`;
- `logixone/migrator:j11-s6-07-closing`:
  `sha256:45e18b0ef2dd8bebee5c84417c4b1e1a1eed8ca9e5517980c8ab81e4358e69b8`.

No se reconstruyeron imágenes, no se ejecutó `up` y no se repitió el reactor: el
cambio es documental y los comandos destructivos/operativos permanecieron fuera de
esta validación.

## Generación y revisión del PDF

Comando:

```powershell
& 'C:\Users\sdiaz\.cache\codex-runtimes\codex-primary-runtime\dependencies\python\python.exe' `
  tools\generate_intellij_setup_pdf.py
```

Render:

- motor: Poppler `pdftoppm`;
- resolución: 120 DPI;
- páginas renderizadas: 12;
- carpeta local no versionada:
  `.tools/tmp/intellij-guide-render-v5/`;
- revisión: todas las páginas inspeccionadas visualmente;
- resultado: portada, índice, encabezados, pies, comandos, callouts, listas,
  enlaces y cortes correctos; sin overflow, páginas vacías ni caracteres dañados.

## Validación técnica final

| Control | Resultado |
|---|---|
| Páginas | 12 |
| Tamaño | 142352 bytes |
| SHA-256 | `acafdde09cb78a6770d3a701c6ecdc7a40820e1c87ea1fd8ec417e04ac00acf1` |
| Título | `Levantar Logixone con IntelliJ IDEA Ultimate` |
| Autor | `Proyecto Logixone` |
| Asunto | `Guía para levantar Logixone con IntelliJ IDEA Ultimate 2026.2` |
| Texto extraíble | 21910 caracteres |
| Mínimo por página | 717 caracteres |
| Páginas vacías | 0 |
| Caracteres de reemplazo | 0 |
| CIDs sin mapear | 0 |
| AcroForm | ausente |
| JavaScript embebido | ausente |

## Documentación integrada

- se agregó la guía y su PDF al índice de `docs/runbooks/`;
- se actualizó la guía de implementación a `1.0-rc37`;
- se actualizó la ficha de validación independiente a `1.0-rc37`;
- no se modificaron contratos, código productivo, migraciones ni infraestructura.
