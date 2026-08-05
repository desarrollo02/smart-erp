# J11-S2-08 — Validación integral y cierre del Sprint 2

- Fecha de ejecución técnica: 2026-07-28
- Estado: Completada con validación independiente diferida por producto; G6 final documentado al pie
- Ambiente: Windows 11 amd64, Java Temurin 21.0.11+10, Maven Wrapper 3.9.16, Docker Engine 29.6.2, Compose 5.3.1, WildFly 41 y PostgreSQL 18.4
- Historia: [J11-S2-08](../sprints/sprint-02/J11-S2-08-validacion-integral-cierre.md)

## Resultado provisional

El baseline posterior a `J11-S2-07` superó desde limpio Maven, Enforcer, ArchUnit, PostgreSQL real, migraciones, JPA/JTA, tres variantes reproducibles del WAR, imágenes OCI, tres trayectorias Compose, health, persistencia tras recreación, aislamiento de empresas, composición de pantallas, auditoría binaria, seguridad y limpieza.

La [Guía de implementación del ERP por empresa](../implementation-guide/README.md) quedó como edición `1.0-rc1`, con 14 capítulos y un ejemplo ficticio completo. El 2026-07-28 el responsable de producto aceptó el estado actual y decidió ejecutar la [ficha de validación independiente](../implementation-guide/VALIDATION.md) cuando exista la demo visual. La validación no se registra como ejecutada ni la guía se etiqueta `1.0`; se conserva como gate explícito de la demo. El cierre actual continúa con G0 final y el PDF obligatorio G6.

## Control documental inicial

La candidata de la guía pasó UTF-8 estricto y comprobación de enlaces:

| Control | Resultado |
|---|---|
| UTF-8 estricto | `OK` |
| enlaces locales inspeccionados | 5 |
| enlaces rotos | 0 |
| capítulos | 14 |
| edición | `1.0-rc1` |

G0 global se repetirá después de incorporar el dictamen independiente y antes de G6.

El G0 global provisional posterior a esta documentación produjo:

| Control | Resultado |
|---|---:|
| Markdown, incluido `AGENTS.md` | 69 |
| errores UTF-8 | 0 |
| archivos con caracteres dañados | 0 |
| enlaces locales | 196 |
| enlaces rotos | 0 |
| historias de Sprint 2 | 9 |
| criterios de aceptación de historias | 154 |
| criterios globales | 18 |
| ADR con estado válido | 5 de 5 |

## Gate Maven y PostgreSQL

Comando:

```powershell
$env:JAVA_HOME=(Resolve-Path '.tools\jdk\jdk-21.0.11+10').Path
$env:MAVEN_USER_HOME=(Resolve-Path '.tools\maven-wrapper-home').Path
.\mvnw.cmd -B "-Dlogixone.postgres.integration=true" clean verify
```

Resultado: código `0`, `BUILD SUCCESS`, 16 de 16 módulos, 136 pruebas, 0 fallos, 0 errores y 0 omitidas en 71,4 segundos.

| Módulo | Pruebas |
|---|---:|
| `plugin-api` | 16 |
| `kernel-api` | 3 |
| `kernel-domain` | 27 |
| `kernel-application` | 37 |
| `migrator` | 16 |
| `kernel-infrastructure-jakarta` | 21 |
| plugins de referencia | 4 |
| `web-shell` | 3 |
| arquitectura | 9 |
| **Total** | **136** |

Las 7 pruebas de migración y 7 de infraestructura usaron PostgreSQL 18.4 real mediante Testcontainers. Se comprobaron base vacía, V1→V2, segunda ejecución, checksum, restricciones, concurrencia, aislamiento, repositorios, `validate`, commit y rollback. Los `WARN` del log pertenecen a casos negativos deliberados; el gate no relajó ni desactivó pruebas.

## Reproducibilidad y composición del WAR

Cada variante se construyó dos veces mediante `clean package`; en ambas ejecuciones coincidieron bytes, SHA-256 y JAR opcionales.

