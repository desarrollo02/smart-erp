# Backlog

Contiene épicas, historias de usuario, criterios de aceptación, dependencias y prioridades del producto.

Una historia debe estar suficientemente definida y ser comprobable antes de entrar en un Sprint.

## Épicas

- [Kernel multiempresa y activación de plugins](epica-kernel-multiempresa-activacion-plugins.md)
- [Personalización obligatoria por empresa](epica-personalizacion-pantallas-por-empresa.md)
- [Identidad, autorización y primera demo visual](epica-identidad-autorizacion-demo-visual.md)
- [Administración operativa segura del kernel](epica-administracion-operativa-kernel.md)
- [Documentos comerciales canónicos e integración SIFEN](epica-documentos-comerciales-y-sifen.md)
- [Facturación masiva, recuperable e idempotente](epica-facturacion-masiva.md)
- [Planes, prorrateo y consumo medido](epica-facturacion-recurrente.md)
- [Telemetría vehicular y seguimiento GPS](epica-telemetria-vehicular.md)
- [Roadmap inicial de plugins productivos](epica-roadmap-plugins-productivos.md)
- [Compras: solicitudes, órdenes, recepciones y devoluciones](epica-compras.md)
- [Terminal de punto de venta](epica-terminal-punto-venta.md)
- [Estaciones de servicio de combustible](epica-estaciones-servicio-combustible.md)
- [Recursos humanos, nómina y cumplimiento paraguayo](epica-recursos-humanos-nomina-paraguay.md)
- [Definiciones maestras del catálogo](epica-definiciones-maestras-catalogo.md)
- [Gobierno de selectores y datos administrables](epica-gobierno-selectores-datos-administrables.md)
- [Instalador Windows reproducible por Sprint](epica-instalador-windows-reproducible.md)
- [Soporte a clientes del ERP](epica-soporte-clientes-erp.md)
- [Gestión de lanzamientos, mejoras y correcciones](epica-gestion-lanzamientos-erp.md)
- [Conector seguro de soporte](epica-conector-soporte-seguro.md)
- [Cooperativa de ahorro y crédito de Paraguay](epica-cooperativa-ahorro-credito-paraguay.md)
- [Migración de legados con Oracle Forms & Reports](epica-migracion-legados-oracle-forms-reports.md)
- [Gestión de procesos de negocio BPM](epica-gestion-procesos-negocio-bpm.md)
- [Mantenimiento de flota](epica-mantenimiento-flota.md)
- [Taller automotriz comercial](epica-taller-automotriz-comercial.md)
- [Floorplans operativos y transaccionales](epica-floorplans-operativos-transaccionales.md)

## Historias futuras refinadas

- [COOP-00 — Gobierno, alcance y matriz normativa](COOP-00-gobierno-alcance-matriz-normativa.md): lista para recibir datos y decisiones de una cooperativa concreta; no autoriza código.
- [WIN-I09 — Selección de plugins con dependencias resueltas](WIN-I09-seleccion-plugins-dependencias.md): selector físico del instalador con cierre transitivo, bloqueo de composiciones inválidas y par WAR/migrador idéntico; no está implementado en la edición interna actual.

## Entregables transversales

- [Guía de implementación del ERP por empresa](../implementation-guide/README.md): comienza como edición utilizable en `J11-S2-08` y evoluciona con cada baseline que cambie la experiencia del implementador.
- [Instalador Windows reproducible](epica-instalador-windows-reproducible.md): en cada cierre producto decide `SÍ` o `NO`; si responde `SÍ`, se regenera después de congelar el baseline.

## Incremento activo

