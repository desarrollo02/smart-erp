# Épica — Migración de legados con Oracle Forms & Reports

- Estado: Planificada; LM-D01 a LM-D12 pendientes
- Plugin: `legacy_migration`
- Clasificación: técnica, opcional y de onboarding/cutover
- Prioridad: obligatoria antes de comercializar una oferta de reemplazo Oracle
- Decisión: [ADR-0040](../adr/0040-modulo-tecnico-migracion-legados-oracle-forms-reports.md)
- Perfil inicial: [Oracle Forms & Reports](../knowledge-base/legacy-migration/oracle-forms-reports-source-profile.md)

## Objetivo

Permitir que una implantación descubra un sistema legado, prepare transformaciones,
ensaye cargas, importe datos por contratos públicos, resuelva errores, concilie
resultados y ejecute un corte recuperable. Oracle Forms, Oracle Reports, PL/SQL y
Oracle Database constituyen el primer origen soportado.

## Valor de negocio

- reduce la dependencia de scripts artesanales por cliente;
- hace estimable el alcance antes de prometer una migración;
- conserva qué dato llegó, desde dónde, con qué regla y en qué ejecución;
- permite ensayar y repetir sin duplicar efectos;
- separa errores de datos, de mapeo, de configuración y de producto;
- demuestra cobertura de pantallas y reportes del legado;
- produce conciliaciones y una aceptación verificable antes del corte;
- conserva el origen intacto y un rollback posible.

## Actores

| Actor | Responsabilidad |
|---|---|
| líder de migración | define alcance, fuente, calendario, responsables y criterio de cierre |
| analista funcional | clasifica Forms/Reports, reglas, pantallas y equivalencias |
| especialista de datos | perfila, mapea, transforma y resuelve calidad |
| DBA/origen | entrega exportaciones o acceso Oracle read-only controlado |
| dueño de módulo | aprueba reglas e import contract del plugin destino |
| seguridad/privacidad | aprueba datos, acceso, enmascarado, retención y evidencias |
| operador de migración | ejecuta dry-runs, ensayos, reintentos y corte autorizado |
| reconciliador | compara conteos, importes, saldos, relaciones y muestras |
| responsable de negocio | firma aceptación o rechaza el corte |
| soporte/auditoría | consulta manifiestos, resultados, decisiones y trazabilidad |

## Capacidades

### Proyecto y gobierno

- empresa/instalación objetivo, origen, alcance, fechas y responsables;
- estados, RACI, riesgos, dependencias y decisiones;
- clasificación de datos y política de retención;
- criterios de aceptación y rollback por dominio.

### Descubrimiento de aplicación

- importar manifiestos y XML de Forms/Reports;
- inventariar módulos, menús, bibliotecas, triggers, PL/SQL y reportes;
- construir dependencias y matriz pantalla/reporte/dato;
- marcar cobertura, duplicados, obsoletos y objetos sin equivalencia;
- convertir hallazgos en casos de uso y pruebas de caracterización.

### Descubrimiento de datos

- esquema, tablas, vistas, constraints, secuencias, triggers y paquetes;
- conteos, nulos, duplicados, huérfanos, rangos y calidad;
- claves naturales, relaciones y orden de carga;
- charset, NLS, zona horaria, escala y reglas de redondeo;
- datos sensibles, LOB/archivos e integraciones externas.

### Mapeo y transformación

- versión inmutable por conjunto de reglas;
- origen, destino público, condición, transformación y valor por defecto;
- lookup por claves públicas, normalización y catálogos controlados;
- decisiones explícitas de merge, rechazo, deduplicación e inactivos;
- previsualización y explicación de cada resultado.

### Ejecución

- dry-run sin mutación;
- ensayo completo, lotes y checkpoints;
- reanudación e idempotencia;
- límites, backpressure y estado visible;
- resultado por lote/registro sin exponer datos sensibles;
- cuarentena, corrección mediante nuevo mapeo y reintento controlado.

### Conciliación y corte

- conteos de origen, landing, válidos, importados, rechazados y omitidos;
- sumas, saldos, relaciones, huérfanos y muestras aprobadas;
- umbrales por dominio y diferencia explicada;
- congelamiento, extracción delta, respaldo destino y ventana;
- aprobación de go/no-go, rollback y cierre;
- evidencia exportable con manifiestos y checksums.

## Fuera de alcance

- escribir, reparar o actualizar la base Oracle origen;
- convertir código Forms/PLSQL automáticamente a Java/JSF;
- ejecutar binarios o scripts aportados sin allowlist/revisión;
- incorporar Oracle Client, herramientas o drivers propietarios al WAR;
- escribir directamente esquemas privados de plugins destino;
- crear entidades finales genéricas, EAV o JSON operativo;
- reemplazar reglas del dominio por transformaciones de migración;
- mantener sincronización bidireccional o dual-write por defecto;
- borrar en masa datos destino para simular rollback;
- funcionar como generador universal de reportes productivos.

