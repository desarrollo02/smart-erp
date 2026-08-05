# ADR-0032 — Plugin para estaciones de servicio de combustible

- Estado: Aceptado
- Fecha: 2026-08-02
- Decisión de producto: incorporar estaciones de servicio como plugin del ERP
- Fuentes: legados Multienvíos/Ingenio La Felsina y referencias oficiales MIC,
  INTN y MADES verificadas el 2026-08-02

> Nota vigente: esta ADR incorporó históricamente `fuel_station` como orden 11 y
> llevó el roadmap a diecisiete. [ADR-0033](0033-dominio-facturacion-recurrente.md)
> insertó después `recurring_billing` en el orden 8; `fuel_station` pasó entonces al
> orden 12. [ADR-0034](0034-plugin-telemetria-vehicular.md) insertó luego
> `vehicle_telemetry` como orden 7; `fuel_station` ocupa actualmente el orden 13 y
> el roadmap contiene diecinueve reutilizables (`19 + N`).

## Contexto

Una estación de servicio combina almacenamiento físico, medición regulada,
recepciones, despachos, turnos, precios, venta rápida, cobro, facturación y
conciliación. Colocar todo dentro de `inventory` reduciría tanques y surtidores a
depósitos comunes. Colocarlo dentro de `point_of_sale` mezclaría medición física y
operación de playa con el checkout. Un plugin vertical sin límites claros podría,
en cambio, duplicar catálogo, stock, caja, documentos y crédito.

Las fuentes legadas solo muestran consumo de combustible para flota mediante
movimientos genéricos de stock. No modelan la operación completa. Las referencias
oficiales confirman que tanques, máquinas, picos, capacidad/producto, verificación,
volumen/precio, precintos y evidencia ambiental son conceptos explícitos que el
sistema debe poder identificar y conservar.

## Decisión

### 1. Nuevo plugin y orden

Se agrega `fuel_station` como plugin funcional reutilizable **número 11**, después
de `point_of_sale` y antes de `accounts_receivable`. El roadmap pasa a diecisiete
plugins reutilizables:

1. `business_partners`;
2. `commercial_catalog`;
3. `inventory`;
4. `purchasing`;
5. `sales`;
6. `logistics`;
7. `commercial_documents`;
8. `sifen`;
9. `treasury`;
10. `point_of_sale`;
11. `fuel_station`;
12. `accounts_receivable`;
13. `accounts_payable`;
14. `accounting`;
15. `human_resources`;
16. `payroll`;
17. `payroll_paraguay`.

Una distribución completa para `N` empresas podrá contener `17 + N` plugins
productivos, sin que todos deban estar activos para cada empresa. La
personalización continúa al final.

### 2. Propiedad funcional

`fuel_station` será dueño de:

- estaciones, playas/islas, tanques, surtidores y picos;
- estados operativos y relaciones físicas entre esos recursos;
- turnos de playa y asignaciones de actores;
- lecturas totalizadoras y mediciones de tanque;
- recepciones físicas de combustible;
- despachos y cursor idempotente de importación;
- conciliación de inventario húmedo, tolerancias y diferencias;
- evidencia de verificación metrológica, precintos e incidentes;
- contratos neutrales para importar datos de dispositivos.

No será dueño de producto, precio maestro, impuesto, stock contable, compra, venta,
factura, CDC, caja, deuda, empleado ni asiento.

### 3. Dependencias y composición

Dependencias funcionales previstas:

- requerida: `commercial_catalog` para producto, unidad, tratamiento y precio
  referenciado;
- requerida: `inventory` para publicar movimientos de stock contable;
- opcional: `purchasing`/`logistics` para relacionar la recepción física con orden,
  remisión y transporte;
- opcional por capacidad: `point_of_sale` para convertir un despacho en checkout;
- indirectas mediante POS/contratos: `commercial_documents`, `treasury` y
  `accounts_receivable`;
- sin dependencia hacia `human_resources`: el turno usa `ActorId`; RR. HH. podrá
  consumir referencias públicas después.

El orden actual 13 permite estabilizar antes POS y los contratos transaccionales. No
obliga a activar POS en un puesto de consumo propio que solo gestione tanques y
despachos internos.

### 4. Stock operativo frente a stock contable

El plugin conserva mediciones físicas e inventario húmedo. La ecuación de cierre
considera apertura, recepciones, despachos, ensayos/ajustes y cierre medido. El
resultado se publica idempotentemente a `inventory`, que continúa siendo la fuente
de stock contable.

No se sincronizan entidades ni se ejecutan joins entre esquemas. Una discrepancia
no altera automáticamente stock ni documentos: crea un resultado que requiere la
política y autorización correspondientes.

### 5. Venta y fiscalidad

Un despacho puede referenciar una operación POS mediante IDs públicos. POS conserva
carrito, sesión y coordinación de checkout; `commercial_documents` emite el
documento; `treasury` registra el cobro; `sifen` produce y transmite la
representación fiscal. `fuel_station` conserva volumen, precio efectivo e importe
como snapshot operativo para poder conciliar.

La estación no llama tablas fiscales ni duplica XML/CDC. La desactivación de SIFEN
no borra despachos o ventas y la activación de la estación no obliga por sí sola a
usar un adaptador fiscal de un país.