Por decisión de producto del 2026-08-11 se abrió documentalmente
[Sprint 9](../sprints/sprint-09/README.md) para `purchasing`, cuarto plugin del
roadmap. [J11-S9-01](../sprints/sprint-09/J11-S9-01-caracterizacion-purchasing.md)
caracterizó el legado actualizado y presentó PU-D01 a PU-D10. Producto las
aceptó sin cambios y autorizó la rama local
`sprint/09-purchasing` el 2026-08-11. J11-S9-02 implementó API y dominio;
J11-S9-03 implementó V1 privada, JPA y repositorios; J11-S9-04 implementó
aplicación, permisos, V2, CDI/JTA e integración con Inventario; J11-S9-05 agregó
cinco pantallas, directorios, selectores y el manual 07. J11-S9-06 compuso Compras
en WAR/migrador. J11-S9-07 repitió Maven, ArchUnit, PostgreSQL, migraciones,
Docker/Compose, health, OIDC y Playwright; completó demo, documentación, fotografía
y PDF, y dejó G0–G6 verdes. La aclaración de producto difiere sólo la validación
independiente de otra persona. J11-S9-08 registró `SÍ` y creó el instalador
interno `0.9.0-internal.1`; Sprint 8 y Sprint 9 continúan abiertos y el baseline
no se promueve a producción.

El responsable de producto decidió el 2026-08-13 insertar
[Sprint 10](../sprints/sprint-10/README.md) como incremento transversal después de
J11-S9-07/J11-S9-08 y antes de `sales`. J11-S9-07 congeló el baseline técnico y
J11-S9-08 creó la edición interna solicitada. Sprint 10 evolucionará los contratos
neutrales y el shell con `WORKLIST`, `TRANSACTION_EDITOR` y `GUIDED_OPERATION`, y
los validará sobre Inventario y Compras reales. La decisión no altera ADR-0011:
`purchasing` conserva el orden ERP 4 y `sales` el orden ERP 5. J11-S10-00
permanece planificada y no se inició código en esta historia.

La candidata visual de [Sprint 3](../sprints/sprint-03/README.md) está disponible y
sus gates técnicos G2-G6 quedaron verdes; G7 independiente continúa pendiente. El
responsable de producto autorizó [Sprint 4](../sprints/sprint-04/README.md) para
agregar la administración operativa segura del kernel.

Por decisión de producto del 2026-07-28, las historias de código de Sprint 4 podrán
quedar `Implementada pendiente de pruebas`; solamente los gates automatizados
pueden permanecer pendientes al finalizar cada historia. El cierre, la promoción y
la declaración de kernel operativo completo exigen ejecutar la matriz acumulada.

`J11-S4-01` a `J11-S4-07` quedaron completadas y los gates técnicos, la demo y el
PDF de `J11-S4-08` están verdes. Sprint 4 permanece abierto únicamente hasta que
una persona independiente complete la ficha de validación.

Por decisión de producto del 2026-07-29 se autorizó iniciar
[Sprint 5](../sprints/sprint-05/README.md) con fundaciones transversales mientras
esa validación humana continúa pendiente. La excepción no cambia resultados
técnicos verdes, no cierra Sprint 4 y no autoriza promoción o producción. Los
cambios nuevos vuelven al flujo incremental normal de pruebas.

El orden aprobado está definido por
[ADR-0011](../adr/0011-roadmap-dependencias-plugins-productivos.md) y su ampliación
[ADR-0027](../adr/0027-terminal-punto-venta-y-ampliacion-roadmap.md) y
[ADR-0030](../adr/0030-familia-recursos-humanos-nomina-paraguay.md) y
[ADR-0032](../adr/0032-plugin-estaciones-servicio-combustible.md) y
[ADR-0033](../adr/0033-dominio-facturacion-recurrente.md) y
[ADR-0034](../adr/0034-plugin-telemetria-vehicular.md): diecinueve plugins ERP
reutilizables, comenzando por `business_partners`, incorporando
`vehicle_telemetry` después de `logistics`,
`recurring_billing` después de `commercial_documents`, `fuel_station` después de
`point_of_sale` y terminando con
`human_resources`, `payroll` y `payroll_paraguay`, más una personalización distinta
por empresa y compuesta siempre al final. Antes del primero se completan los
habilitadores de composición, migraciones y plantilla, más el contrato rector de
eventos/outbox de Sprint 5. La infraestructura asíncrona se implementará sólo con
un productor y consumidor reales.

[ADR-0036](../adr/0036-operaciones-proveedor-soporte-lanzamientos-conector.md)
agrega una familia futura separada: `customer_support` y `release_management` en
la composición central del proveedor, y `support_connector` como plugin técnico
opcional en la instalación del cliente. El catálogo global planificado contiene
veintidós reutilizables, pero la secuencia ERP 1–19 no se renumera y los plugins
centrales no se envían por defecto a clientes. Las tres épicas quedan planificadas
sin autorizar código durante Sprint 8.

