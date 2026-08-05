# Facturación masiva: caracterización del legado y frontera propuesta

- Fecha de análisis: 2026-08-02
- Estado: decisiones incorporadas al backlog; implementación no iniciada
- Fuente principal: `C:\cosme\multienvios\miaterra`, commit
  `55a56963f00329edd2da57b53a1a94da129cc819`
- Fuente complementaria: `C:\cosme\felsina\ingeniolafelsina`, commit
  `412b3cd978757b1b8a389f2007060a90f5c7322b`
- Tratamiento de las fuentes: solo lectura; no se copiaron clases, consultas ni
  pantallas

## Objetivo

Caracterizar el caso en que una empresa prepara muchas facturas de un período y
las emite en una fecha común. El análisis distingue la selección comercial, la
preparación de borradores, la emisión de documentos canónicos y el envío fiscal
por lotes. Son procesos relacionados, pero no son el mismo lote ni comparten ciclo
de vida.

## Comportamiento observado en Multienvíos

La pantalla `TswGenerarPreFacturas.xhtml` y sus controladores permiten:

1. filtrar actividades pendientes por mes o por rango de fechas;
2. construir un identificador de período;
3. agrupar equipos por cliente o grupo de facturación;
4. revisar y editar cliente, cantidad y precio antes de confirmar;
5. transformar actividades seleccionadas en pedidos usados como prefacturas;
6. seleccionar prefacturas, una serie, condición de venta y fecha común;
7. recorrer la selección y crear una factura por prefactura;
8. mostrar cantidades generadas y no generadas;
9. anular prefacturas para habilitar nuevamente su origen.

El comportamiento confirma necesidades útiles: período de facturación separado de
la fecha de emisión, vista previa, agrupación configurable, selección parcial,
valores por defecto empresariales, corrección previa, resultado individual y
recuperación del origen cuando se descarta un borrador.

## Comportamiento observado en Ingenio La Felsina

`RhwPlanillaSalarioControlador` puede convertir una planilla salarial en un
comprobante de compra usando proveedor, artículo, moneda, categoría y cuenta
configurados. El controlador de recursos humanos prepara datos y reutiliza
directamente un controlador de compras para persistir el comprobante.

No es el mismo caso que la facturación masiva de clientes, pero demuestra que más
de un dominio puede originar documentos. En Logixone, nómina o cualquier otro
plugin deberá entregar una solicitud tipada al contrato público del propietario
del documento; no importará controladores, entidades ni tablas privadas.

## Riesgos del diseño legado que no deben trasladarse

| Riesgo observado | Decisión para Logixone |
|---|---|
| la vista recorre registros y reutiliza un controlador de factura por cada fila | el lote es un agregado persistente y la ejecución ocurre en servicios de aplicación recuperables |
| numeración obtenida mediante `MAX + 1` | asignación atómica y concurrente por establecimiento, punto, documento y autorización aplicable |
| tasa 10 % y división por 11 embebidas | cálculo desde reglas y snapshots tributarios vigentes, nunca constantes de la pantalla |
| consultas construidas concatenando empresa y fechas | parámetros tipados y ámbito empresarial obligatorio |
| acoplamiento entre tesorería, ventas, clientes, monitor y compras | IDs y contratos públicos; ninguna relación JPA ni consulta de esquema ajeno |
| una excepción puede coexistir con el incremento del contador de generadas | resultado persistente por ítem, confirmado únicamente con el documento realmente creado |
| proceso completo en memoria y dentro de una interacción web | trabajo fraccionado, reanudable y observable; la UI consulta progreso |
| no existe identidad idempotente explícita por origen/período | restricción única empresarial que impide facturar dos veces el mismo origen y período |
| anulación de prefactura muta varios dominios acoplados | cancelar un borrador solo libera su reserva lógica; una factura emitida se corrige mediante el ciclo documental autorizado |
| fecha de período, fecha de emisión y fecha técnica se confunden | campos y validaciones separadas, con zona horaria empresarial explícita |

## Decisiones de producto y arquitectura

| Código | Decisión |
|---|---|
| FM-D01 | `commercial_documents` será dueño del lote de generación y de cada factura canónica. |
| FM-D02 | No se crea un plugin `bulk_billing` inicial: dividiría el propietario del documento sin aportar un dominio autónomo. |
| FM-D03 | Los plugins de origen entregan candidatos inmutables mediante un contrato público; `commercial_documents` no lee sus tablas. |
| FM-D04 | El lote se prepara, prevalida, congela, aprueba y ejecuta; no se emite directamente desde una búsqueda cambiante. |
| FM-D05 | Cada ítem usa una transacción corta, resultado propio e idempotencia por empresa, origen, versión y período. |
| FM-D06 | Un fallo parcial no revierte facturas ya emitidas ni obliga a repetirlas; se reintentan únicamente ítems elegibles. |
| FM-D07 | Período, corte, fecha comercial de emisión, instante de firma y transmisión fiscal son conceptos distintos. |
| FM-D08 | La numeración se reserva de forma atómica al emitir; queda prohibido `MAX + 1`. |
| FM-D09 | El lote comercial y los lotes SIFEN tienen identidades, límites, estados y reintentos separados. |
| FM-D10 | `recurring_billing` requiere otro ADR y se justifica solo por planes, calendarios, prorrateos o consumo medido reutilizables; ADR-0033 confirmó posteriormente esa condición. |

