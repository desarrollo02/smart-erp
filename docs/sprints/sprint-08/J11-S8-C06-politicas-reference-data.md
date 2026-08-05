# J11-S8-C06 — Políticas empresariales auditadas de datos de referencia

- Estado: Completada
- Fecha: 2026-08-05
- Épica: [Datos de referencia normativos](../../backlog/epica-datos-referencia-normativos.md)
- Corte: RD-04
- Plugin: `reference_data`

## Objetivo

Completar la administración empresarial de las anulaciones de país y moneda que
el esquema V1 ya anticipó, sin modificar publicaciones normativas ni permitir
códigos arbitrarios.

## Decisiones del corte

- el permiso de mutación es `reference_data.policy.manage`, separado de
  `reference_data.view`;
- ausencia de anulación significa `ENABLED` con versión efectiva `0`;
- sólo se puede cambiar un código existente en la publicación corriente;
- cada cambio exige la versión observada y falla cerrado ante concurrencia;
- V2 conserva cada cambio en historia append-only con empresa, catálogo, código,
  versión, estado, actor, correlación y fecha;
- la auditoría técnica registra identificadores, resultado y versiones, nunca
  nombres personales o labels normativos;
- la ruta `/reference-data` continúa siendo propiedad del plugin y muestra las
  acciones únicamente después de revalidar permiso y empresa en el servidor.

## Alcance

1. modelo y puerto de política independientes de Jakarta;
2. servicio de aplicación autorizado y auditado;
3. migración privada V2 e implementación JPA/JTA;
4. consulta de historia por empresa y código;
5. administración visual neutral sobre la pantalla existente;
6. pruebas unitarias, migración, PostgreSQL, autorización negativa y responsive.

## Fuera de alcance

- importar o promover publicaciones completas, reservado para RD-05;
- decidir la representación de `minor unit = N.A.` de ISO 4217;
- búsqueda bajo demanda y umbral de listas grandes;
- actualización automática desde Internet;
- modificar códigos, nombres o publicaciones normativas desde el navegador.

## Criterios de aceptación

- **CA-01:** `reference_data.policy.manage` aparece en el descriptor, protege la
  ruta administrativa y no autoriza operaciones de otros plugins.
- **CA-02:** deshabilitar y volver a habilitar un código conocido afecta sólo a la
  empresa autorizada y conserva referencias históricas.
- **CA-03:** código inexistente, empresa distinta, permiso ausente y versión
  obsoleta fallan cerrados sin cambios parciales.
- **CA-04:** repetición del mismo estado es idempotente y queda auditada como
  `UNCHANGED`.
- **CA-05:** cada cambio crea una revisión append-only y un evento técnico con la
  misma empresa, actor, correlación y versiones.
- **CA-06:** V1 no se modifica; V2 es inmutable, idempotente mediante Flyway y
  validada desde una base nueva y desde V1.
- **CA-07:** la UI no acepta altas de códigos, revalida permiso en servidor y
  distingue habilitado/inhabilitado e historia.
- **CA-08:** pruebas de módulo, PostgreSQL, arquitectura, `mvn verify`,
  Docker/Compose y Playwright responsive quedan verdes antes de cerrar la historia.

## Secuencia de validación

Cada corte se prepara en el índice y se materializa bajo
`.tools/tmp/validation/J11-S8-C06/`. Las pruebas se ejecutan con el Maven Wrapper
raíz y `-f` contra esa materialización. No se usan toolchains ni servicios del IDE.

La evidencia está registrada en
`docs/evidence/J11-S8-C06-politicas-reference-data.md`.

## Implementación alcanzada

- servicio y puerto puros para estado efectivo, cambio e historia;
- permiso dedicado en descriptor, menú y rutas **Administrar** consumidoras;
- V2 aditiva con historia append-only y V1 conservada byte por byte;
- repositorio nativo JPA/JTA con `expectedVersion` y aislamiento por empresa;
- auditoría técnica `CHANGED`, `UNCHANGED` y `REJECTED`;
- detalle neutral con estado, versión, historial y acciones habilitar/inhabilitar;
- pruebas focales de los 14 módulos afectados verdes;
- PostgreSQL/Testcontainers 5/5 con Flyway V1–V4 e idempotencia;
- `clean verify` integral: 26 módulos, 498 pruebas y 28 ArchUnit verdes;
- Compose real con migrator en código 0, health 2/2 y OIDC 4/4 verdes;
- composición JTA aislada 12/12, sin omisiones y retirada después de validarse;
- Playwright 1/1 con 30 capturas en 375, 720 y 1280 px, incluida autorización
  negativa, historia append-only, restauración de XDR y ausencia de overflow.

CA-01 a CA-08 quedaron satisfechos. La política ficticia de `XDR` terminó
habilitada en versión 22 y el stack principal conservó sus volúmenes. No se
promovieron imágenes ni se desplegó a producción.
