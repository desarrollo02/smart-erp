# Épica - Roadmap inicial de plugins productivos

- Estado: En ejecución; una fundación normativa R0 y diecinueve plugins ERP conservan su secuencia, tres
  plugins de operaciones del proveedor y seis de la familia cooperativa quedan
  planificados, tres plugins ERP tienen demo oficial y la recongelación de Sprint
  8 permanece pendiente
- Fecha de incorporación: 2026-07-28
- Prioridad: habilitadores autorizados mientras la validación independiente del kernel sigue pendiente
- Decisiones relacionadas: [ADR-0011](../adr/0011-roadmap-dependencias-plugins-productivos.md), [ADR-0027](../adr/0027-terminal-punto-venta-y-ampliacion-roadmap.md), [ADR-0030](../adr/0030-familia-recursos-humanos-nomina-paraguay.md), [ADR-0031](../adr/0031-facturacion-masiva-en-documentos-comerciales.md), [ADR-0032](../adr/0032-plugin-estaciones-servicio-combustible.md), [ADR-0033](../adr/0033-dominio-facturacion-recurrente.md), [ADR-0034](../adr/0034-plugin-telemetria-vehicular.md), [ADR-0035](../adr/0035-operacion-offline-terminal-punto-venta.md), [ADR-0036](../adr/0036-operaciones-proveedor-soporte-lanzamientos-conector.md), [ADR-0037](../adr/0037-familia-cooperativa-ahorro-credito-paraguay.md) y [ADR-0038](../adr/0038-plugin-datos-referencia-normativos.md)

## Objetivo

Construir progresivamente los dominios ERP como plugins con dueño, esquema,
migraciones, contratos, permisos y pantallas propios, evitando un plugin monolítico
y manteniendo la personalización exclusiva de cada empresa como última capa.

## Resultado esperado

La primera versión empresarial completa dispondrá de una fundación normativa
compartida, diecinueve plugins ERP numerados y un plugin de personalización
distinto por empresa. Para una empresa que utilice todo el alcance, la distribución
contendrá veintiún plugins productivos. Para `N` empresas podrá contener `20 + N`,
aunque la activación efectiva seguirá siendo independiente por empresa.

El catálogo futuro general incorpora además `customer_support`,
`release_management` y `support_connector`, más seis plugins para cooperativas de
ahorro y crédito, llegando a veintinueve reutilizables junto con `reference_data`.
Los dos primeros pertenecen
a la composición central del proveedor, el conector a la instalación cliente y la
familia cooperativa a un perfil vertical propio. No se pretende empaquetar los
veintinueve en una única distribución.

## Secuencia aprobada

| Fase | Orden | Plugin | Resultado funcional |
|---|---:|---|---|
| Fundación compartida | R0 | `reference_data` | países, monedas, procedencia y políticas por empresa |
| Fundaciones | 1 | `business_partners` | clientes, proveedores, contactos y direcciones |
| Fundaciones | 2 | `commercial_catalog` | productos, servicios, unidades, impuestos y precios |
| Operación | 3 | `inventory` | depósitos, stock, movimientos y reservas |
| Operación | 4 | `purchasing` | solicitudes, órdenes, recepciones y devoluciones |
| Operación | 5 | `sales` | presupuestos, pedidos y compromisos de venta |
| Operación | 6 | `logistics` | preparación, despacho, transporte y entrega |
| Operación | 7 | `vehicle_telemetry` | dispositivos, posición, recorridos, sensores, geocercas y seguimiento auditable |
| Documentos | 8 | `commercial_documents` | factura, notas, remisión, snapshots y generación masiva recuperable |
| Servicios recurrentes | 9 | `recurring_billing` | planes, suscripciones, prorrateo, uso medido y cargos |
| Fiscal | 10 | `sifen` | firma, transmisión y eventos fiscales versionados |
| Finanzas | 11 | `treasury` | cajas, bancos, pagos, cobros y conciliación |
| Canal de venta | 12 | `point_of_sale` | terminales, sesiones de cajero, carrito y checkout rápido online/offline con sincronización idempotente |
| Vertical | 13 | `fuel_station` | tanques, surtidores, picos, turnos, lecturas, despachos y conciliación húmeda |
| Finanzas | 14 | `accounts_receivable` | deuda de clientes, cuotas y cobranzas |
| Finanzas | 15 | `accounts_payable` | obligaciones, vencimientos y pagos a proveedores |
| Finanzas | 16 | `accounting` | asientos, períodos, mayores y cierres |
| Personas | 17 | `human_resources` | legajo, relación laboral, organización, ausencias y tiempo |
| Personas | 18 | `payroll` | conceptos, períodos, liquidaciones y recibos neutrales |
| País | 19 | `payroll_paraguay` | reglas y artefactos IPS/MTESS versionados |
| Empresa | último | `<empresa>_customization` | cambios exclusivos sobre contratos públicos estabilizados |

