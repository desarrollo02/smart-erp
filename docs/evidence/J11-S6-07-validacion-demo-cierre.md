# Evidencia J11-S6-07 - Validación integral y demo de Sprint 6

- Fecha: 2026-07-30
- Estado: G0-G6 verdes; G7 independiente pendiente
- Historia: [J11-S6-07](../sprints/sprint-06/J11-S6-07-validacion-demo-cierre.md)
- Demo: [runbook de cierre](../runbooks/demo-cierre-sprint-06.md)

## Resultado ejecutivo

El primer plugin productivo quedó demostrado sobre WildFly 41, PostgreSQL 18.4,
Keycloak 26.7.0 y la imagen verificada final. Directorio, alta y ficha de Socios
Comerciales ejecutaron operaciones persistentes reales; la activación por empresa
se negó cuando correspondía y volvió a habilitarse sin pérdida de datos.

La demo visual está disponible. El estado no equivale al cierre formal: la guía
candidata aún necesita el recorrido de una persona independiente registrado en
`docs/implementation-guide/VALIDATION.md`.

## G0 - Documentación y trazabilidad

El validador inicial recorrió 170 Markdown y encontró:

- enlaces rotos: 0;
- errores UTF-8: 0;
- mojibake: 0;
- filtraciones de secretos: 0.

Después de incorporar runbook, evidencia, retrospectiva, siguiente Sprint y PDF se
repitió el mismo gate sobre el baseline final. El resultado actualizado se registra
sobre 174 Markdown: 0 enlaces rotos, 0 errores UTF-8, 0 archivos con mojibake y 0
filtraciones de secretos.

## G1 - Reactor y arquitectura

Con `JAVA_HOME` fijado al JDK 21.0.11+10 validado en `.tools/jdk/`:

```powershell
.\mvnw.cmd -B clean verify
.\mvnw.cmd -B -Pwith-business-partners-demo clean verify
```

Ambos reactores terminaron con `BUILD SUCCESS`, 20/20 módulos. ArchUnit ejecutó
17/17 escenarios sin fallos ni omisiones. La primera invocación heredó por error
Java 8 del sistema y se detuvo en Maven Enforcer antes de compilar; se corrigió el
ambiente y se repitió el gate completo con Java 21.

## G2 - Composición física

La variante base no empaqueta implementaciones de plugins ni proveedores del
migrador. La variante `with-business-partners-demo` contiene exactamente:

1. `business_partners`;
2. `reference_plugin`;
3. `reference_customization_a`;
4. `reference_customization_b`.

WAR y migrador derivan del mismo perfil. El WAR mide 792107 bytes, tiene SHA-256
`D4BA9C2DF4CA29AAA59375B90775D59AFBA7EC8082A47347EE6CB960DC107095`
y el mismo hash dentro de la imagen final. No contiene pgJDBC, JUnit, Playwright,
Testcontainers, REST Assured ni el arnés JTA en `WEB-INF/lib`.

## G3 - PostgreSQL, JPA y migraciones

El perfil PostgreSQL/Testcontainers final terminó verde:

| Módulo | Unitarias | PostgreSQL | Total |
|---|---:|---:|---:|
| `business-partners` | 31 | 14 | 45 |
| `kernel-infrastructure-jakarta` | 16 | 13 | 29 |
| `migrator` | 19 | 12 | 31 |

Total focalizado: 105 pruebas, 0 fallos, 0 errores y 0 omitidas. Cubrió esquema
privado, JPQL estricto, aislamiento, concurrencia, versiones, restricciones,
checksums, bases vacías/actualizadas e idempotencia. El intento de duplicar una
personalización exclusiva produjo la violación esperada de la restricción y la
prueba quedó verde.

El arnés temporal JTA ejecutó previamente 6/6 escenarios verdes después de
garantizar que el código de rol aleatorio siempre comienza con letra. El WAR de
prueba fue retirado y no forma parte de la imagen final.

## G4 - Docker, Compose, health, OIDC y persistencia

Ambos Dockerfiles terminaron `buildx --check` sin advertencias. Las imágenes se
construyeron en modo `verified` y con el mismo perfil:

| Artefacto | Tag local | Digest local |
|---|---|---|
| aplicación | `logixone/app:j11-s6-07-closing` | `sha256:12e874125851bd304b41369a6b4d38f537014d4d398c7313bee8efbdc57b533d` |
| migrador | `logixone/migrator:j11-s6-07-closing` | `sha256:45e18b0ef2dd8bebee5c84417c4b1e1a1eed8ca9e5517980c8ab81e4358e69b8` |

Los volúmenes `logixone_postgres-data` y `logixone_keycloak-data` permanecieron
presentes. El migrador se ejecutó dos veces y en ambas informó:

- `core`: versión 6, migraciones ejecutadas 0;
- `business_partners`: versión 1, migraciones ejecutadas 0;
- `reference_plugin`: versión 1, migraciones ejecutadas 0.

Se recreó únicamente `app`. Antes y después coincidieron:

