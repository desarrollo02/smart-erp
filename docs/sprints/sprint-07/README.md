# Sprint 7 - Catálogo comercial `commercial_catalog`

- Estado: G0-G6 verdes y demo oficial disponible; G7 independiente pendiente
- Fecha de planificación: 2026-07-30
- Dependencia técnica: G0-G6 de Sprint 6 verdes
- Pendiente transversal: validación independiente G7 de la guía candidata
- ADR rector: [ADR-0011](../../adr/0011-roadmap-dependencias-plugins-productivos.md)

## Objetivo

Construir `commercial_catalog` como segunda fundación productiva: productos,
servicios, clasificaciones, unidades, datos tributarios comerciales y precios
reutilizables. Debe publicar referencias estables para inventario, compras, ventas
y documentos sin contener stock, pedidos, facturas ni reglas SIFEN.

## Orden de historias propuesto

| Orden | Historia | Resultado esperado |
|---:|---|---|
| 1 | [J11-S7-00](J11-S7-00-gobierno-planificacion.md) | alcance, decisiones pendientes, riesgos y gates |
| 2 | [J11-S7-01](J11-S7-01-caracterizacion-commercial-catalog.md) | caracterización del legado y lenguaje del catálogo |
| 3 | [J11-S7-02](J11-S7-02-dominio-contratos-commercial-catalog.md) | dominio neutral y contratos públicos versionados |
| 4 | [J11-S7-03](J11-S7-03-persistencia-commercial-catalog.md) | esquema privado, migraciones y repositorios |
| 5 | [J11-S7-04](J11-S7-04-aplicacion-seguridad-commercial-catalog.md) | comandos, consultas, permisos, autorización y auditoría |
| 6 | [J11-S7-05](J11-S7-05-interfaz-commercial-catalog.md) | directorio, alta y ficha JSF Material Design 3 responsive |
| 7 | [J11-S7-06](J11-S7-06-integracion-composicion-commercial-catalog.md) | composición física, migrador, guía y demo candidata |
| 8 | [J11-S7-07](J11-S7-07-validacion-demo-cierre.md) | validación integral, demo oficial, retrospectiva y PDF |

## Documentación de cierre

- [Estructura de plugins y dependencias](estructura-plugins-y-dependencias.md), con
  gráfico del baseline, alternativa textual, inventario y perfiles físicos.
- [Manual de usuario](../../user-guide/README.md).
- [Manual técnico para desarrolladores](../../developer-guide/README.md).
- [Guía para Visual Studio Code](../../runbooks/levantar-logixone-visual-studio-code.md).

## Límites iniciales

- no crear existencias, depósitos, lotes, reservas o movimientos de inventario;
- no modelar pedidos de compra/venta ni documentos comerciales;
- no copiar tablas o código del legado;
- no incorporar XML/XSD ni reglas de transmisión SIFEN;
- no permitir que precios reescriban documentos históricos;
- no compartir entidades JPA con `business_partners` u otros plugins;
- no diseñar persistencia antes de aceptar conceptos, cardinalidades e invariantes;
- no agregar outbox sin un productor y consumidor reales.

## Demo visual objetivo

La demo de cierre debe permitir, con datos ficticios:

1. listar y buscar conceptos comerciales;
2. registrar un producto y un servicio mediante recorridos claros;
3. asignar clasificación, unidad y datos tributarios autorizados;
4. administrar al menos una lista o regla de precio aprobada en el alcance;
5. inactivar y reactivar sin borrar historia;
6. desactivar el plugin para una empresa, demostrar denegación y restaurarlo;
7. repetir los recorridos en 375, 720 y 1280 px sin overflow.

La demo no simulará stock, venta, factura ni cumplimiento fiscal.

## Condición de continuidad

La planificación puede avanzar mientras G7 independiente siga pendiente, conforme
a la continuidad ya autorizada para fundaciones de plugins. No se promueve ninguna
imagen, no se publica la guía `1.0` y no se despliega a producción hasta resolver
ese gate.

## Estado del siguiente paso

`J11-S7-00` a `J11-S7-07` están técnicamente completas. El responsable de producto confirmó
CC-D01 a CC-D10 sin cambios el 2026-07-30 y ratificó que `commercial_catalog` será
otro módulo/plugin funcional. Los módulos separados `commercial-catalog-api` y
`commercial-catalog`, el contrato público `1.0.0` y el dominio neutral quedaron
verdes. J11-S7-03 agregó V1 con veinte tablas, JPA privada, repositorios, secuencia
atómica y gates PostgreSQL/ArchUnit verdes. J11-S7-04 agregó cuatro permisos,
comandos/consultas por empresa, definiciones, contratos públicos, auditoría y
límites CDI/JTA validados. `J11-S7-05` implementó dos menús, los contratos y
handlers de artículos/listas de precios y generalizó el floorplan Jakarta Faces.
J11-S7-06 agregó el perfil físico con ambos plugins, el par de imágenes,
migraciones/fixture idempotentes, activación y permisos mediante administración y
un Playwright verde en los siete anchos. J11-S7-07 volvió a validar reactor base y
completo, arquitectura, PostgreSQL, imágenes, Compose, salud, OIDC, persistencia y
la demo acumulada con 47 capturas. La nueva demo navegable y el PDF de Sprint 7
están disponibles. G7 independiente continúa pendiente, por lo que no hay cierre
formal, promoción ni producción. El siguiente trabajo planificado es
[Sprint 8 - `inventory`](../sprint-08/README.md).