## Familia de operaciones del proveedor

Esta familia no recibe números 20–22 porque usa perfiles físicos distintos y no
altera las precedencias de dominio ERP:

| Plugin | Tipo | Composición | Resultado funcional |
|---|---|---|---|
| `customer_support` | funcional | central del proveedor | cobertura, instalaciones, tickets, SLA, conversaciones, diagnósticos y resolución |
| `release_management` | funcional | central del proveedor | defectos, mejoras, candidatos, compatibilidad, gates, notas y publicación |
| `support_connector` | técnico opcional | ERP del cliente | enlace HTTPS saliente, solicitudes y diagnósticos consentidos/sanitizados |

La clasificación técnica es de producto. El `PluginKind` actual sólo contiene
`FUNCTIONAL` y `CUSTOMIZATION`; SC-00 debe decidir y versionar de forma compatible
una eventual clase `TECHNICAL` antes del descriptor.

`support_connector` no abre puertos administrativos, no ejecuta comandos, scripts
o SQL, no actualiza el ERP y no bloquea la operación cuando el servicio central
está ausente. El proveedor no distribuye sus consolas centrales dentro del ERP del
cliente.

## Familia vertical cooperativa

Esta familia tampoco recibe números después de 19. Reutiliza fundaciones comunes
y tiene su propio orden C1–C6:

| Orden | Plugin | Resultado funcional |
|---:|---|---|
| C1 | `cooperative_membership` | socios, admisión, estado, aportes y desvinculación |
| C2 | `cooperative_governance` | asambleas, órganos, mandatos, votaciones y decisiones |
| C3 | `aml_compliance` | debida diligencia, riesgo, alertas y casos LA/FT |
| C4 | `cooperative_savings` | productos, cuentas, submayor, intereses y restricciones |
| C5 | `cooperative_credit` | solicitud, aprobación, cartera, garantías, mora y cobranza |
| C6 | `cooperative_regulatory_paraguay` | paquetes, cálculos y artefactos INCOOP/SEPRELAD |

El perfil mínimo futuro combina `business_partners`, `treasury`, `accounting` y
los seis anteriores. Ahorros no se guardan en tesorería y préstamos no se guardan
en `accounts_receivable`; esos plugins conservan liquidaciones y deuda comercial,
respectivamente. Reglas paraguayas se versionan en el adaptador nacional y no se
hardcodean en los dominios neutrales.

La secuencia, gates y fuentes oficiales están en
[ADR-0037](../adr/0037-familia-cooperativa-ahorro-credito-paraguay.md) y la
[épica cooperativa](epica-cooperativa-ahorro-credito-paraguay.md). La planificación
no autoriza código ni operación financiera durante Sprint 8.

## Política de ejecución

1. No se inicia un plugin mientras el anterior deje contratos o pruebas relevantes
   fallando.
2. El orden es de construcción; runtime usa el orden topológico validado del
   catálogo.
3. Cada plugin declara solo dependencias públicas necesarias, sin importar internos
   ni entidades ajenas.
4. Cada plugin persistente posee esquema `plg_<plugin_id>` y migraciones inmutables.
5. Un plugin desactivado conserva tablas, migraciones y datos.
6. Los plugins funcionales nunca dependen de una personalización.
7. El plugin empresarial se crea después de estabilizar las versiones de pantallas
   y extensiones que modificará.
8. Cada Sprint termina con demo visual responsive y PDF de cierre verificado.
9. Cada selector declara fuente y propietario; un catálogo empresarial tiene una
   ruta de administración antes de cerrar su historia.
10. En cada cierre se pregunta si se creará un instalador Windows. Con `SÍ` se
    genera y prueba contra el baseline final; con `NO` se conserva `current` y se
    documenta como no representativo del Sprint nuevo.