[ADR-0037](../adr/0037-familia-cooperativa-ahorro-credito-paraguay.md) agrega una
segunda familia futura separada para cooperativas paraguayas:
`cooperative_membership`, `cooperative_governance`, `aml_compliance`,
`cooperative_savings`, `cooperative_credit` y
`cooperative_regulatory_paraguay`. Reutiliza `business_partners`, `treasury` y
`accounting`, elevó el catálogo global planificado a veintiocho reutilizables y
no renumera la secuencia ERP 1–19. La familia queda planificada en su
[épica propia](epica-cooperativa-ahorro-credito-paraguay.md); no autoriza código,
captación de ahorros ni operación crediticia durante Sprint 8.

Los países y monedas compartidos tienen propietario explícito en
[ADR-0038](../adr/0038-plugin-datos-referencia-normativos.md) y se implementan en
la [épica de datos de referencia normativos](epica-datos-referencia-normativos.md).
`reference_data` es una fundación R0 que eleva el catálogo global a veintinueve
reutilizables sin renumerar ERP 1–19. El reactor conserva el subconjunto histórico
`PY/PYG/USD` y J11-S8-C07 implementa publicaciones `FULL` 248/178, unidad menor
opcional y búsqueda paginada; sus gates runtime y la recongelación permanecen pendientes.

[ADR-0040](../adr/0040-modulo-tecnico-migracion-legados-oracle-forms-reports.md)
agrega al plan `legacy_migration` como técnico opcional para descubrimiento,
ensayos, importación, conciliación y corte. Oracle Forms & Reports es el primer
perfil de origen. No renumera ERP 1–19 y eleva el catálogo global planificado a
treinta reutilizables. Una oferta de reemplazo Oracle debe completar la
[épica de migración](epica-migracion-legados-oracle-forms-reports.md) antes de
declararse comercializable.

[ADR-0045](../adr/0045-plugin-gestion-procesos-negocio-bpm.md) agrega al plan
`business_process_management` como plugin funcional transversal, reutilizable y
opcional por empresa. No recibe un orden ERP ni altera que J11-S9-07 sea el
siguiente incremento. Eleva el catálogo global planificado a treinta y un
reutilizables y propone la aprobación de solicitudes de Compras como primer
piloto, después de resolver BPM-D01 a BPM-D12 en su
[épica propia](epica-gestion-procesos-negocio-bpm.md).

[ADR-0046](../adr/0046-familia-mantenimiento-flota-taller-automotriz.md) agrega
la familia vertical Flota con orden interno F1 `fleet_maintenance` y F2
`automotive_workshop`. Producto aprobó FM-D01 a FM-D12 y AW-D01 a AW-D10 sin
cambios el 2026-08-12. La familia no renumera ERP 1–19, eleva el catálogo global
planificado a treinta y tres reutilizables y mantiene J11-S9-07 como siguiente
incremento. F1 requiere una identidad pública estable de vehículo en Logística;
F2 se construye después de F1, Ventas y Documentos Comerciales. Consulte las
épicas de [mantenimiento de flota](epica-mantenimiento-flota.md) y
[taller automotriz comercial](epica-taller-automotriz-comercial.md).

[COOP-00](COOP-00-gobierno-alcance-matriz-normativa.md) quedó refinada con
COOP-D01–D15, registro de fuentes, mapa de dependencias y gates G0–G5. Continúa
pendiente de estatuto, tipo/nivel, productos, plan contable, matriz LA/FT,
migración y responsables de una cooperativa concreta; no constituye ejecución.

[ADR-0031](../adr/0031-facturacion-masiva-en-documentos-comerciales.md) incorpora
la facturación por lote como capacidad futura de `commercial_documents`, sin crear
un plugin `bulk_billing`. ADR-0033 agrega después el dominio autónomo
`recurring_billing`, sin transferirle la factura. El lote comercial persistente,
idempotente y recuperable se mantiene separado de la corrida de cargos y de los
lotes técnicos de transmisión de `sifen`.

