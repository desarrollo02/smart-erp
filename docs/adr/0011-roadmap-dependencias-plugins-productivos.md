# ADR-0011 - Roadmap y dirección de dependencias de plugins productivos

- Estado: Aceptado; cantidad y orden ERP ampliados por ADR-0027, ADR-0030,
  ADR-0032, ADR-0033 y ADR-0034; alcance offline de POS ampliado por ADR-0035;
  familia separada de operaciones del proveedor agregada por ADR-0036 y familia
  vertical cooperativa agregada por ADR-0037
- Fecha: 2026-07-28
- Decisión de producto: construir doce plugins reutilizables y una personalización distinta por empresa
- Inicio condicionado: reemplazado parcialmente por ADR-0012; los habilitadores
  pueden avanzar con validación independiente pendiente, sin promoción ni producción

> Nota vigente: [ADR-0027](0027-terminal-punto-venta-y-ampliacion-roadmap.md)
> agrega `point_of_sale` en el orden 10 y
> [ADR-0030](0030-familia-recursos-humanos-nomina-paraguay.md) agrega
> `human_resources`, `payroll` y `payroll_paraguay`. Posteriormente,
> [ADR-0032](0032-plugin-estaciones-servicio-combustible.md) insertó
> `fuel_station`; [ADR-0033](0033-dominio-facturacion-recurrente.md) agrega después
> `recurring_billing` en el orden 8. Finalmente,
> [ADR-0034](0034-plugin-telemetria-vehicular.md) inserta
> `vehicle_telemetry` después de `logistics`, en el orden 7. El orden actual de
> `commercial_documents` es 8, `recurring_billing` es 9, POS es 12,
> `fuel_station` es 13 y la familia de personas ocupa 17 a 19. La
> lista de doce y las exclusiones que siguen abajo conservan la decisión histórica
> original; el roadmap actual tiene diecinueve reutilizables más una
> personalización por empresa.
> [ADR-0035](0035-operacion-offline-terminal-punto-venta.md) no cambia ese orden:
> exige que la primera versión productiva de POS permita ventas offline durables y
> sincronización idempotente.
>
> [ADR-0031](0031-facturacion-masiva-en-documentos-comerciales.md) asigna la
> facturación masiva a `commercial_documents`; no agrega otro plugin ni modifica
> el orden.
>
> [ADR-0036](0036-operaciones-proveedor-soporte-lanzamientos-conector.md) agrega
> `customer_support`, `release_management` y el técnico opcional
> `support_connector`. El catálogo global futuro pasa a veintidós reutilizables,
> pero estos tres forman una familia de operaciones del proveedor y no renumeran
> la secuencia ERP 1–19 ni se componen juntos por defecto.
>
> [ADR-0037](0037-familia-cooperativa-ahorro-credito-paraguay.md) agrega
> `cooperative_membership`, `cooperative_governance`, `aml_compliance`,
> `cooperative_savings`, `cooperative_credit` y
> `cooperative_regulatory_paraguay`. El catálogo global futuro pasa a veintiocho
> reutilizables. La familia es un perfil vertical separado que reutiliza
> `business_partners`, `treasury` y `accounting`; no recibe órdenes 20–25, no
> renumera ERP 1–19 y no autoriza implementación durante Sprint 8.

## Contexto

El kernel inicial ya define empresas, identidad, autorización, auditoría, registro,
activación y composición de plugins. Antes de implementar dominios ERP es necesario
decidir sus propietarios, la secuencia de construcción y la dirección general de
dependencias.

Comenzar directamente por facturación obligaría a inventar representaciones
provisionales de clientes, productos, stock, transporte y pagos. Un único plugin
“ERP” volvería a concentrar ventas, inventario, finanzas e integración fiscal y
recrearía el acoplamiento del sistema legado.

La secuencia de desarrollo no reemplaza el grafo técnico. En runtime,
`PluginCatalogResolver` continuará validando dependencias, compatibilidad, ciclos y
orden topológico. La lista acordada define el roadmap de producto y una dirección
de diseño; cada plugin deberá declarar solamente dependencias públicas realmente
necesarias.

## Decisión

### 1. Cantidad inicial

La primera versión empresarial completa tendrá doce tipos de plugin funcional o de
integración reutilizables:

1. `business_partners`;
2. `commercial_catalog`;
3. `inventory`;
4. `purchasing`;
5. `sales`;
6. `logistics`;
7. `commercial_documents`;
8. `sifen`;
9. `treasury`;
10. `accounts_receivable`;
11. `accounts_payable`;
12. `accounting`.

Cada empresa agrega exactamente un plugin `CUSTOMIZATION` propio. Por tanto, una
distribución que contenga los doce reutilizables y personalizaciones para `N`
empresas contendrá `12 + N` plugins productivos. La presencia física no implica que
todos estén activos para todas las empresas.