11. La familia de operaciones del proveedor se planifica en una línea separada;
    no inicia código durante Sprint 8 y requiere decisión explícita de prioridad
    para un Sprint posterior.
12. La familia cooperativa se planifica como otro perfil vertical; COOP-00 debe
    confirmar tipo, estatuto, fuentes vigentes, seguridad y reconciliación antes
    de crear módulos o tratar datos reales.

## Dependencias de producto

- `reference_data` es una fundación funcional compartida: publica países y monedas
  mediante `reference-data-api`, sin convertir el kernel en maestro.
- `business_partners` y `commercial_catalog` son independientes entre sí, requieren
  `reference_data` 1.x y no contienen lógica de ventas o documentos.
- `inventory` usa IDs y contratos del catálogo, nunca sus entidades.
- compras y ventas usan participantes, catálogo e inventario mediante contratos.
- logística se integra con reservas, pedidos y participantes mediante IDs/eventos.
- telemetría usa el `VehicleId` público de logística y posee dispositivos,
  observaciones, recorridos y tracking lifecycle; logística y documentos operan
  cuando el plugin está ausente o inactivo.
- documentos conserva snapshots, ejecuta lotes idempotentes y no relee maestros
  para cambiar el pasado;
- facturación recurrente conserva planes, suscripciones, prorrateos y uso
  facturable; publica candidatos inmutables al contrato de documentos sin poseer
  facturas;
- SIFEN traduce una proyección pública del documento canónico.
- tesorería publica movimientos y liquidaciones; cobrar/pagar no accede a sus
  tablas.
- punto de venta coordina catálogo, inventario, ventas, documentos y tesorería
  mediante contratos; no duplica sus fuentes de verdad ni depende directamente de
  SIFEN. Cuando está offline conserva sólo proyecciones versionadas y un diario
  local cifrado; al reconectar sincroniza efectos idempotentes y hace visibles los
  conflictos.
- estaciones de servicio posee la operación física y el inventario húmedo; usa
  catálogo e inventario por contratos y puede entregar despachos a POS sin poseer
  carrito, documento, cobro o deuda.
- contabilidad consume eventos o contratos de lectura; ningún plugin operativo
  depende de ella.
- recursos humanos conserva legajo e historia laboral sin convertir al empleado en
  participante comercial o usuario.
- nómina requiere contratos públicos de recursos humanos y publica hechos de pago
  o contabilización sin acceder a tablas financieras.
- `payroll_paraguay` adapta reglas y artefactos oficiales versionados sin acceder a
  tablas privadas de nómina.
- personalización depende únicamente de los contratos que modifica y se compone al
  final.
- `customer_support` requiere `business-partners-api` y puede usar opcionalmente
  contratos públicos comerciales y de releases; nunca posee facturas,
  suscripciones o artefactos.
- `release_management` posee cambios y releases y no depende de soporte; publica
  comandos/eventos propios para evitar un ciclo.
- `support_connector` vive en otra distribución: usa un protocolo público HTTPS
  saliente, no una dependencia runtime requerida del plugin central, y conserva
  sólo identidad, consentimiento, cola y auditoría mínimos.
- `cooperative_membership` referencia participantes por API y conserva membresía,
  aportes y estado sin duplicar el maestro comercial.
- `cooperative_governance` consume snapshots públicos de socios para asambleas y
  órganos sin trasladar identidad o autorización al kernel.
- `aml_compliance` publica decisiones y recibe observaciones tipadas; no lee ni
  modifica saldos privados.
- `cooperative_savings` posee la obligación con el socio y usa tesorería sólo para
  liquidar dinero; `cooperative_credit` posee la cartera y no depende de cuentas
  por cobrar comerciales.
- `cooperative_regulatory_paraguay` consume proyecciones públicas e identifica
  fuente, versión, vigencia y checksum de reglas/artefactos; no posee asientos ni
  submayores.

La dependencia exacta de cada versión se aprobará en la historia de diseño del
plugin. Esta épica no autoriza dependencias circulares ni un módulo compartido con
entidades de varios dominios.

## Habilitadores previos

Antes de `business_partners` deben completarse:

- validación independiente pendiente de `J11-S4-08` antes de cualquier promoción;
- descubrimiento y orden reproducible de migraciones `plg_*`;
- plantilla mínima de plugin productivo;
- contrato rector de eventos/outbox de ADR-0013; la infraestructura se materializa
  con el primer intercambio asíncrono real;
