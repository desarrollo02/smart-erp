# Sprint 9 - Compras `purchasing`

- Estado: J11-S9-06 implementada y validada automáticamente; validación independiente pendiente; cierre J11-S9-07 habilitado
- Fecha de inicio documental: 2026-08-11
- Dependencia funcional: `business_partners`, `commercial_catalog`, `reference_data` e `inventory`
- Dependencia de gobierno: Sprint 8 permanece abierto y G7 independiente continúa pendiente
- Continuidad excepcional: sólo la validación independiente de otra persona se difiere hasta una candidata comercializable
- ADR rector: [ADR-0011](../../adr/0011-roadmap-dependencias-plugins-productivos.md)
- Épica: [Compras](../../backlog/epica-compras.md)

## Objetivo

Construir `purchasing` como cuarto plugin productivo. Debe separar solicitud,
orden, recepción y devolución de la factura del proveedor, el pago, la retención,
la contabilidad y la valoración; integrarse con los plugins anteriores sólo por
contratos públicos e identificadores.

## Autorización y límites de la continuidad

El responsable de producto aclaró el 2026-08-11 que la continuidad hasta una
versión comercializable difiere únicamente las pruebas de aceptación o la
validación independiente a cargo de otra persona. Las pruebas automatizadas
ejecutables por el agente continúan siendo obligatorias. Por ello:

- se autoriza caracterizar, decidir e implementar `purchasing` en el orden del
  Sprint;
- cada historia de código ejecuta su prueba mínima, pruebas de módulo y gates
  automatizados proporcionales al riesgo;
- una prueba automatizada que falle bloquea el avance y debe corregirse;
- Sprint 8 y Sprint 9 permanecen abiertos mientras sus gates aplicables estén
  pendientes;
- no se promueven imágenes, no se etiqueta cierre, no se despliega a producción y
  no se entrega un instalador nuevo;
- J11-S9-07 repite la matriz acumulada sobre el baseline de cierre, completa la
  demo oficial, la fotografía de plugins, el PDF obligatorio y la validación
  independiente.

La excepción cambia el calendario, no los criterios de aceptación ni la
Definition of Done.

## Orden del incremento

| Orden | Historia | Estado | Resultado esperado |
|---:|---|---|---|
| 1 | [J11-S9-00](J11-S9-00-gobierno-planificacion.md) | Completada documentalmente | alcance, gobierno y deuda de pruebas explícitos |
| 2 | [J11-S9-01](J11-S9-01-caracterizacion-purchasing.md) | Completada; decisiones aceptadas | comportamiento legado, frontera y PU-D01 a PU-D10 |
| 3 | [J11-S9-02](J11-S9-02-dominio-contratos-purchasing.md) | Implementada y validada automáticamente; validación independiente pendiente | API Java pura, dominio y contratos públicos |
| 4 | [J11-S9-03](J11-S9-03-persistencia-purchasing.md) | Implementada y validada automáticamente; validación independiente pendiente | esquema privado, migraciones y repositorios |
| 5 | [J11-S9-04](J11-S9-04-aplicacion-purchasing.md) | Implementada y validada automáticamente; validación independiente pendiente | aplicación, permisos, auditoría, JTA e idempotencia |
| 6 | [J11-S9-05](J11-S9-05-interfaz-purchasing.md) | Implementada y validada automáticamente; validación independiente pendiente | pantallas neutrales Material Design 3; Playwright al componerlas |
| 7 | [J11-S9-06](J11-S9-06-integracion-composicion-purchasing.md) | Implementada y validada automáticamente; validación independiente pendiente | composición, integraciones y demo candidata |
| 8 | J11-S9-07 | Pendiente | pruebas acumuladas, demo oficial, documentación, PDF y cierre |
| 9 | J11-S9-08 | Pendiente | decisión de instalador y gate Windows sólo con respuesta `SÍ` |

## Recorridos funcionales objetivo

1. crear, editar, enviar y consultar una solicitud;
2. aprobar o rechazar con separación solicitante/aprobador;
3. crear una orden directa o desde cantidades aprobadas;
4. emitir y consultar una orden histórica;
5. registrar y confirmar recepción parcial/final;
6. cerrar un faltante explícitamente;
7. registrar y confirmar devolución a proveedor;
8. consultar cumplimiento y trazabilidad completa.

## Pantallas candidatas

| Pantalla | Tareas principales |
|---|---|
| Solicitudes | bandeja, filtros, nuevo borrador, envío, aprobación/rechazo y clonación |
| Órdenes | bandeja, creación directa/desde solicitudes, emisión, cancelación y cierre |
| Recepciones | órdenes abiertas, cantidades pendientes, destino y confirmación |
| Devoluciones | recepciones disponibles, causa, cantidades y confirmación |
| Seguimiento | cumplimiento por orden/línea, historia, referencias y errores recuperables |

Cada historia visual deberá documentar términos, datos, acciones, permisos,
errores y diagramas de tablas en el manual de usuario del módulo, y deberá diseñar
375, 720 y 1280 px. Playwright se ejecuta en la primera historia que componga la
pantalla en una aplicación navegable; no se sustituye por validación humana.

## Límites

- no copiar `StwOrdenCompra`, `TswSolicitudCompra` ni sus controladores;
- no introducir `javax.*`;
- no agregar comprobante fiscal, deuda, pago, retención, asiento o valoración;
- no implementar expedientes de importación comercial, embarques ni aduana en
  V1; la migración técnica de documentos abiertos usa el contrato público
  controlado definido para `legacy_migration`;