`reference-plugin`, `reference-customization-a` y `reference-customization-b` son
fixtures técnicos y no se cuentan como plugins productivos.

### 2. Orden de construcción

| Orden | Plugin | Propiedad principal | Motivo de precedencia |
|---:|---|---|---|
| 1 | `business_partners` | personas, organizaciones, clientes, proveedores, contactos y direcciones | los procesos posteriores necesitan participantes estables |
| 2 | `commercial_catalog` | productos, servicios, categorías, unidades, impuestos y precios | inventario y transacciones necesitan conceptos comerciales públicos |
| 3 | `inventory` | depósitos, existencias, lotes, movimientos y reservas | compras, ventas y logística necesitan una frontera de stock |
| 4 | `purchasing` | solicitudes, órdenes, recepciones y devoluciones a proveedor | materializa primero el flujo de entrada y prueba inventario |
| 5 | `sales` | presupuestos, pedidos, condiciones y compromisos de venta | usa participantes, catálogo, precios y disponibilidad |
| 6 | `logistics` | preparación, despacho, transporte, origen, destino, vehículos y transportistas | entrega y remisión necesitan contratos logísticos previos |
| 7 | `commercial_documents` | factura, notas, remisión, numeración, snapshots, impuestos, totales y generación masiva | se diseña sobre contratos comerciales y logísticos estabilizados |
| 8 | `sifen` | proyección fiscal, firma, transmisión, respuestas y eventos | traduce documentos canónicos sin invadir su dominio |
| 9 | `treasury` | cajas, bancos, medios de pago, movimientos y conciliación | prepara la infraestructura de cobros y pagos |
| 10 | `accounts_receivable` | deuda de clientes, cuotas, vencimientos, cobranzas y saldos | nace de documentos emitidos y liquidaciones de tesorería |
| 11 | `accounts_payable` | obligaciones con proveedores, vencimientos y pagos | nace de compras/documentos recibidos y tesorería |
| 12 | `accounting` | plan de cuentas, asientos, períodos, mayores y cierres | consume hechos estabilizados; ningún dominio operativo depende de contabilidad |
| último | `<empresa>_customization` | diferencias exclusivas de la empresa sobre contratos públicos | necesita versiones estables de todas las pantallas y extensiones que modifica |

Este orden no obliga a activar todos los plugins ni convierte cada predecesor en una
dependencia Maven. Una dependencia se declara solo cuando el contrato público es
necesario y debe permanecer acíclica.

### 3. Dirección de dependencias

La dirección preferida es:

```text
business_partners + commercial_catalog
                  |
              inventory
             /         \
      purchasing       sales
             \         /
               logistics
                   |
        commercial_documents
                   |
                 sifen

treasury -> accounts_receivable / accounts_payable -> accounting

todos los funcionales efectivos -> personalización de la empresa
```

El diagrama expresa flujo de conocimiento, no acceso a entidades. Los límites
continúan siendo:

- contratos públicos, IDs y eventos entre plugins;
- cero relaciones JPA entre plugins;
- cero lectura o escritura de esquemas privados ajenos;
- snapshots cuando un documento histórico necesita datos externos;
- ningún plugin operativo depende de `accounting` o de una personalización;
- `sifen` depende de una proyección fiscal pública de `commercial_documents`;
- la personalización depende solo de los plugins cuyos contratos modifica.

Si dos dominios parecen necesitarse mutuamente, antes de codificar se deberá
extraer un contrato de una sola dirección o usar eventos. No se autoriza crear un
módulo compartido genérico con entidades de ambos.

### 4. Propiedad de datos y UI

Cada plugin persistente será propietario de su esquema `plg_<plugin_id>`,
migraciones, entidades, casos de uso, permisos y contratos públicos. Las pantallas
se publicarán como contratos neutrales renderizados por el shell JSF Material
Design 3.

La personalización se construye al final de la implementación de una empresa y se
compone al final en runtime. Puede cambiar pantallas ajenas solo mediante
`ScreenId`, elementos, slots y operaciones públicas versionadas. No puede importar
beans, XHTML, entidades, repositorios o adaptadores internos.

### 5. Habilitadores antes del primer plugin

No se inicia `business_partners` hasta:

1. completar la validación independiente de `J11-S4-08` antes de promoción; los
   habilitadores técnicos pueden avanzar conforme a ADR-0012;
2. definir y probar descubrimiento/orden de migraciones `plg_*` en el migrador;
3. crear la plantilla mínima de plugin productivo sin copiar fixtures;
4. aplicar el contrato de eventos/outbox de ADR-0013 antes del primer intercambio
   asíncrono;
5. documentar la épica y los criterios del Sprint correspondiente.

