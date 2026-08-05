# Épica — Planes, prorrateo y consumo medido

- Estado: Planificada como plugin funcional `recurring_billing`
- Fecha: 2026-08-02
- Orden vigente: 9, después de `commercial_documents` y antes de `sifen`
- Decisión: [ADR-0033](../adr/0033-dominio-facturacion-recurrente.md)
- Análisis: [frontera del dominio](../knowledge-base/commercial-documents/recurring-billing-domain-analysis.md)
- Prioridad: futura; no modifica el trabajo activo de Sprint 8

## Objetivo

Administrar planes y suscripciones, calcular prorrateos y transformar consumo
medido en cargos reproducibles que `commercial_documents` pueda emitir de forma
masiva sin compartir entidades ni ciclos de vida.

## Recorridos principales

### Configurar y contratar un plan

1. crear una versión de plan con vigencia, cadencia, componentes y política;
2. asociar productos, unidades y precios mediante referencias públicas;
3. activar una suscripción para un participante y fecha efectivas;
4. congelar la versión aplicada cuando comienza su uso;
5. programar el primer ciclo sin crear todavía una factura.

### Cambiar una suscripción con prorrateo

1. registrar el cambio efectivo sin sobrescribir la configuración anterior;
2. dividir el ciclo en segmentos no solapados;
3. aplicar la base temporal y redondeo versionados;
4. mostrar cálculo y trazabilidad antes de cerrar;
5. producir cargos de débito/crédito según la política aprobada.

### Facturar consumo

1. recibir uso con clave idempotente, unidad, intervalo y versión de fuente;
2. validar compatibilidad, duplicados y correcciones;
3. cerrar el corte y agregar cantidades elegibles;
4. tarificar y congelar cargos;
5. publicar candidatos al lote de `commercial_documents`;
6. conservar el vínculo por IDs hasta documento y ajuste resultantes.

## Historias propuestas

| Historia | Resultado |
|---|---|
| RB-01 | caracterización, decisiones RB-D01–RB-D10 y glosario temporal/monetario |
| RB-02 | módulos `recurring-billing-api`/`recurring-billing`, descriptor y dominio |
| RB-03 | persistencia privada de planes, suscripciones, vigencias y ciclos |
| RB-04 | prorrateo determinista, precisión y matriz calendario/aniversario |
| RB-05 | ingestión idempotente, correcciones y agregación de consumo |
| RB-06 | corrida de cálculo, cargos, aprobación, scheduler y recuperación |
| RB-07 | integración genérica con lotes de `commercial_documents` |
| RB-08 | UI responsive de planes, suscripciones, uso, cálculo y errores |
| RB-09 | composición, seguridad, carga, demo, manuales y PDF |

## Criterios de aceptación

- **RB-CE01:** una versión de plan usada permanece inmutable y los cambios tienen
  vigencia explícita.
- **RB-CE02:** segmentos de suscripción no se solapan ni dejan intervalos ambiguos.
- **RB-CE03:** prorrateos de 28/29/30/31 días son reproducibles con la política
  congelada.
- **RB-CE04:** repetir o reordenar consumo no duplica cantidades ni cargos.
- **RB-CE05:** una corrección conserva el registro original y explica su efecto.
- **RB-CE06:** la misma corrida reanudada no publica dos candidatos equivalentes.
- **RB-CE07:** una factura emitida nunca se modifica por consumo tardío; se genera
  un ajuste autorizado.
- **RB-CE08:** `commercial_documents` puede operar lotes de otros orígenes con
  `recurring_billing` ausente o inactivo.
- **RB-CE09:** producto, participante, documento y medidor sectorial se referencian
  solo mediante IDs/contratos; ArchUnit verifica la frontera.
- **RB-CE10:** desactivar el plugin no borra planes, consumo, cargos ni evidencia.
- **RB-CE11:** permisos separan administración, ingestión, cálculo, aprobación y
  publicación.
- **RB-CE12:** logs no contienen datos de cliente, lecturas detalladas ni payloads
  crudos innecesarios.
- **RB-CE13:** UI funciona en 375, 720 y 1280 px sin overflow horizontal normal.
- **RB-CE14:** Playwright cubre vacío, alta, cambio prorrateado, duplicado,
  corrección tardía, corrida parcial, acceso denegado y recuperación.
- **RB-CE15:** carga declarada demuestra memoria acotada y procesamiento por
  bloques, sin depender de una sesión web abierta.

## Contratos públicos previstos

`recurring-billing-api` será Java puro y expondrá contratos mínimos:

- `BillingPlanId`, `SubscriptionId`, `BillingCycleId`, `UsageRecordId`,
  `RatedChargeId` y `ChargeGenerationRunId`;
- comandos versionados para alta/cambio de suscripción e ingestión de uso;
- consulta pública de referencia y estado por empresa;
- candidato inmutable compatible con `commercial-documents-api`;
- eventos pasados de suscripción, uso aceptado/corregido, ciclo cerrado y cargos
  publicados.

No expondrá entidades JPA, expresiones arbitrarias, payloads sectoriales, facturas
ni datos fiscales completos.

## Permisos previstos

- `recurring_billing.view`;
- `recurring_billing.plans.manage`;
- `recurring_billing.subscriptions.manage`;
- `recurring_billing.usage.ingest`;
- `recurring_billing.runs.prepare`;
- `recurring_billing.runs.approve`;
- `recurring_billing.runs.execute`;
- `recurring_billing.runs.retry`;
- `recurring_billing.adjustments.approve`;
- `recurring_billing.integrations.manage`.

## Gates

- JUnit con reloj controlado, calendarios, precisión y propiedades de prorrateo;
- PostgreSQL/Testcontainers para vigencias, constraints, concurrencia,
  idempotencia y reinicio;
- JPA/JTA para cierre/publicación y outbox;
- ArchUnit y composición con origen opcional presente/ausente;
- carga para consumo y corridas con volúmenes declarados;
- Docker/Compose, health, scheduler, métricas, cuarentena y recuperación;
- Playwright responsive y seguridad negativa;
- demo visual, fotografía de plugins, manuales y PDF de cierre.

## Fuera de alcance inicial

- factura, nota, numeración, XML, CDC o transmisión fiscal;
- cobro, saldo, deuda o asiento;
- control de medidores o almacenamiento de telemetría cruda;
- fórmulas ejecutables arbitrarias cargadas por usuario;
- tarifas escalonadas, bolsas, mínimos, tiempo real o pagadores múltiples;
- implementación durante Sprint 8.

## Orden y autorización

ADR-0033 incorporó históricamente esta épica como orden 8. ADR-0034 insertó
después `vehicle_telemetry` como orden 7; el roadmap vigente contiene diecinueve
reutilizables y `recurring_billing` ocupa el orden 9. No se inicia hasta cerrar
Sprint 8 y completar los plugins 4–8.