- no usar entidades, DTO internos, tablas o claves foráneas de otros plugins;
- no emitir eventos sin consumidor real;
- no diseñar migración de datos hasta verificar la base legado en solo lectura;
- no llamar “verde”, “cerrado” o “comercializable” a un corte sin pruebas.

## Gobierno Git resuelto

El responsable de producto autorizó el 2026-08-11 crear la rama local
`sprint/09-purchasing`. La rama se creó desde `main` conservando el workspace
documental existente. No se creó commit, no se publicó la rama y no se representó
Sprint 8 como cerrado. Las historias de código posteriores se materializarán desde
esta rama de integración sin reescribir ni perder cambios existentes.

## Estado de pruebas automatizadas

El corte final de código materializado ejecutó el 2026-08-12:

- `mvn verify` con `with-purchasing-demo`: 28 módulos verdes y 535 pruebas
  unitarias acumuladas;
- ArchUnit y composición física: 34 pruebas verdes;
- `purchasing` con PostgreSQL 18.4/Testcontainers: 7 integraciones verdes;
- migrador final ejecutado dos veces sin migraciones pendientes ni cambios de
  checksum;
- runtime: 2 pruebas de health/readiness y 4 de OIDC verdes;
- Playwright: un recorrido integral verde y 18 capturas en 375/720/1280 px;
- total materializado: 549 pruebas, cero fallos, errores u omitidas.

Las pruebas bloquearon el avance y permitieron corregir transacciones de consulta,
resolución exacta de referencias, búsquedas sin sensibilidad a mayúsculas y la
preservación/validación de selectores dependientes. La revisión final agregó una
entrada raíz protegida, mejoró contraste y eliminó ambigüedad en los controles
Playwright. La validación funcional
independiente continúa pendiente.

## Compatibilidad con migración de legados

[ADR-0040](../../adr/0040-modulo-tecnico-migracion-legados-oracle-forms-reports.md)
agrega al roadmap el técnico opcional `legacy_migration`. No cambia el orden de
Sprint 9 ni crea una dependencia de `purchasing` hacia el migrador.

J11-S9-02 definió el comando público tipado para importar solicitudes u órdenes
abiertas conservando número, fechas, snapshots, estado, procedencia e
idempotencia. J11-S9-04 implementó el ledger de importación V2 y la API CDI
autorizada. El adaptador Oracle vive del lado de migración y
consume la API; nunca accede a `plg_purchasing`. Migrar el historial completo no
se asume por defecto y requiere alcance, volumen, retención y conciliación
explícitos.

Una candidata que publicite reemplazo de Oracle Forms & Reports debe integrar el
gate LM-09 además de J11-S9-07.

## Compatibilidad futura con BPM

[ADR-0045](../../adr/0045-plugin-gestion-procesos-negocio-bpm.md) agrega al plan
`business_process_management` como funcional transversal y opcional. No cambió el
alcance de J11-S9-06 ni adelanta BPM; el siguiente gate sigue siendo J11-S9-07.

La aprobación de solicitudes de Compras es el primer piloto propuesto para una
iteración BPM propia. El piloto consumirá un evento público de solicitud enviada y
ejecutará un comando público de aprobación/rechazo; `purchasing` revalidará
empresa, permiso, actor distinto del solicitante, versión e idempotencia. Compras
no dependerá del motor y debe conservar su recorrido normal con BPM ausente o
inactivo. Antes del piloto se aceptarán BPM-D01 a BPM-D12 y se materializará
ADR-0013 con productor y consumidor reales.

## Compatibilidad futura con mantenimiento y taller

[ADR-0046](../../adr/0046-familia-mantenimiento-flota-taller-automotriz.md)
agrega al plan la familia vertical Flota con F1 `fleet_maintenance` y F2
`automotive_workshop`. Producto aceptó FM-D01 a FM-D12 y AW-D01 a AW-D10 sin
cambios el 2026-08-12. La decisión no cambió alcance, código ni orden de Sprint 9;
tras completar J11-S9-06, el siguiente gate sigue siendo J11-S9-07.

F1 sólo puede comenzar después de estabilizar el `VehicleId` público de
`logistics-api`; F2 requiere además F1, Ventas y Documentos Comerciales. Compras
será un proveedor de contratos públicos para repuestos y servicios tercerizados,
pero no dependerá de la familia ni incorporará lógica de mantenimiento.

## Próximo gate

J11-S9-06 compuso Compras en WAR y migrador, dejó una aplicación navegable en
`http://localhost:18080/logixone/` y completó Maven, ArchUnit, PostgreSQL,
Docker/Compose, migraciones, health, OIDC y Playwright. J11-S9-07 queda habilitada
para congelar el baseline de cierre, repetir la matriz acumulada, completar la
demo oficial, actualizar la fotografía de plugins y regenerar/revisar el PDF
obligatorio. La aceptación por otra persona permanece pendiente y Sprint 9 no
está cerrado.

## Iteración transversal posterior aprobada

El responsable de producto decidió el 2026-08-13 que, después de congelar el
baseline técnico mediante J11-S9-07 y registrar la decisión de instalador
J11-S9-08, se ejecute
[Sprint 10](../sprint-10/README.md) para implementar floorplans operativos y
transaccionales antes de comenzar `sales`.

La decisión responde a la revisión de las pantallas reales de Inventario y
Compras: los maestros conservarán el floorplan actual, mientras movimientos,
órdenes, recepción/devolución y aprobaciones serán pilotos de captura
condicional, editor de líneas, resumen contextual y bandeja de trabajo. El cambio
no reabre J11-S9-05/J11-S9-06 ni modifica el siguiente trabajo inmediato:
J11-S9-07 sigue siendo el gate habilitado.