## Flujo canónico propuesto

```text
fuente facturable -> preparar lote -> prevalidar -> congelar candidatos
                  -> aprobar -> ejecutar por ítem -> factura canónica emitida
                  -> evento/outbox -> proyección fiscal -> lotes SIFEN
```

Estados mínimos del lote comercial:

- `DRAFT`: criterios y candidatos todavía modificables;
- `VALIDATING`: comprobaciones empresariales y documentales en curso;
- `READY`: snapshot completo sin errores bloqueantes;
- `APPROVED`: autorizado para ejecutar;
- `RUNNING`: ítems reclamados en bloques pequeños;
- `PARTIALLY_COMPLETED`: existen éxitos y fallos recuperables o definitivos;
- `COMPLETED`: todos los ítems elegibles tienen resultado definitivo;
- `CANCELLED`: cancelado antes de emitir nuevos documentos.

Cada ítem conserva estado, origen público, versión del origen, período, clave de
idempotencia, snapshot de entrada, documento resultante, intentos y código de
resultado. Los errores visibles no incluyen datos personales innecesarios.

## Tablas conceptuales del propietario

Los nombres finales se aprobarán con el diseño de `commercial_documents`, pero el
esquema privado necesitará equivalentes relacionales de:

- `invoice_generation_run` para criterios, fecha común, estado y totales;
- `invoice_generation_item` para candidato, idempotencia y resultado;
- `invoice_generation_attempt` para intentos, clasificación del fallo y tiempos;
- `invoice_generation_event` para transiciones append-only;
- referencia por ID a la factura canónica creada, sin duplicar su agregado.

No se guardará el lote como un único JSON operativo ni se abrirá una transacción
para miles de facturas. Los trabajadores reclamarán ítems mediante versión
optimista o bloqueo controlado y podrán reanudar el proceso después de un reinicio.

## Frontera con SIFEN verificada

El portal oficial consultado el 2026-08-02 mantiene el Manual Técnico versión 150
y publica notas técnicas acumulativas, incluida la Nota Técnica 27 del 2026-03-09.
La guía oficial de mejores prácticas de octubre de 2024 indica que el servicio
asíncrono recibe lotes de hasta 50 DE, del mismo RUC emisor y tipo documental, con
un mensaje comprimido de hasta 1.000 KB. El resultado se consulta después y cada
CDC obtiene su propio estado.

Por eso `sifen` toma facturas ya emitidas y forma sus propios lotes técnicos. No
reutiliza `invoice_generation_run`, no vuelve a calcular el documento y nunca
reenvía un CDC mientras su resultado no sea definitivo.

Fuentes oficiales verificadas:

- [Documentación técnica e-Kuatia](https://ekuatia.set.gov.py/web/e-kuatia/documentacion-tecnica);
- [Guía de mejores prácticas para la gestión del envío de DE](https://ekuatia.set.gov.py/documents/20123/420592/Gu%C3%ADa%2Bde%2BMejores%2BPr%C3%A1cticas%2Bpara%2Bla%2BGesti%C3%B3n%2Bdel%2BEnv%C3%ADo%2Bde%2BDE.pdf/38fe5830-98c0-2241-9895-671f86f1225f?t=1729866823709);
- [Nota Técnica 27 del Manual Técnico 150](https://ekuatia.set.gov.py/documents/20123/420595/NT_E_KUATIA_027_MT_V150.pdf/e5376c97-64cf-3fe0-e962-b6f22c8c207a?t=1773076266295).

Esta consulta confirma el enfoque, pero no certifica una futura implementación.
La historia fiscal deberá descargar en `.tools/`, verificar checksum y congelar
manual, notas, XSD, catálogos y WSDL aplicables a su fecha de ejecución.

## Pruebas de caracterización futuras

1. dos intentos con la misma clave empresarial producen una sola factura;
2. dos trabajadores no reclaman ni numeran dos veces el mismo ítem;
3. un fallo en el ítem N no revierte los N-1 confirmados;
4. un reinicio permite reanudar sin repetir documentos;
5. cambiar un maestro después de congelar no altera el snapshot aprobado;
6. una fecha común inválida, autorización vencida o período cerrado bloquean antes
   de numerar;
7. reintentar un error definitivo requiere una nueva decisión autorizada;
8. una factura emitida publica exactamente un hecho idempotente hacia `sifen` y
   cuentas por cobrar;
9. los lotes SIFEN respetan 50 DE, RUC, tipo, tamaño y consulta diferida;
10. empresa, permisos, auditoría y separación de esquemas se verifican en cada
    comando y consulta.

## Límites

Este documento no implementa módulos, tablas, scheduler ni integración fiscal. No
autoriza comenzar `commercial_documents` antes de completar los plugins y gates
precedentes del roadmap.

## Ampliación confirmada

Producto confirmó el 2026-08-02 planes recurrentes, prorrateo y consumo medido
como dominio independiente. [ADR-0033](../../adr/0033-dominio-facturacion-recurrente.md)
agrega `recurring_billing`; el
[análisis específico](recurring-billing-domain-analysis.md) define su frontera sin
alterar los hallazgos históricos de este documento.