## Pantallas planificadas

| Orden | Pantalla | Contenido |
|---:|---|---|
| 1 | Proyectos | alcance, empresa, origen, responsables, estado, riesgos y próximos gates |
| 2 | Fuentes | snapshots, paquetes, manifiestos, checksums, herramientas y clasificación |
| 3 | Forms y Reports | artefactos, dependencias, triggers, consultas, parámetros, cobertura y dueño |
| 4 | Perfil de datos | entidades, volúmenes, claves, calidad, sensibles y bloqueos |
| 5 | Mapeos | versiones, reglas, lookups, defaults, ejemplos y aprobación |
| 6 | Ensayos | plan, dry-run, lotes, checkpoints, progreso y rendimiento |
| 7 | Cuarentena | errores por categoría, causa, tratamiento y reintento |
| 8 | Conciliación | métricas origen/destino, diferencias, muestras y umbrales |
| 9 | Corte | checklist, respaldo, freeze, delta, go/no-go y rollback |
| 10 | Evidencia | aprobaciones, auditoría, manifiestos, reportes y retención |

Todas usan Jakarta Faces 4.1, Material Design 3 y diseños 375/720/1280 px. Las
listas se buscan y paginan en servidor. Datos sensibles se enmascaran por defecto.

## Permisos candidatos

| Permiso | Capacidad |
|---|---|
| `legacy_migration:read` | consultar proyectos y resultados autorizados |
| `legacy_migration:manage_projects` | crear alcance, responsables y políticas |
| `legacy_migration:inventory_sources` | registrar snapshots, artefactos y perfiles |
| `legacy_migration:manage_mappings` | proponer/versionar mapeos sin ejecutarlos |
| `legacy_migration:approve_mappings` | aprobar una versión para ensayo/corte |
| `legacy_migration:run_rehearsal` | ejecutar dry-run o ensayo |
| `legacy_migration:run_cutover` | ejecutar sólo el plan de corte aprobado |
| `legacy_migration:reconcile` | registrar métricas y decisiones de conciliación |
| `legacy_migration:approve_cutover` | decisión go/no-go y aceptación final |
| `legacy_migration:manage_retention` | aplicar retención/purga controlada con doble autorización |
| `legacy_migration:export_evidence` | generar paquetes de evidencia sanitizados |

Mapear y aprobar, ejecutar corte y aprobar corte no deben concentrarse en una sola
identidad en la primera versión.

## Modelo conceptual

| Agregado | Responsabilidad |
|---|---|
| `MigrationProject` | alcance, empresa, estado, responsables, políticas y gates |
| `SourceSnapshot` | punto consistente, origen, herramienta, fecha y manifiesto |
| `SourceArtifact` | archivo/objeto, tipo, checksum, dependencia y clasificación |
| `MappingSet` | versión inmutable y estado de aprobación |
| `MigrationPlan` | destinos, orden, lotes, límites y criterio de reanudación |
| `MigrationRun` | ejecución, modo, snapshot, mapping, actor, estado y checkpoint |
| `RecordResult` | clave origen, destino público, resultado, checksum y problema |
| `MigrationIssue` | categoría, severidad, causa, tratamiento y responsable |
| `Reconciliation` | métrica, origen, destino, diferencia, umbral y decisión |
| `CutoverPlan` | freeze, delta, respaldo, go/no-go, rollback y ventana |
| `MigrationSignoff` | alcance aceptado/rechazado, actor, fecha y evidencia |

## Estados candidatos

Proyecto:

`DRAFT → INVENTORIED → MAPPED → REHEARSED → READY_FOR_CUTOVER → CUTOVER_RUNNING → RECONCILING → ACCEPTED → CLOSED`

Salidas alternativas: `BLOCKED`, `CANCELLED` o `ROLLED_BACK`. Un proyecto cerrado
no se reabre; un nuevo delta/corrección crea ejecución o proyecto relacionado.

Ejecución:

`PLANNED → RUNNING → PAUSED/FAILED/COMPLETED → RECONCILED/REJECTED`.

## Historias

| Orden | Historia | Resultado |
|---:|---|---|
| 1 | LM-00 | decisiones LM-D01 a LM-D12, RACI, threat model, licencias, privacidad, aceptación y rollback |
| 2 | LM-01 | clasificación técnica, API/dominio neutral, descriptor y composición opcional |
| 3 | LM-02 | esquema privado, migraciones, storage, cifrado, auditoría y retención |
| 4 | LM-03 | runner e inventario reproducible de Forms/Reports/PLSQL |
| 5 | LM-04 | paquete portable, conector Oracle read-only, perfilado y checkpoints |
| 6 | LM-05 | mapeo versionado, normalización, dry-run, cuarentena y reanudación |
| 7 | LM-06 | adaptadores públicos para módulos incluidos en la primera oferta |
| 8 | LM-07 | conciliación, signoff, cutover, delta y rollback |
| 9 | LM-08 | pantallas, manual por módulo, runbooks y demo responsive |
| 10 | LM-09 | seguridad, rendimiento, recuperación, matriz acumulada y gate comercializable |

