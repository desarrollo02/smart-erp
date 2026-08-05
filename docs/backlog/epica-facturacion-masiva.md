# Épica — Facturación masiva, recuperable e idempotente

- Estado: Planificada entre `recurring_billing` y `commercial_documents`
- Fecha: 2026-08-02
- Prioridad: se implementa con `commercial_documents`, después de estabilizar
  `sales` y `logistics`; no modifica el trabajo autorizado de Sprint 8
- Decisiones: [ADR-0031](../adr/0031-facturacion-masiva-en-documentos-comerciales.md) y [ADR-0033](../adr/0033-dominio-facturacion-recurrente.md)
- Conocimiento: [caracterización del legado](../knowledge-base/commercial-documents/facturacion-masiva-legacy-characterization.md)

## Objetivo

Permitir que una empresa prepare, revise, apruebe y emita de forma segura cientos o
miles de facturas correspondientes a un período y una fecha común, sin duplicar
documentos, bloquear una única transacción ni perder trazabilidad cuando algunos
ítems fallen.

## Usuarios y escenarios

- administrador de facturación mensual de servicios;
- operador que prepara candidatos y corrige datos antes de emitir;
- responsable que aprueba una corrida con fecha y totales conocidos;
- supervisor que monitorea, reintenta u omite errores autorizados;
- soporte que reconstruye el estado después de una caída sin repetir facturas;
- integración fiscal que agrupa las facturas emitidas en lotes SIFEN separados.

Escenarios iniciales:

1. facturar servicios o contratos de un período mensual;
2. facturar una selección manual obtenida desde un plugin de origen;
3. programar la ejecución de un lote ya congelado y aprobado;
4. reanudar una corrida interrumpida;
5. resolver resultados parciales y exportar evidencia;
6. emitir nuevamente solo cuando una nueva versión del origen y el ciclo
   documental lo permitan;
7. generar cargos periódicos desde planes y suscripciones;
8. calcular altas, bajas y cambios prorrateados;
9. agregar consumo medido, aceptar correcciones y emitir ajustes trazables.

Los tres últimos escenarios pertenecen al plugin independiente
`recurring_billing`. Sus historias, modelo y gates se detallan en la
[épica específica](epica-facturacion-recurrente.md).

## Historias propuestas

| Historia | Resultado |
|---|---|
| FM-01 | caracterización, ADR, vocabulario y separación lote comercial/SIFEN |
| FM-02 | contrato público de preparación y dominio del lote/ítem |
| FM-03 | migraciones privadas, idempotencia y asignación atómica de numeración |
| FM-04 | prevalidación, congelación, aprobación, ejecución fraccionada y recuperación |
| FM-05 | permisos, auditoría, outbox, métricas y cuarentena operativa |
| FM-06 | UI JSF Material Design 3 para preparar, aprobar, seguir y resolver el lote |
| FM-07 | primera fuente real desde `sales`, sin acceso a tablas cruzadas |
| FM-08 | proyección hacia `sifen` y transmisión asíncrona en lotes técnicos separados |
| FM-09 | pruebas integrales, demo responsive, manuales, PDF y evidencia de cierre |

Las historias FM siguen siendo propiedad de `commercial_documents`. Las historias
RB calculan cargos y publican candidatos; no duplican el lote ni la factura.

## Modelo mínimo esperado

### Lote comercial

- `InvoiceGenerationRunId` UUID, empresa y versión optimista;
- período, corte, zona horaria y fecha comercial propuesta;
- fuente o conjunto cerrado de fuentes compatibles;
- estado, revisión, creador, aprobador e instantes relevantes;
- conteos y totales por moneda derivados de sus ítems;
- transiciones append-only.

### Ítem

- identidad y versión pública del origen;
- clave de idempotencia empresarial;
- snapshot congelado del candidato;
- receptor, moneda y documento resultante por IDs públicos;
- estado, intentos, clasificación y código seguro de resultado;
- importe de control para conciliación del lote.

El snapshot no sustituye las tablas relacionales del documento. La factura emitida
se conserva en el agregado canónico de `commercial_documents`.

## Reglas funcionales