### 6. Hardware y seguridad

La primera versión será manual/importadora: captura lecturas, recepciones y
despachos por UI, archivo o API autenticada. No autorizará bombas, abrirá válvulas,
modificará precios de un surtidor ni ejecutará control remoto.

Cada protocolo de fabricante requiere un adaptador técnico versionado con:

- mapeo a contratos públicos de dispositivo;
- identidad de estación/equipo y autenticación mutua;
- allowlist de red y mínimo privilegio;
- cursor, idempotencia, firma/hash cuando aplique y reloj confiable;
- buffer, reintento, cuarentena, métricas y recuperación;
- pruebas con simulador y hardware autorizado;
- ADR de seguridad antes de habilitar cualquier comando de salida.

El dominio nunca contiene SDK, DTO o nombres de registros de un fabricante.

### 7. Evidencia regulatoria

El plugin puede registrar versión, emisor, vigencia, instrumento, serie, resultado,
certificado y precintos. Esos datos son evidencia operativa, no una declaración
automática de cumplimiento.

Las reglas nacionales ejecutables no se incrustan en el núcleo. Un futuro
`fuel_station_paraguay` solo se propondrá si reportes, catálogos o validaciones
oficiales versionadas demuestran un adaptador nacional autónomo. Hasta entonces se
usan catálogos configurados y documentos de evidencia con verificación humana.

### 8. Alcance inicial

La primera edición cubre combustibles líquidos y:

- configuración de estación/tanque/surtidor/pico;
- turnos y lecturas;
- recepción y despacho;
- conciliación de inventario húmedo;
- integración pública con inventario y POS;
- evidencias de verificación e incidentes.

Quedan fuera GLP, GNV, hidrógeno, carga eléctrica, control remoto, tienda de
conveniencia, lavadero, fidelización, flota integral, fijación regulada de precios y
automatización de cumplimiento.

### 9. Seguridad, permisos y auditoría

Permisos iniciales previstos:

- `fuel_station.view`;
- `fuel_station.configuration.manage`;
- `fuel_station.shifts.operate`;
- `fuel_station.readings.record`;
- `fuel_station.deliveries.receive`;
- `fuel_station.dispenses.import`;
- `fuel_station.reconciliations.close`;
- `fuel_station.variances.approve`;
- `fuel_station.compliance.manage`;
- `fuel_station.integrations.manage`.

Cada operación revalida empresa, estación, recurso y versión. Auditoría y logs usan
IDs, conteos, volumen agregado y códigos seguros; no exponen credenciales de
dispositivo, certificados privados, datos completos de clientes ni payloads crudos.

## Consecuencias

### Positivas

- la operación física tiene propietario sin contaminar POS o inventario;
- stock húmedo y contable se concilian sin duplicar tablas;
- los fabricantes pueden variar mediante adaptadores;
- estaciones de venta y puestos de consumo propio comparten el mismo dominio;
- cumplimiento y evidencia permanecen versionables por país.

### Costes y riesgos

- se agrega un decimoséptimo plugin y cambian los órdenes posteriores;
- la integración con dispositivos amplía la superficie de seguridad y operación;
- medición, temperatura y tolerancias exigen precisión y pruebas específicas;
- una mala política de diferencias puede crear ajustes contables incorrectos;
- regulaciones y modelos de equipo requieren revisión vigente antes de operar.

## Alternativas descartadas

### Incorporarlo en `inventory`

Se descarta porque inventario no debe poseer surtidores, totalizadores, turnos,
precintos ni protocolos de playa.

### Incorporarlo en `point_of_sale`

Se descarta porque puede existir un puesto de consumo propio sin venta y porque el
checkout no es dueño de tanques o mediciones.

### Plugin por fabricante de surtidor

Se descarta como dominio. Los fabricantes aportan adaptadores técnicos opcionales;
la estación mantiene un contrato neutral.

### Agregar desde ahora `fuel_station_paraguay`

Se difiere hasta identificar un contrato oficial ejecutable, versionado y distinto
de conservar evidencias/configuración.

## Verificación futura obligatoria

La implementación deberá caracterizar operaciones reales de una estación,
confirmar FS-D01–FS-D10, congelar normativa aplicable, diseñar API Java pura,
migraciones privadas, outbox/inbox, seguridad de dispositivos, precisión,
concurrencia, recuperación, Playwright responsive, Docker/Compose, health,
observabilidad y demo visual. No se inicia antes de completar el orden 4–12.

## Referencias

- [Caracterización de estaciones de servicio](../knowledge-base/fuel-station/legacy-characterization.md)
- [Épica de `fuel_station`](../backlog/epica-estaciones-servicio-combustible.md)
- [ADR-0027 — Point of sale](0027-terminal-punto-venta-y-ampliacion-roadmap.md)
- [ADR-0031 — Facturación masiva](0031-facturacion-masiva-en-documentos-comerciales.md)
- [ADR-0033 — Dominio independiente de facturación recurrente](0033-dominio-facturacion-recurrente.md)
- [ADR-0034 — Plugin de telemetría vehicular](0034-plugin-telemetria-vehicular.md)
