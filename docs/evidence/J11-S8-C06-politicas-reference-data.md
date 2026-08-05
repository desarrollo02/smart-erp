# J11-S8-C06 — Evidencia de políticas empresariales de datos de referencia

- Fecha: 2026-08-05
- Rama de trabajo: `chore/J11-S8-C04-adopcion-git`
- Estado: implementación y gates técnicos verdes
- Historia: [J11-S8-C06](../sprints/sprint-08/J11-S8-C06-politicas-reference-data.md)

## Resultado

`reference_data` separa la publicación normativa inmutable de la habilitación por
empresa. La ausencia de override significa habilitado con versión cero. Cada
cambio real exige la versión observada, actualiza sólo la fila corriente de la
misma empresa y agrega una revisión append-only V2 con actor, correlación y fecha.

El permiso `reference_data.policy.manage` protege menú, ruta, consulta y mutación.
La UI permite habilitar o inhabilitar referencias existentes, pero no crear ni
editar códigos, nombres, números o publicaciones normativas.

## Gates ejecutados

| Gate | Resultado |
|---|---|
| Servicio, autorización, auditoría y concurrencia | verdes; entradas inexistentes, permiso ausente y versión obsoleta fallan cerradas |
| PostgreSQL/Testcontainers | 5/5; PostgreSQL 18.4, Flyway V1–V4, seis tablas privadas, migración e idempotencia |
| `clean verify` final desde `.tools` | 26/26 módulos, 498 pruebas, 0 fallos/errores/omitidas y 28 ArchUnit |
| Compose real | migrator `Exited (0)`; PostgreSQL, Keycloak y app saludables; segunda migración con cero cambios |
| Health/OIDC | 2/2 health y 4/4 OIDC verdes en el WAR normal |
| JTA aislado | 12/12 en total: 2 health, 6 JTA y 4 OIDC; cero omisiones; arnés retirado y ausente del WAR normal |
| Playwright | `BusinessPartnersVisualIT` 1/1, incluida política XDR, historial, permiso revocado y restauración |
| Responsive | 30 capturas generadas; revisión visual satisfactoria en 375, 720 y 1280 px, sin overflow normal |

La prueba visual dejó `XDR` habilitado en la empresa ficticia con versión 22. Las
últimas revisiones conservan la alternancia 19–22 sin reescritura; recrear varias
veces el contenedor de aplicación no alteró PostgreSQL ni Keycloak.

## Artefactos del baseline

- app: `logixone/app:j11-s8-c07-reference-data`,
  `sha256:52cf22451dc7ff89192a9b88d89e97b26b0e45f508654d67c52b6fd38b83d9fd`,
  501.161.623 bytes, usuario `jboss`;
- migrator: `logixone/migrator:j11-s8-c07-reference-data`,
  `sha256:1b598fb140659a04501a5890c2279c80545cf0115eba0711ef37a30cfdf19c77`,
  105.478.277 bytes, usuario `10001:10001`;
- evidencia visual: [`screenshots/J11-S8-C07/e2e`](screenshots/J11-S8-C07/e2e/).

No se promovieron imágenes, no se desplegó a producción y no se modificó el
instalador Windows anterior.

## Hallazgos corregidos durante el gate

El selector bajo demanda exponía un defecto real de ciclo de vida JSF: el bean es
`RequestScoped` y la fila dinámica ya no existía al decodificar el POST siguiente.
La selección ahora viaja como parámetro HTML explícito del shell y se vuelve a
validar en servidor contra la fuente, la empresa, el permiso y el estado vigente.
Las esperas Playwright se ajustaron a los redirects reales sin relajar conteos ni
valores esperados.

## Pendientes de cierre de Sprint

C06 queda técnicamente validada y el PDF final fue regenerado y revisado contra el
mismo baseline. Producto registró `NO` para el instalador de este corte y conservó
`current` intacto; el Sprint completo aún requiere la validación independiente G7.
