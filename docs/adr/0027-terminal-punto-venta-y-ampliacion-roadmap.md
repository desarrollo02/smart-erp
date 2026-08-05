# ADR-0027 - Terminal de punto de venta y ampliación del roadmap

- Estado: Aceptado
- Fecha: 2026-08-01
- Decisión de producto: `point_of_sale` debe existir como plugin productivo para
  supermercados y pequeños comercios
- Modifica: cantidad, orden y fuera de alcance de
  [ADR-0011](0011-roadmap-dependencias-plugins-productivos.md)

> Nota vigente: ADR-0027 conserva la decisión histórica de agregar POS como orden
> 10. [ADR-0030](0030-familia-recursos-humanos-nomina-paraguay.md) y
> [ADR-0032](0032-plugin-estaciones-servicio-combustible.md) y
> [ADR-0033](0033-dominio-facturacion-recurrente.md) ampliaron después el roadmap a
> dieciocho reutilizables. [ADR-0034](0034-plugin-telemetria-vehicular.md)
> insertó después `vehicle_telemetry` como orden 7 y amplió el roadmap a
> diecinueve; POS ocupa ahora el 12 y `fuel_station` el 13.
>
> [ADR-0035](0035-operacion-offline-terminal-punto-venta.md) sustituye desde
> 2026-08-04 la decisión online de la sección 6: vender offline pasa a ser requisito
> obligatorio de la primera versión productiva de POS. ADR-0027 conserva aquí la
> decisión histórica y los límites de propiedad del plugin.

## Contexto

El roadmap original dejó POS fuera de la primera secuencia. Producto decidió que
la versión empresarial completa debe incluir un terminal de venta rápida para
comercios minoristas. Esta capacidad necesita una interacción distinta de una
pantalla administrativa de ventas: lectura continua de artículos, teclado o tacto,
cobro, emisión, impresión, devoluciones y operación concurrente de varias cajas.

Incluir esas responsabilidades dentro de `sales`, `treasury` o
`commercial_documents` mezclaría una experiencia de canal con los dominios que
siguen siendo fuentes de verdad. El terminal debe poder evolucionar sin duplicar
precios, stock, documentos ni dinero.

## Decisión

### 1. Plugin y nueva cantidad

Se agrega `point_of_sale` como plugin funcional reutilizable. En este corte, el
roadmap pasó de doce a **trece plugins reutilizables**, más exactamente una personalización
`CUSTOMIZATION` distinta por empresa.

Una distribución completa para una empresa contiene catorce plugins productivos:
trece reutilizables y su personalización. Para `N` empresas puede contener
`13 + N`, sin implicar que todos estén activos para cada empresa.

### 2. Orden de construcción

`point_of_sale` ocupa el orden 10, después de `treasury` y antes de cuentas por
cobrar:

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
11. `accounts_receivable`;
12. `accounts_payable`;
13. `accounting`;
14. al final, `<empresa>_customization`.

Se ubica después de tesorería porque una venta de mostrador completa necesita
precios, disponibilidad, confirmación comercial, documento y liquidación de uno o
varios medios de pago ya estabilizados. No se posterga hasta contabilidad porque
ningún dominio operativo depende de ella.

### 3. Responsabilidad del plugin

`point_of_sale` será dueño de:

- terminales registrados por empresa y establecimiento;
- sesiones operativas del cajero y estado de venta en curso;
- carrito rápido, suspensión y recuperación controlada de operaciones;
- coordinación idempotente del checkout;
- perfiles de periféricos y trabajos de impresión, sin almacenar secretos;
- permisos específicos, auditoría y pantallas de operación del terminal.

Usará contratos públicos para:

- buscar conceptos, unidades, precios e impuestos en `commercial_catalog`;
- consultar y afectar disponibilidad mediante `inventory`;
- materializar la venta y sus correcciones en `sales`;
- emitir ticket, factura o nota canónica mediante `commercial_documents`;
- registrar cobros, pagos divididos, caja y conciliación mediante `treasury`.

La dependencia exacta y sus rangos se aprobarán en la caracterización. La intención
inicial es requerir catálogo, inventario, ventas, documentos y tesorería. No se
autoriza acceso JPA, SQL ni importación de internos entre esos plugins.

### 4. Frontera fiscal y por país

El POS no calcula ni certifica reglas fiscales nacionales. `sifen` continúa como
adaptador separado para Paraguay y no se convierte en dependencia obligatoria del
terminal. El POS solicita un documento canónico y muestra su estado fiscal mediante
contratos públicos cuando exista un adaptador efectivo para la empresa.

Una empresa de otro país podrá sustituir SIFEN por su integración correspondiente
sin reescribir el terminal.

### 5. Experiencia de uso y dispositivos