| Variante | Bytes | SHA-256 | Plugins opcionales |
|---|---:|---|---:|
| base | 166767 | `6791BCCFE28B8B4944487C5E24BB2072FB0D36EBE7460ED2E72A2190BC0D0B37` | 0 |
| `with-reference-plugin` | 169015 | `84F1FB72423DFDABEC18AD76E27A6A8411838B06476A42972039096DB201E95A` | 1 |
| `with-screen-customization-plugins` | 173323 | `4248A29ECBA0CE73052F45DBDD2C03A91C7106258976A1EE1E6DB4610100AE99` | 3 |

La variante completa contiene exactamente:

- `reference-plugin-0.1.0-SNAPSHOT.jar`;
- `reference-customization-a-0.1.0-SNAPSHOT.jar`;
- `reference-customization-b-0.1.0-SNAPSHOT.jar`.

El arnés JTA no aparece en el WAR normal ni en ninguna imagen. Tampoco se empaquetan pgJDBC, Hibernate, WildFly ni APIs Jakarta provistas por el servidor.

## Imágenes candidatas

Ambos Dockerfiles pasaron `docker buildx build --check` sin advertencias. Las imágenes locales construidas para `linux/amd64` son:

| Imagen | ID/digest local | Usuario |
|---|---|---|
| `logixone/app:j11-s2-08` | `sha256:389a594984c88c772590dd255488aa21145e9ff23adad2e11811f7155be49f1d` | `jboss` |
| `logixone/app:j11-s2-08-screens` | `sha256:97adf4347397bed1318dbdd6d5611cfd25d9afdd797fc60256385e74f3d7b4e5` | `jboss` |
| `logixone/migrator:j11-s2-08` | `sha256:7e5344b5356d8b5409c63f8dc166bd78fbe213487fa7430206db7d0b397de343` | `10001:10001` |

Son identidades locales de BuildKit, no digests publicados en un registro. Se conservaron como evidencia reproducible; no hubo promoción ni despliegue productivo.

## Compose desde base vacía y runtime JTA

Proyecto aislado: `logixone-s208-empty`, puerto `18088`.

1. PostgreSQL quedó saludable.
2. El migrador aplicó V1 y V2: `migrations_executed=2 schema_version=2`.
3. Liveness y readiness respondieron `200 UP`.
4. El arnés se construyó con el perfil opt-in `jta-runtime-harness`, se copió temporalmente al contenedor verificado y WildFly creó su marcador `.deployed`.
5. REST Assured ejecutó 6 de 6 pruebas, sin fallos ni omitidas: dos de health y cuatro de JTA/aplicación.
6. Dos empresas recibieron `jta_custom_a` y `jta_custom_b`, contribuciones y pantallas distintas; ninguna recibió la personalización de la otra.
7. El fallo de auditoría y la excepción runtime revirtieron empresa y activación de forma atómica.

## Persistencia después de recrear contenedores

Antes de recrear se calcularon conteos y firmas ordenadas del estado. Se ejecutó `down --remove-orphans` sin `--volumes`, se verificó la existencia del volumen y se volvió a ejecutar `up --wait`.

| Control | Antes | Después |
|---|---|---|
| empresas | 3 | 3 |
| activaciones | 3 | 3 |
| migraciones | `1:-1098736951,2:-1309935940` | igual |
| firma de empresas | `9a89c6245e20b8b3f62a84ca1c0e7fca` | igual |
| firma de activaciones | `ea18b9fb7767ca7aacedd8fbc4f14322` | igual |
| readiness | `200` | `200` |

El migrador de la recreación informó `migrations_executed=0 schema_version=2`. La prueba demuestra que recrear contenedores no pisa ni reinicializa un volumen existente.

## Actualización V1→V2 y recuperación de readiness

Proyecto aislado: `logixone-s208-upgrade`, puerto `18089`.

1. La imagen histórica `logixone/migrator:j11-s1-03` aplicó solo V1 con checksum `-1098736951`.
2. La aplicación candidata arrancó deliberadamente sin el nuevo migrador.
3. Liveness respondió `200`; readiness respondió `503` porque V2 estaba pendiente.
4. `logixone/migrator:j11-s2-08` aplicó exactamente una migración y conservó V1.
5. La misma instancia de aplicación recuperó readiness `200`, con el mismo ID de contenedor y 0 reinicios.
6. Las 2 pruebas REST de health quedaron verdes.

