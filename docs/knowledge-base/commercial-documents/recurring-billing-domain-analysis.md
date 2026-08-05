# Facturación recurrente: análisis del dominio independiente

- Fecha: 2026-08-02
- Estado: decisión incorporada al roadmap; implementación no iniciada
- Decisión de producto: incluir planes recurrentes, prorrateo y consumo medido
  como dominio independiente
- Fuentes legadas: Multienvíos e Ingenio La Felsina, solo lectura

## Objetivo

Separar el cálculo de qué corresponde cobrar en un ciclo de la creación de la
factura. El dominio debe poder reproducir planes, cambios parciales y consumo sin
poseer numeración, documentos fiscales, cobros ni deuda.

## Evidencia y brecha del legado

La caracterización previa encontró selección por período, agrupación y generación
de prefacturas/facturas. No encontró un agregado reusable que mantenga versiones
de planes, suscripciones, calendarios, segmentos prorrateados, lecturas corregibles
o cálculo determinista de cargos.

El legado permite entender la necesidad de una corrida y de candidatos
revisables, pero no es una implementación trasladable de facturación recurrente.
La decisión de producto aporta ahora el dominio autónomo que ADR-0031 había dejado
como condición futura.

## Frontera aprobada

### `recurring_billing` posee

- planes y versiones con vigencia;
- suscripciones y sus ítems facturables;
- calendario, ciclo y período de servicio;
- altas, bajas, suspensiones y cambios efectivos dentro del período;
- reglas y segmentos de prorrateo;
- registro idempotente de consumo facturable y sus correcciones;
- agregación, tarificación y cargos calculados reproducibles;
- corrida de cálculo, revisión y publicación de candidatos de factura.

### Otros propietarios conservan

| Capacidad | Propietario |
|---|---|
| cliente/receptor y direcciones | `business_partners` |
| producto, unidad, precio base e impuesto | `commercial_catalog` |
| pedido o contratación comercial de origen | `sales` |
| factura, numeración, snapshots fiscales y lote de emisión | `commercial_documents` |
| XML, firma, CDC, envío y respuesta | `sifen` |
| caja, cobro y conciliación financiera | `treasury` |
| deuda, cuota, vencimiento y cobranza | `accounts_receivable` |
| medidor físico y telemetría sectorial | plugin operativo que los origine |

`recurring_billing` conserva únicamente el registro de uso aceptado para cobrar,
con ID y versión de la fuente. No controla medidores ni lee tablas operativas.

## Dos corridas diferentes

```text
plan + suscripción + uso
          |
          v
corrida de cálculo de cargos (`recurring_billing`)
          |
          v
candidatos versionados e inmutables
          |
          v
lote de generación de facturas (`commercial_documents`)
          |
          v
facturas individuales -> proyección fiscal -> lotes SIFEN
```

La primera corrida determina importes facturables. La segunda valida y emite
documentos. Tienen IDs, permisos, estados, reintentos e idempotencia separados.

## Modelo conceptual

- `billing_plan` y `billing_plan_version`: cadencia, momento de cobro, regla de
  prorrateo y componentes vigentes;
- `subscription`: cliente público, estado, inicio, fin y zona horaria;
- `subscription_item`: plan/producto, cantidad, vigencia y configuración;
- `billing_cycle`: intervalo de servicio y fecha de corte;
- `subscription_change`: alta, suspensión, reanudación, cambio o baja efectiva;
- `billable_usage_record`: fuente, clave idempotente, cantidad decimal, unidad,
  intervalo de consumo, instante observado/recibido y versión;
- `usage_correction`: reemplazo o delta explícito sin borrar el registro original;
- `proration_segment`: límites, días/unidades de tiempo y factor reproducible;
- `rated_charge`: concepto, cantidad, precio, importe, moneda y snapshots;
- `charge_generation_run`: corte, revisión, estado, conteos y resultado;
- `invoice_candidate`: proyección inmutable entregada al contrato público de
  documentos.

## Reglas e invariantes

1. una versión de plan utilizada no se reescribe; un cambio crea otra vigencia;
2. cada intervalo de una suscripción queda cubierto por una única versión activa;
3. el método de prorrateo se declara y congela: días calendario, segundos reales u
   otra base aprobada; no se infiere desde la UI;
4. el mismo conjunto de entradas, versiones y política produce los mismos cargos;
5. dinero, cantidades y factores usan decimales explícitos, nunca `double`;
6. redondeo, moneda y zona horaria quedan en el snapshot de cálculo;
7. consumo se deduplica por empresa, fuente, medidor/referencia, clave y versión;
8. una corrección tardía no modifica una factura emitida: genera un cargo o ajuste
   candidato para el ciclo permitido;
9. un cargo publicado no vuelve a estado mutable;
10. emitir o rechazar una factura no borra plan, consumo ni cálculo de origen.

## Alcance inicial propuesto

- cargos fijos por adelantado o vencidos;
- ciclos mensuales calendario y por aniversario;
- alta, baja, suspensión, reanudación y cambio de plan con prorrateo;
- consumo medido simple con unidad compatible, tarifa plana y correcciones;
- corrida manual o programada con vista previa y aprobación;
- candidatos agrupables por cliente, suscripción, moneda y regla explícita;
- integración idempotente con `commercial_documents`.

Quedan para incrementos posteriores tarifas escalonadas, mínimos comprometidos,
bolsas acumulables, descuentos complejos, rating en tiempo real, roaming, reparto
entre pagadores y protocolos de medidores.

## Decisiones

| Código | Decisión |
|---|---|
| RB-D01 | Crear `recurring_billing` como plugin funcional reutilizable independiente. |
| RB-D02 | ADR-0033 lo ubicó históricamente en el orden 8; ADR-0034 desplaza su orden vigente a 9, después de `commercial_documents` y antes de `sifen`. |
| RB-D03 | Mantener separados corrida de cargos, lote comercial y lote fiscal. |
| RB-D04 | Planes y versiones son inmutables una vez usados; los cambios tienen vigencia. |
| RB-D05 | Prorrateo declara base temporal, inclusividad, zona y redondeo reproducibles. |
| RB-D06 | El plugin recibe consumo por contrato idempotente; no posee hardware sectorial. |
| RB-D07 | Correcciones tardías generan ajustes futuros, nunca mutación de facturas emitidas. |
| RB-D08 | `commercial_documents` conserva factura, numeración e impuestos documentales finales. |
| RB-D09 | El plugin puede desactivarse sin impedir lotes masivos de otros orígenes. |
| RB-D10 | La primera versión limita tarifas y agrupaciones antes de añadir rating avanzado. |

## Pruebas futuras

- prorrateo de alta/baja/cambio en meses de 28, 29, 30 y 31 días;
- límites inclusivos/exclusivos y cambios de zona horaria;
- repetición y corrección de consumo recibido fuera de orden;
- corrida repetida/reanudada sin duplicar cargos ni candidatos;
- cambio de plan o precio sin reescribir ciclos cerrados;
- corrección posterior a emisión convertida en candidato de ajuste;
- aislamiento empresarial, permisos y ausencia de JPA/SQL cruzado;
- `recurring_billing` presente con documentos activo/inactivo y facturación masiva
  de otros orígenes sin el plugin recurrente.

## Límites

Este análisis no crea módulos, scheduler, migraciones ni pantallas. No autoriza
adelantar `recurring_billing` durante Sprint 8 ni antes de completar los plugins
4–8.