La interfaz seguirá siendo Jakarta Faces 4.1 con Material Design 3, pero estará
optimizada para operación repetitiva:

- navegación completa por teclado y foco predecible;
- controles táctiles y acciones principales claramente diferenciadas;
- lectura de código de barras compatible inicialmente con dispositivos que operan
  como teclado;
- búsqueda rápida, cantidades, pagos divididos, vuelto y confirmación inequívoca;
- estados visibles de red, terminal, caja, impresora y operación;
- responsive en 375, 720 y 1280 px, con validación adicional del tamaño de terminal
  que se apruebe durante la caracterización;
- recuperación segura ante doble envío, refresco, timeout o respuesta incierta.

Integraciones nativas con balanza, cajón, impresora fiscal u otros dispositivos se
harán mediante adaptadores versionados y evaluados. No se inyectarán JavaScript,
drivers o ejecutables arbitrarios desde el plugin.

### 6. Operación online inicial — decisión histórica sustituida

La primera versión será online y requerirá conectividad con el servidor Logixone.
El modo offline queda fuera de este ADR: necesita definir almacenamiento local
cifrado, identidad, numeración, stock, límites de crédito, sincronización,
idempotencia, conflictos, fiscalidad y recuperación. No se simulará offline mediante
caché del navegador sin una nueva decisión arquitectónica y matriz de seguridad.

La decisión vigente es ADR-0035: una terminal aprovisionada deberá vender al menos
en efectivo durante una caída de conectividad, persistir un diario local cifrado y
sincronizar de forma idempotente. La topología local exacta se decide en
`POS-OFF-00` antes de implementar el dominio.

## Consecuencias

### Positivas

- supermercados y pequeños comercios obtienen un recorrido de caja especializado;
- ventas, inventario, documentos y tesorería conservan sus propias fuentes de
  verdad;
- el terminal puede activarse por empresa y personalizarse mediante contratos;
- la solución fiscal continúa reemplazable por país;
- la concurrencia de varias cajas se diseña explícitamente y no como formularios
  administrativos reutilizados a la fuerza.

### Costes y riesgos

- el roadmap crece a trece plugins reutilizables;
- checkout exige idempotencia y compensación entre varios dominios;
- periféricos, impresión y latencia requieren una matriz adicional de ambientes;
- descuentos, anulaciones, devoluciones y aperturas/cierres de caja necesitan
  permisos finos y auditoría;
- el modo offline obligatorio amplía runtime local, seguridad, sincronización,
  instalación, soporte y pruebas de la primera versión productiva.

## Alternativas descartadas

### Incorporar el terminal dentro de `sales`

Se descarta porque mezclaría el dominio de ventas con una UI de canal, periféricos,
sesiones de caja y recuperación operacional específica.

### Incorporarlo dentro de `treasury`

Se descarta porque tesorería administra dinero, cajas y conciliación, no carrito,
artículos, cantidades, promociones ni emisión comercial.

### Aplicación independiente con datos propios

Se descarta como fuente de verdad porque duplicaría catálogo, stock, precios y
documentos. Un cliente especializado futuro podrá consumir el contrato del plugin,
pero no mantener un ERP paralelo.

## Condiciones antes de implementar

1. cerrar los plugins predecesores y sus contratos públicos relevantes;
2. caracterizar supermercados y pequeños comercios sin copiar código legado;
3. aprobar decisiones sobre terminal, turno, carrito, devolución, concurrencia,
   impresión, periféricos y disponibilidad;
4. definir la transacción distribuida o saga idempotente del checkout;
5. establecer permisos, auditoría, datos de demostración y seguridad negativa;
6. ejecutar demo real con al menos dos terminales concurrentes, teclado, táctil,
   lector simulado, varios medios de pago y recuperación ante reintento;
7. mantener la personalización empresarial como último plugin de la composición.
8. aprobar `POS-OFF-00`, la topología local, el análisis de amenazas y la matriz de
   sincronización exigidos por ADR-0035.

## Referencias

- [ADR-0011 - Roadmap de plugins productivos](0011-roadmap-dependencias-plugins-productivos.md)
- [ADR-0013 - Eventos e idempotencia por plugin](0013-eventos-integracion-outbox-por-plugin.md)
- [ADR-0017 - Interacción visual neutral](0017-interaccion-visual-neutral-de-plugins.md)
- [Épica de terminal de punto de venta](../backlog/epica-terminal-punto-venta.md)
- [ADR-0032 - Plugin para estaciones de servicio de combustible](0032-plugin-estaciones-servicio-combustible.md)
- [ADR-0033 - Dominio independiente de facturación recurrente](0033-dominio-facturacion-recurrente.md)
- [ADR-0035 - Operación offline obligatoria en POS](0035-operacion-offline-terminal-punto-venta.md)