1. consultar candidatos no los reserva ni factura;
2. congelar fija exactamente qué se aprobará;
3. aprobar no asigna aún todos los números ni garantiza éxito fiscal;
4. cada ítem se emite una sola vez para su clave idempotente;
5. un fallo parcial no repite ni revierte éxitos confirmados;
6. solo errores clasificados como reintentables pueden ejecutarse sin cambiar la
   revisión;
7. una factura emitida no vuelve a borrador;
8. estados comercial, de cobranza y fiscal evolucionan por separado;
9. desactivar el plugin conserva lotes, documentos, outbox y evidencia;
10. una empresa nunca observa ni opera lotes de otra.

## Criterios de aceptación

- **FM-CE01:** una solicitud repetida con la misma clave devuelve el mismo
  resultado y no crea otra factura.
- **FM-CE02:** dos ejecutores concurrentes no procesan dos veces un ítem ni asignan
  el mismo número.
- **FM-CE03:** 1.000 candidatos se procesan en bloques acotados, sin transacción ni
  sesión web de duración equivalente al lote completo.
- **FM-CE04:** caída y reinicio reanudan pendientes sin repetir éxitos.
- **FM-CE05:** fecha, autorización, receptor, moneda, impuestos y totales se
  prevalidadan antes de aprobar.
- **FM-CE06:** un cambio posterior de maestros no altera snapshots aprobados ni
  facturas emitidas.
- **FM-CE07:** éxito parcial muestra conteos exactos y detalle paginado por código
  de resultado.
- **FM-CE08:** reintento, omisión y cancelación requieren permiso y auditoría.
- **FM-CE09:** el plugin de origen se integra solo por API pública, IDs y versión;
  ArchUnit demuestra ausencia de entidades y JPA cruzados.
- **FM-CE10:** el outbox se confirma junto con la factura y el consumidor deduplica
  por `event_id`.
- **FM-CE11:** `sifen` crea lotes independientes que respetan RUC, tipo, cantidad,
  tamaño, protocolo y consulta diferida vigentes.
- **FM-CE12:** compacto, medio y expandido permiten preparar, aprobar y consultar
  sin overflow horizontal normal.
- **FM-CE13:** Playwright cubre vacío, prevalidación fallida, ejecución parcial,
  reintento, acceso denegado y recuperación.
- **FM-CE14:** manual de usuario, guía de implementación, manual técnico,
  fotografía de plugins y PDF explican límites y operación.

## Seguridad y auditoría

Permisos previstos:

- `commercial_documents.invoice_batches.view`;
- `commercial_documents.invoice_batches.prepare`;
- `commercial_documents.invoice_batches.approve`;
- `commercial_documents.invoice_batches.execute`;
- `commercial_documents.invoice_batches.retry`;
- `commercial_documents.invoice_batches.cancel`.

La auditoría usa IDs, revisión, conteos, operación y código de resultado. RUC,
nombre, dirección, líneas, XML, certificados y secretos no se registran en logs.

## Gates técnicos

- dominio Java puro y pruebas unitarias;
- PostgreSQL/Testcontainers para constraints, locks, idempotencia y reinicio;
- JPA/JTA para transacción por ítem y outbox;
- ArchUnit para límites entre origen, documentos y SIFEN;
- pruebas de carga con volumen y memoria declarados;
- Docker/Compose, health, métricas y recuperación;
- Playwright responsive y seguridad negativa;
- matriz SIFEN contra manual, notas, XSD, WSDL y catálogos congelados por checksum.

## Dependencias y orden

La emisión masiva se desarrolla dentro del orden 8, `commercial_documents`.
ADR-0034 inserta antes `vehicle_telemetry` como orden 7. `recurring_billing` ocupa
el orden 9 para planes, prorrateo y consumo; `sifen` pasa al orden 10 y consume
solamente la proyección fiscal pública.

No se inicia esta épica hasta completar `purchasing`, `sales`, `logistics`,
`vehicle_telemetry` y los gates anteriores. Documentos no depende funcionalmente
de telemetría; la precedencia corresponde al orden de construcción aprobado. La
documentación actual no autoriza adelantarla durante Sprint 8.