LM-01 a LM-08 podrán usar `Implementada pendiente de pruebas` mientras siga
vigente la decisión de producto del 2026-08-11. LM-09 debe ejecutar la matriz
completa antes de ofrecer migración Oracle como capacidad comercializable.

## Adaptadores de la primera candidata

El alcance final se confirma en LM-D12. La recomendación inicial para la primera
oferta útil es:

1. empresas/configuración mínima del kernel mediante procedimiento gobernado;
2. socios comerciales;
3. catálogo comercial;
4. depósitos, ubicaciones y saldos iniciales de inventario;
5. solicitudes/órdenes abiertas de Compras si `purchasing` ya está incluido;
6. usuarios y permisos sólo mediante el procedimiento OIDC aprobado, nunca
   migrando contraseñas Oracle.

No se migran transacciones históricas completas por defecto. Cada tipo documental
debe decidir entre migración operativa, snapshot de consulta, archivo externo o
conservación del legado read-only.

## Criterios de aceptación

- **LM-CE01:** todos los artefactos origen esperados están inventariados o
  justificados con checksum y dependencia.
- **LM-CE02:** el origen se usa exclusivamente en solo lectura y queda intacto.
- **LM-CE03:** no existen credenciales ni datos sensibles innecesarios en Git,
  logs, manifiestos o evidencias.
- **LM-CE04:** herramientas/drivers Oracle declaran versión, licencia, origen y
  checksum y no se empaquetan silenciosamente.
- **LM-CE05:** toda transformación pertenece a una versión inmutable aprobada.
- **LM-CE06:** un dry-run no muta datos operativos.
- **LM-CE07:** reintentar/reanudar la misma ejecución no duplica efectos.
- **LM-CE08:** todo destino se escribe mediante contrato público tipado y conserva
  procedencia/idempotencia.
- **LM-CE09:** registros inválidos quedan en cuarentena explicable; no se omiten
  silenciosamente.
- **LM-CE10:** conciliación compara conteos, totales, relaciones y muestras por
  dominio con umbral explícito.
- **LM-CE11:** ninguna diferencia crítica queda sin resolver o aceptación firmada.
- **LM-CE12:** respaldo y rollback se prueban antes del corte.
- **LM-CE13:** permisos separan mapeo, ejecución y aceptación.
- **LM-CE14:** desactivar/retirar el plugin detiene fuentes y menús sin borrar
  evidencias o datos operativos importados.
- **LM-CE15:** pantallas funcionan en 375/720/1280 px con teclado, foco y
  paginación sin overflow horizontal normal.
- **LM-CE16:** Forms/Reports produce matriz de cobertura; no se presenta análisis
  como conversión automática.
- **LM-CE17:** la demo ejecuta un ensayo repetible y un corte ficticio con error,
  cuarentena, reintento, conciliación y rollback.
- **LM-CE18:** el gate comercializable incluye volumen representativo, seguridad
  negativa, interrupción/reanudación y reconstrucción desde manifiestos.

## Matriz de pruebas acumulada

- dominio: estados, versiones, mapeos, idempotencia y signoff;
- parsers: XML Forms/Reports, PL/SQL y manifiestos maliciosos/incompletos;
- seguridad: zip-slip, XXE, path traversal, payload grande, secretos, permisos y
  empresa ajena;
- PostgreSQL: aislamiento, constraints, concurrencia, checkpoints y retención;
- contratos: adaptadores tipados y ausencia de SQL/JPA cruzado;
- runner: corte de red, fuente lenta, checkpoint, reanudación y checksum inválido;
- volumen: tamaño representativo, memoria acotada, paginación y backpressure;
- cutover: freeze, delta, respaldo, fallo intermedio, rollback y repetición;
- UI: 375/720/1280, vacío/error, teclado y datos enmascarados;
- composición: plugin/runner presentes y ausentes sin bloquear el ERP;
- documentación: manual, runbook, evidencia y paquete reproducible.

## Dependencias para comenzar

LM-00 puede refinarse documentalmente sin interrumpir Sprint 9. La implementación
espera:

- confirmación de LM-D01 a LM-D12;
- `PluginKind` técnico resuelto;
- al menos un plugin destino con contrato público de importación;
- entorno Oracle o paquete ficticio legalmente utilizable;
- política de storage, secretos, retención y privacidad;
- definición de qué datos debe conservar la primera oferta comercializable.
