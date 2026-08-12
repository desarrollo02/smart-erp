# Evidencia - J11-S9-01 caracterización de `purchasing`

- Fecha: 2026-08-11
- Resultado: revisión estática/documental completada; pruebas automatizadas no ejecutadas
- Fuente legado: `C:\cosme\mega\miaterra\fuente\tag`
- Commit observado: `7fa64a7313940527a1b16856fbbccbad38f7c916`
- Legado modificado: no
- Base de datos consultada: no

## Evidencia de inspección

Se inspeccionaron en solo lectura:

- menús `menuTesoreria.xhtml` y `menuStock.xhtml`;
- pantallas `TswSolicitudCompra.xhtml`, `StwCOOrdenCompra.xhtml`,
  `TswCOComprasV2.xhtml` y `StwCOComprasPendientes.xhtml`;
- formularios/listados relacionados con solicitud y orden;
- controladores `TswSolicitudCompraControlador`,
  `StwCOOrdenCompraControlador`, `TswComprasControlador` y
  `StwCOComprasPendientesControlador`;
- entidades de solicitud, orden, detalle, historial, proceso y vínculo con
  comprobante;
- enums `EstadoSolicitud` y `TipoCompra`;
- documentación versionada de Compras V2 y recepción de stock pendiente;
- contratos públicos vigentes de socios, catálogo, referencia e inventario.

## Entregables revisados

- [caracterización](../knowledge-base/purchasing/legacy-characterization.md);
- [épica de Compras](../backlog/epica-compras.md);
- [Sprint 9](../sprints/sprint-09/README.md);
- [gobierno J11-S9-00](../sprints/sprint-09/J11-S9-00-gobierno-planificacion.md);
- [historia J11-S9-01](../sprints/sprint-09/J11-S9-01-caracterizacion-purchasing.md).

## Pruebas pendientes

Por decisión de producto del 2026-08-11 no se ejecutaron pruebas automatizadas.
Maven, ArchUnit, PostgreSQL/Testcontainers, JTA/OIDC, Docker/Compose, health,
seguridad y Playwright quedan acumulados para la candidata comercializable. No se
declara ningún gate verde por esta evidencia.

## Decisiones posteriores registradas

- PU-D01 a PU-D10 fueron aceptadas sin cambios por producto el 2026-08-11;
- se autorizó y creó la rama local `sprint/09-purchasing`, sin commit ni
  publicación remota;
- J11-S9-02 quedó habilitada para API y dominio neutral;
- continúa siendo necesaria autorización separada si se requiere inspeccionar una
  base legado de solo lectura.