- backlog y criterios de aceptación del Sprint del plugin.

Estos habilitadores son trabajo de plataforma y no se cuentan como plugins
productivos.

## Criterios de aceptación de la épica

- **CE-01:** existen diecinueve plugins ERP reutilizables con propietarios separados.
- **CE-02:** cada plugin persistente usa esquema y migraciones propios.
- **CE-03:** no existen relaciones JPA, repositorios ni joins privados entre plugins.
- **CE-04:** las dependencias son acíclicas y compatibles.
- **CE-05:** cada plugin puede estar físicamente presente y desactivado por empresa.
- **CE-06:** retirar o desactivar no elimina datos automáticamente.
- **CE-07:** documentos comerciales son independientes del formato SIFEN.
- **CE-08:** contabilidad no es dependencia de dominios operativos.
- **CE-09:** cada empresa operativa tiene exactamente una personalización propia.
- **CE-10:** la personalización se compone después de todos los funcionales.
- **CE-11:** cada pantalla productiva usa JSF, Material Design 3 y responsive.
- **CE-12:** cada Sprint presenta una demo visual sobre el artefacto probado.
- **CE-13:** la guía para implementadores explica selección, activación y
  personalización de cada capacidad incorporada.
- **CE-14:** cada cierre registra la decisión `SÍ`/`NO` sobre el instalador; con
  `SÍ` corresponde al baseline final, diagnostica la máquina y conserva datos, y
  con `NO` el artefacto anterior permanece intacto y no se presenta como vigente.
- **CE-15:** cada selector tiene fuente/propietario y todo catálogo empresarial
  dispone de administración autorizada y auditable.
- **CE-16:** la primera versión productiva de `point_of_sale` vende al menos en
  efectivo con una terminal aprovisionada y sin Internet, conserva un diario local
  durable y sincroniza sin duplicar efectos.
- **CE-17:** el catálogo global planifica veintinueve reutilizables, pero las
  composiciones de cliente y proveedor no incluyen plugins ajenos a su función.
- **CE-18:** `customer_support` y `release_management` no forman parte por defecto
  de la distribución enviada a clientes.
- **CE-19:** `support_connector` es opcional, sólo inicia conexiones salientes y no
  ejecuta código o consultas remotas.
- **CE-20:** ausencia, caída, desconexión o desactivación del conector no bloquea
  ningún recorrido ERP ni elimina datos.
- **CE-21:** los seis plugins cooperativos tienen propietarios y contratos
  separados; no convierten socios en clientes, ahorro en caja ni préstamos en
  cuentas por cobrar comerciales.
- **CE-22:** aportes, ahorros y créditos usan libros append-only, reversos,
  idempotencia y conciliación con tesorería/contabilidad.
- **CE-23:** reglas y presentaciones paraguayas conservan fuente oficial, versión,
  vigencia y checksum sin afirmar certificación automática.

## Fuera de alcance inicial

- manufactura o producción;
- CRM especializado;
- analítica transversal o data warehouse;
- carga dinámica de JAR;
- compartir una personalización entre empresas.

Estas capacidades pueden convertirse en plugins futuros mediante nuevas épicas y
decisiones explícitas.

Un CRM especializado continúa fuera de alcance. `customer_support` se limita a
atención posventa, cobertura, casos y resolución; no agrega marketing, prospección
o pipeline comercial.

## Trabajo actual autorizado

Sprint 5 implementó composición, migraciones y plantilla, y aceptó en ADR-0013 el
contrato de eventos necesario sin inventar infraestructura. Sprint 6 completó
[caracterización](../knowledge-base/business-partners/legacy-characterization.md),
aceptó BP-D01 a BP-D10, cerró dominio/API pública en J11-S6-02 y agregó en
J11-S6-03 la V1 privada, JPA y repositorios con PostgreSQL real.

`J11-S6-04` completó comandos, consultas, cuatro permisos, autorización actual y
auditoría técnica central del plugin. `J11-S6-05` completó la primera interfaz JSF
Material Design 3 responsive, el contrato neutral 0.4.0 y Playwright en compacto,
medio, expandido y límites. `J11-S6-06` incorporó plugin y migraciones al perfil
físico reproducible `with-business-partners-demo`, construyó el par de imágenes y
validó conservación de datos. `J11-S6-07` dejó verdes reactor, arquitectura,
PostgreSQL, Docker/Compose, health, OIDC y la demo Playwright final; el cierre
formal continúa condicionado a G7 independiente.