- participantes: 23;
- canales generales: 6;
- contactos: 6;
- decisiones persistidas de plugin: 3.

El contenedor final usa exactamente el digest de aplicación registrado. Liveness
respondió HTTP 200 `UP`; readiness respondió HTTP 200 con `catalog`,
`configuration`, `database`, `migrations` y `oidc-configuration` en `UP`. Los logs
contienen el datasource, `WFLYSRV0010` y `WFLYSRV0025`, sin marcadores de error de
despliegue.

La matriz runtime sobre la imagen final terminó:

- health: 2/2;
- OIDC: 4/4, aceptando el token válido y rechazando audience, issuer y expiración
  inválidos;
- 0 fallos y 0 errores.

Una primera invocación OIDC usó rutas relativas a los secretos desde el submódulo
y no alcanzó los casos del producto. Se corrigió el comando a rutas absolutas y se
repitió la matriz completa; la ejecución incompleta no se cuenta como verde.

## G5 - Seguridad, demo y responsive

La suite final se ejecutó contra la imagen verificada, no contra un WAR copiado:

- `BusinessPartnersVisualIT`: 1/1;
- `VisualDemoIT`: 5/5;
- total: 6/6, 0 fallos, 0 errores, 0 omitidos.

El recorrido creó un participante ficticio, lo buscó y abrió, actualizó nombres,
asignó roles cliente/proveedor, agregó identificación, dirección, canal y contacto,
ejecutó el ciclo de vida, desactivó el plugin desde administración, comprobó la
denegación funcional y lo reactivó en un bloque de restauración.

Las 35 capturas de `docs/evidence/screenshots/J11-S6-07-closing/e2e/` cubren:

- administración en 375, 720 y 1280 px;
- personalización A y B;
- directorio, alta y ficha de Socios Comerciales en los tres rangos;
- identificaciones, direcciones y contactos;
- denegación por plugin inactivo.

Playwright verificó hojas Material cargadas, labels y
`document.documentElement.scrollWidth <= viewport + 1`. La revisión visual no
encontró contenido cortado, overflow horizontal, acciones perdidas ni marcadores
técnicos vacíos. La página de auditoría es extensa por la cantidad de eventos, pero
mantiene jerarquía, ancho y acciones correctos.

## G6 - Documentación, retrospectiva y PDF

Verde. Se actualizaron la guía de implementación, el runbook de demo, la
retrospectiva, el backlog y la planificación de Sprint 7. El PDF obligatorio quedó
en `docs/output/pdf/guia-estructura-repositorio-logixone.pdf` con:

- 44 páginas A4;
- 251051 bytes;
- SHA-256
  `7D3DB14EC163575631F040D7551ADBE4BFB84E5D6FB020DE97889580FBC59DC0`;
- título `Logixone Jakarta 11 - Guía de estructura - Cierre técnico Sprint 6`;
- autor `Proyecto Logixone Jakarta 11`;
- asunto e identificadores de Sprint 6 correctos;
- texto extraíble en las 44 páginas, sin páginas vacías, caracteres de reemplazo,
  controles extraños, cifrado, formulario ni JavaScript;
- render de las 44 páginas a PNG y revisión visual completa de portada, índice,
  encabezados, pies, tablas, comandos, cortes y caracteres, sin defectos.

El PDF estable es byte a byte idéntico al candidato renderizado y revisado.

## G7 - Validación independiente

Pendiente. Requiere una persona que no haya implementado las capacidades evaluadas
y que complete la ficha de la guía sin asistencia oral de sus autores. Hasta ese
dictamen:

- Sprint 6 no se declara formalmente cerrado;
- la guía permanece `1.0-rc`;
- las imágenes no se promueven ni publican;
- no se autoriza producción.

## Retrospectiva técnica

### Funcionó bien

- separar directorio, alta y ficha convirtió la primera UI productiva en un flujo
  ERP comprensible sin romper el contrato neutral;
- el mismo perfil físico para WAR y migrador evitó divergencia de plugins;
- la prueba real de desactivar/reactivar demostró que seguridad y conservación de
  datos no dependen de ocultar controles;
- validar cada defecto con la prueba más pequeña permitió aislar JSF, JTA y OIDC.

### Hallazgos

- formularios independientes dentro de `ui:repeat` perdían valores cuando dos
  secciones quedaban renderizadas; una forma estable por pestaña conserva el
  estado JSF;
- argumentos de método dentro del `ui:repeat` administrativo no eran una frontera
  confiable con bean request-scoped; campos nativos tipados y un comando sin
  argumentos estabilizaron la activación;
- `required` de JSF a nivel de toda la ficha bloqueaba acciones no relacionadas;
  la validación requerida pertenece a la interacción específica del adaptador;
- las rutas de secretos usadas por módulos Maven deben ser absolutas.

### Acción para Sprint 7

Planificar `commercial_catalog` comenzando por caracterización, decisiones de
dominio y contratos públicos. Antes de diseñar tablas o UI se deben separar
producto, servicio, unidad, clasificación, impuesto y precio; no se adelantará
inventario, compras, ventas ni documentos.