### 6. Capacidades fuera de la primera secuencia

Nómina, recursos humanos, estaciones de servicio, producción, CRM, POS y analítica transversal no forman
parte de los doce plugins iniciales. Podrán incorporarse mediante nuevas épicas y
ADR cuando tengan alcance, propietario y dependencias claras.

Los reportes propios de un dominio pertenecen inicialmente a su plugin. Un futuro
plugin analítico cruzado deberá usar proyecciones/eventos y nunca joins sobre
tablas privadas.

ADR-0036 planifica fuera de esta secuencia `customer_support`,
`release_management` y `support_connector`. Los dos primeros pertenecen a la
instancia central del proveedor; el tercero es técnico, opcional y vive en la
instalación cliente. No constituyen un CRM general, no cambian el orden 1–19 y no
se implementan durante Sprint 8.

## Consecuencias

### Positivas

- cada dominio tiene propietario y esquema identificables;
- se reducen dependencias circulares y modelos provisionales;
- documentos y SIFEN quedan separados;
- contabilidad no dirige reglas operativas;
- la personalización se implementa sobre contratos ya estabilizados;
- cada incremento puede terminar con demo visual y prueba aislada del plugin.

### Costes y riesgos

- doce plugins implican más contratos, migraciones y matrices de composición;
- una frontera mal definida puede requerir versionar contratos públicos;
- documentos, logística, inventario y finanzas necesitan eventos cuidadosamente
  diseñados;
- el roadmap completo no representa una única versión corta ni obliga a todas las
  empresas a contratar todo;
- la personalización puede descubrir tarde que falta un punto de extensión; en ese
  caso se versiona el contrato propietario, no se accede a internos.

## Alternativas descartadas

### Comenzar directamente por facturación

Se descarta porque facturación necesita participantes, conceptos, impuestos,
stock, ventas y logística estables, y produciría maestros provisionales.

### Un solo plugin comercial

Se descarta porque mezclaría catálogo, inventario, compras, ventas, documentos y
finanzas, dificultando activación por empresa y propiedad de datos.

### Integrar SIFEN dentro de documentos

Se descarta porque acoplaría el agregado canónico a una versión fiscal y dificultaría
otros países o la coexistencia de adaptadores.

### Construir primero la personalización

Se descarta porque sus pantallas objetivo y rangos de versión todavía no existirían
o cambiarían durante la implementación funcional.

## Verificación obligatoria por plugin

Cada épica o Sprint de plugin deberá:

1. caracterizar requisitos sin copiar mecánicamente el legado;
2. aprobar dominio, esquema, contratos y dependencias antes del código;
3. demostrar que el kernel y plugins anteriores no dependen de su implementación;
4. ejecutar unitarias, ArchUnit, PostgreSQL/Testcontainers, WAR presente/ausente,
   Docker/Compose, seguridad y Playwright cuando aplique;
5. probar activación/desactivación por empresa sin pérdida de datos;
6. publicar pantallas responsive Material Design 3 sobre JSF;
7. terminar con una demo visual navegable;
8. actualizar la guía para implementadores y el PDF obligatorio de cierre.

## Referencias

- [ADR-0002 - Arquitectura de plugins](0002-arquitectura-plugins.md)
- [ADR-0005 - Activación y personalización obligatoria](0005-contexto-empresarial-activacion-personalizacion.md)
- [ADR-0010 - Documentos canónicos y SIFEN](0010-modelo-canonico-documentos-referencia-sifen.md)
- [ADR-0012 - Composición física única y migraciones de plugins](0012-composicion-unica-y-migraciones-de-plugins.md)
- [ADR-0013 - Eventos de integración y outbox por plugin](0013-eventos-integracion-outbox-por-plugin.md)
- [ADR-0027 - Terminal de punto de venta y ampliación del roadmap](0027-terminal-punto-venta-y-ampliacion-roadmap.md)
- [ADR-0030 - Familia de recursos humanos, nómina y cumplimiento paraguayo](0030-familia-recursos-humanos-nomina-paraguay.md)
- [ADR-0032 - Plugin para estaciones de servicio de combustible](0032-plugin-estaciones-servicio-combustible.md)
- [ADR-0033 - Dominio independiente de facturación recurrente](0033-dominio-facturacion-recurrente.md)
- [ADR-0034 - Plugin de telemetría vehicular y seguimiento GPS](0034-plugin-telemetria-vehicular.md)
- [ADR-0036 - Operaciones del proveedor, soporte y conector seguro](0036-operaciones-proveedor-soporte-lanzamientos-conector.md)
- [ADR-0037 — Familia para cooperativas de ahorro y crédito](0037-familia-cooperativa-ahorro-credito-paraguay.md)
- [Épica del roadmap de plugins](../backlog/epica-roadmap-plugins-productivos.md)