`vehicle_telemetry` queda planificado como orden 7. Posee dispositivos,
asignaciones, observaciones, recorridos, geocercas, alertas y el ciclo auditado del
seguimiento; `logistics` conserva vehículos, conductores, rutas y viajes. La
inmovilización o el apagado remoto no forman parte de su primer alcance.

`recurring_billing` queda planificado como orden 9 y posee planes, suscripciones,
prorrateo, consumo facturable y corridas de cargos; `commercial_documents`
conserva la factura y el lote de emisión. `fuel_station` queda como orden 13. Posee la operación física de
tanques, surtidores, picos, lecturas, turnos y conciliación húmeda; catálogo,
inventario, POS, documentos, SIFEN y tesorería conservan sus propias fuentes de
verdad. Su código no se adelanta al trabajo activo de Sprint 8 ni a los órdenes
4–12.

`J11-S5-01` a `J11-S5-04` dejaron verdes composición, migraciones `plg_*`,
plantilla, gates técnicos y demo responsive. La validación independiente G7 sigue
pendiente, por lo que Sprint 5 no se declara formalmente cerrado ni se promueve su
imagen.

[Sprint 6](../sprints/sprint-06/README.md) implementó y validó técnicamente
`business_partners`. [Sprint 7](../sprints/sprint-07/README.md) implementó
`commercial_catalog`, lo integró con el primer plugin y dejó G0–G6 y la demo
responsive verdes. El cierre formal continúa condicionado al mismo G7
independiente; no se promueven imágenes ni se autoriza producción.

[Sprint 8](../sprints/sprint-08/README.md) inició la caracterización de
`inventory`. [J11-S8-01](../sprints/sprint-08/J11-S8-01-caracterizacion-inventory.md)
separó depósitos, saldos, movimientos, reservas y conteos de catálogo, compras,
ventas, logística, costos y contabilidad. El responsable de producto confirmó
IN-D01 a IN-D10 sin cambios el 2026-07-31. J11-S8-02 creó los módulos físicos,
API `1.0.0`, dependencia requerida del catálogo y dominio neutral. J11-S8-03 creó
ADR-0024, V1 privada, JPA y seis repositorios. J11-S8-04 agregó V2, siete permisos,
casos de uso autorizados y auditados, contratos CDI y transacciones JTA. J11-S8-05
agregó tres menús/pantallas neutrales, directorios empresariales, handlers
autorizados y presentación en el shell; PostgreSQL, reactor y arquitectura quedaron
verdes. J11-S8-06 y J11-S8-07 completaron composición, Playwright, demo y gates
técnicos; J11-S8-08 produjo la primera edición interna del instalador. El hallazgo
visual sobre selectores sin administración reabrió el baseline mediante
[J11-S8-C01](../sprints/sprint-08/J11-S8-C01-administracion-perfiles-tributarios.md).
[J11-S8-C02](../sprints/sprint-08/J11-S8-C02-gobierno-selectores-administrables.md)
ya validó altas y ciclos de definiciones simples y perfiles tributarios, revisión
explícita e historial visible de contenido/vigencia tributaria, además de
altas de familias y tipos de canal. El décimo corte publicó metadatos y gobierno
visual autorizado para los 18 selectores nativos, completando 77/77. El undécimo
corte validó retorno seguro con borrador y refresco para el renderer de plugins.
El duodécimo validó el equivalente para los 11 usos nativos administrables. El
decimotercero agregó ciclo activo/inactivo versionado y auditado a tipos de canal.
El decimocuarto agregó ese ciclo a las familias de variantes, preservando sus
atributos, identidad y referencias existentes. El decimoctavo agregó revisión
estructural, historial append-only y versión inmutable para asignaciones
existentes. El decimonoveno corte valida la asignación visual/versionada de
familias a artículos. El vigésimo administra tipos de identificación y
tipos/propósitos de dirección mediante el mismo maestro V4 de socios. C06/C07
resolvieron las políticas empresariales, publicaciones normativas completas y
búsqueda paginada requeridas por `purchasing`; la recongelación formal continúa
pendiente, pero producto autorizó la continuidad excepcional el 2026-08-11.
El instalador nuevo sólo se decidirá después de completar esos gates.