Las 4 pruebas JTA aparecen omitidas en este corte específico porque el arnés opt-in no se desplegó; son no aplicables y ya habían pasado sin omisiones en el escenario anterior. El gate integral Maven tuvo 0 omitidas.

## Imagen con personalizaciones físicas

Proyecto aislado: `logixone-s208-screens`, puerto `18090`.

- liveness: `200 UP`;
- readiness: `200 UP`;
- catálogo CDI: `plugin_count=3`;
- identidades: `reference_plugin@1.0.0`, `reference_custom_a@1.0.0`, `reference_custom_b@1.0.0`;
- contenedores de aplicación y PostgreSQL saludables, migrador one-shot finalizado.

## Auditoría de seguridad

| Control | Resultado |
|---|---|
| plugin opcional en imagen base | 0 |
| plugins opcionales en imagen de pantallas | 3 exactos |
| entradas del arnés en ambas imágenes | 0 |
| librerías provistas empaquetadas | 0 |
| contraseña embebida como variable de imagen | no |
| valor real del secreto en historial de imagen | no |
| valor real del secreto en logs runtime | no |
| sentencias SQL en logs runtime | no |

Las respuestas públicas se limitaron al contrato de health. Los endpoints empresariales pertenecían exclusivamente al arnés temporal y no aparecen en la distribución. Las pruebas compararon resultados por empresa y no observaron filtración cruzada.

## Limpieza

Antes de limpiar se verificaron las etiquetas exactas de cada contenedor mediante JSON de `docker inspect`. Después se ejecutó `down --volumes --remove-orphans` solo para los tres proyectos efímeros.

| Proyecto | Contenedores antes/después | Volúmenes antes/después | Redes antes/después |
|---|---|---|---|
| `logixone-s208-empty` | 3 / 0 | 1 / 0 | 2 / 0 |
| `logixone-s208-upgrade` | 2 / 0 | 1 / 0 | 2 / 0 |
| `logixone-s208-screens` | 3 / 0 | 1 / 0 | 2 / 0 |

Los datos sintéticos eliminados ya no son recuperables. Ningún proyecto Compose ajeno fue modificado. Las tres imágenes candidatas permanecen identificadas.

## Incidencias y correcciones

1. La primera consulta del WAR usó `distribution/target` en lugar de `distribution/logixone-war/target`; se localizó el artefacto real. El build estaba verde.
2. La primera firma de activaciones usó `state`; el esquema expone `desired_state`. La consulta falló antes de detener contenedores, se inspeccionó `information_schema` y se repitió correctamente.
3. Dos comandos de inspección usaron una plantilla Go cuyas comillas fueron alteradas por PowerShell. No modificaron recursos. La limpieza se autorizó solo después de validar etiquetas leyendo JSON.
4. El recorrido independiente no puede ser auto-certificado por quien preparó la guía. Se creó una ficha explícita y el estado permanece pendiente.
5. El primer parche combinado de evidencia, ficha e índices no encontró una frase de contexto y fue rechazado atómicamente. Se dividió en cambios pequeños y cada archivo pasó su G0 inmediato.
6. Un control documental intentó canalizar directamente la salida de `foreach`, sintaxis no válida en ese contexto de PowerShell. No modificó archivos; se repitió acumulando resultados y terminó verde.

## Matriz de criterios provisional