[Sprint 7](../sprints/sprint-07/README.md) completó técnicamente
`commercial_catalog`: gobierno, caracterización, CC-D01 a CC-D10, API/dominio,
persistencia, aplicación, permisos, UI, composición, imágenes, demo acumulada y
PDF están verdes en G0-G6. El cierre formal continúa condicionado a G7
independiente.

[Sprint 8](../sprints/sprint-08/README.md) inició `inventory`. J11-S8-01 completó
la caracterización y producto confirmó IN-D01 a IN-D10 sin cambios el 2026-07-31.
J11-S8-02 materializó y validó `inventory-api@1.0.0`, el descriptor con dependencia
requerida del catálogo y el dominio neutral. J11-S8-03 agregó ADR-0024, el esquema
privado V1, nueve mapeos JPA y seis repositorios acotados por empresa; PostgreSQL,
arquitectura y reactor quedaron verdes. J11-S8-04 agregó V2, siete permisos,
aplicación autorizada y auditada, contratos CDI y frontera JTA. J11-S8-05 agregó
tres recorridos visuales neutrales, directorios empresariales, handlers autorizados
y presentación mediante el shell. J11-S8-06 agregó el perfil
`with-inventory-demo`, composición coherente de WAR/migrador, V1–V2 idempotentes,
imágenes, activación y permisos administrativos y una demo real responsive de
inventario. J11-S8-07 completó los gates integrales, congeló imágenes, ejecutó la
demo oficial y generó fotografía, retrospectiva y PDF. J11-S8-08 produjo y probó
el primer instalador interno. J11-S8-C01 reabrió el baseline, corrigió la
administración visual de perfiles tributarios y dejó verdes la candidata, la demo
responsive y los gates afectados. ADR-0028 agregó la remediación transversal de
selectores, que debe resolverse antes de `purchasing`. Después se recongelarán la
fotografía y el PDF y se preguntará si se creará un instalador nuevo; no se
adelantan `point_of_sale`, la familia de recursos humanos ni una personalización
empresarial real. ADR-0030 agregó `human_resources`, `payroll` y
`payroll_paraguay`; ADR-0032 agregó `fuel_station`; ADR-0033 insertó
`recurring_billing` y ADR-0034 agregó `vehicle_telemetry` como orden 7. El roadmap
vigente ubica `commercial_documents` en 8, `recurring_billing` en 9, POS en 12,
`fuel_station` en 13 y la familia de personas en 17–19. Ningún código futuro
comienza hasta completar los predecesores.
ADR-0035 no modifica el orden: incorpora `POS-OFF-00` a `POS-OFF-06` y hace
obligatoria la venta offline en la primera versión productiva del POS.

ADR-0036 agrega documentalmente la familia de operaciones del proveedor y las
épicas de soporte, releases y conector seguro. No modifica el reactor, la
composición ejecutable ni el trabajo autorizado de Sprint 8. Antes de implementar
deben cerrarse identidad del portal, SLA, protocolo, threat model, consentimiento,
retención y fuente de verdad de releases.

ADR-0037 agrega documentalmente la familia para cooperativas de ahorro y crédito.
No modifica el reactor ni la composición ejecutable. COOP-00 queda condicionado a
una prioridad futura y a contratos estables de participantes, tesorería y
contabilidad; la familia no se presenta como implementada ni habilita captación o
crédito real.

El refinamiento posterior de
[COOP-00](COOP-00-gobierno-alcance-matriz-normativa.md) define quince decisiones,
fuentes, matriz de trazabilidad, seguridad, conciliación, migración y gates G0–G5.
La historia está suficientemente descrita para recibir información de una
cooperativa, pero no se ejecuta ni autoriza un Sprint por este cambio documental.

ADR-0038 agrega `reference_data` como fundación R0 sin renumerar ERP 1–19. El
primer corte ejecutable incorpora API Java pura, V1 privada con procedencia y
subconjunto explícito `PY/PYG/USD`, consulta autorizada y consumo transaccional
desde socios y catálogo. El catálogo global pasa a veintinueve reutilizables. La
publicación completa, importador, administración de políticas y gates finales de
RD-06 continúan pendientes antes de iniciar `purchasing`.