| Criterio | Estado | Evidencia |
|---|---|---|
| CA-01 | Cumplido | S2-00 a S2-07 están completados y enlazados. |
| CA-02 | Cumplido | G0 provisional verde; se repite contra el corte final previo al PDF. |
| CA-03 | Cumplido | 136 pruebas, 0 omitidas en `clean verify`. |
| CA-04 | Cumplido | Enforcer y 9 reglas ArchUnit verdes. |
| CA-05 | Cumplido | PostgreSQL 18.4 real para migrador y repositorios. |
| CA-06 | Cumplido | base vacía, V1→V2, reejecución y checksums. |
| CA-07 | Cumplido | JPA `validate`, DDL administrado solo por Flyway. |
| CA-08 | Cumplido | matriz A/B runtime y persistencia sin filtración. |
| CA-09 | Cumplido | casos positivos/negativos, rollback y concurrencia verdes. |
| CA-10 | Cumplido | tres WAR reproducibles 0/1/3. |
| CA-11 | Cumplido | arnés ausente de WAR e imágenes normales. |
| CA-12 | Cumplido | tres trayectorias Compose, migración y persistencia. |
| CA-13 | Cumplido | auditoría de respuestas, imágenes y logs. |
| CA-14 | Cumplido | 0 recursos residuales; imágenes identificadas. |
| CA-15 | Cumplido | JTA no aplicable declarado en health-only; UI/OIDC siguen fuera de alcance. |
| CA-16 | Cumplido | validación atómica y referencias inválidas cubiertas por unitarias. |
| CA-17 | Cumplido | personalización A/B exclusiva y al final. |
| CA-18 | Cumplido con excepción | gates técnicos aceptados, siguiente trabajo definido y excepción registrada. |
| CA-19 | Cumplido | guía versionada `1.0-rc1` e indexada. |
| CA-20 | Cumplido | conceptos y límites preceden procedimientos. |
| CA-21 | Cumplido | recorrido de 14 capítulos. |
| CA-22 | Cumplido | ejemplo Distribuidora Boreal, sin datos ni secretos reales. |
| CA-23 | Diferido por producto | no ejecutado; pasa a ser gate obligatorio de la demo visual. |
| CA-24 | Cumplido | compatibilidad, edición e historial documentados. |
| CA-25 | Cumplido | PDF final regenerado y verificado; identidad registrada al pie. |

## Riesgos y siguientes pasos

- No existe metadata Git; no puede certificarse diff, commit ni procedencia criptográfica del árbol de trabajo.
- Los IDs de imagen son locales; promoción real requiere publicar y fijar digests de registro.
- No existe identidad, autorización HTTP, UI navegable ni dominio ERP productivo; presentarlos como implementados sería incorrecto.
- El siguiente incremento debe comenzar por `J11-S3-00`: planificar identidad autenticada, contexto empresarial confiable y la primera UI mínima, revisando primero proveedor OIDC, modelo de membresía y límites de la demo.
- El siguiente paso permitido es planificar y aceptar `J11-S3-00`. Durante el Sprint de demo se debe ejecutar `VALIDATION.md`, resolver hallazgos y recién entonces elevar la guía a `1.0`.

## G6 - PDF obligatorio de cierre del Sprint 2

| Control | Evidencia final |
|---|---|
| Ruta estable | `docs/output/pdf/guia-estructura-repositorio-logixone.pdf` |
| Edición | cierre Sprint 2, corte `2026-07-28` |
| Inventario documentado | 263 archivos mantenidos; 154 fuentes Java, 69 documentos Markdown y 16 módulos Maven |
| Páginas | 20, tamaño A4, sin páginas vacías |
| Tamaño | 181686 bytes |
| SHA-256 | `8B6E6507E12CD82AE5F1B24256312445D906DFC2A25C51AFDC5815058A095F0D` |
| Metadatos | título, asunto, autor y creador presentes; PDF 1.4, no cifrado, sin formularios ni JavaScript |
| Contenido lógico | texto extraíble en las 20 páginas, 58456 caracteres, 0 caracteres de reemplazo y 0 guiones Unicode no normalizados |
| Navegación | índice visible y marcadores internos presentes |
| Revisión visual | las 20 páginas se renderizaron a PNG; se revisaron portada, índice, encabezados, pies, diagramas, tablas, inventario, cortes y página final |
| Resultado | verde; sin superposiciones, contenido cortado, páginas vacías ni caracteres dañados |

Durante la preparación se corrigieron la convergencia del índice, un símbolo de viñeta no portable, espacios innecesarios, contraste de cabeceras de tablas y separación de recuadros. Después de esas correcciones se regeneró y revisó la ruta estable. Los archivos de trabajo quedaron en `tmp/pdfs/` únicamente durante la revisión y se eliminaron al terminar.
