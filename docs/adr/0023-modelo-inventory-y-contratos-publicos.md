# ADR-0023 - Modelo de `inventory` y contratos públicos

- Estado: Aceptado
- Fecha: 2026-07-31
- Historia: `J11-S8-02`
- Decisiones de producto: IN-D01 a IN-D10 confirmadas el 2026-07-31

## Contexto

El legado mezcla existencias con catálogo, compras, ventas, remisiones, costos,
vehículos y contabilidad. También modifica saldos e historial directamente, usa
reservas sin ciclo completo y reemplaza el saldo al cerrar un inventario físico.
Copiar ese modelo trasladaría acoplamiento, pérdida de trazabilidad y reglas
ambiguas al nuevo ERP.

`inventory` debe ser el tercer plugin productivo. Necesita consumir la identidad y
conversión pública de un producto de `commercial_catalog`, pero no puede importar
su dominio, entidades, repositorios o tablas. En este corte todavía no corresponde
elegir esquema, JPA, permisos, pantallas ni composición de distribución.

## Decisión

1. Se crean dos módulos físicos: `inventory-api` para contratos Java puros e
   `inventory` para descriptor y dominio privado.
2. El descriptor publica `inventory@1.0.0`, tipo `FUNCTIONAL`, compatibilidad con
   `plugin-api` `[0.4.0,0.5.0)` y una dependencia funcional `REQUIRED` de
   `commercial_catalog` `[1.0.0,2.0.0)`.
3. `inventory-api@1.0.0` depende únicamente de Java estándar y `CompanyId`.
   Publica identidades opacas, disponibilidad, snapshot de conversión, comandos de
   movimientos y reservas por propósitos separados. No expone clases internas.
4. Cada depósito y ubicación son obligatorios. Crear un depósito crea una
   ubicación de sistema `GENERAL`, que no puede inactivarse ni duplicarse.
5. Un concepto inventariable es una inscripción local y explícita de un
   `CatalogItemReference` activo de tipo `PRODUCT`. El ID local no sustituye ni
   copia la identidad pública del catálogo.
6. La clave de stock contiene ítem inventariable, depósito, ubicación, condición y
   dimensiones opcionales de lote, serie y vencimiento. Las políticas son
   `NONE`/`LOT`/`SERIAL`, `NONE`/`OPTIONAL`/`REQUIRED` y
   `AVAILABLE`/`QUARANTINED`/`DAMAGED`.
7. Cantidad física, reservada y disponible se expresan en unidad base. La cantidad
   acepta hasta 6 decimales y el factor hasta 12; el movimiento conserva unidad y
   cantidad presentadas, unidad base, factor, cantidad base y versión del ítem.
   Una línea serializada representa exactamente una unidad.
8. Físico y disponible nunca pueden ser negativos. La disponibilidad es siempre
   `físico - reservado`; la concurrencia se protege mediante versión esperada.
9. El libro usa movimientos inmutables `RECEIPT`, `ISSUE`, `TRANSFER`,
   `ADJUSTMENT` y `REVERSAL`, siempre con motivo, fuente neutral e idempotencia.
   Una transferencia tiene débito y crédito atómicos por igual cantidad base; una
   corrección crea una reversión enlazada y no modifica el pasado.
10. La reserva conserva cantidad original, consumida, liberada y restante, fuente,
    vencimiento y estados `ACTIVE`, `PARTIALLY_CONSUMED`, `CONSUMED`, `RELEASED`
    o `EXPIRED`. Permite consumo y liberación parciales sin sobrepasar el remanente.
11. El conteo sigue `DRAFT -> COUNTING -> REVIEW -> POSTED` o `CANCELLED`, bloquea
    únicamente su alcance y produce diferencias explícitas que deberán convertirse
    en movimientos de ajuste; nunca reemplaza el saldo directamente.
12. La versión 1 no contiene importes, monedas, costos ni valoración. Tampoco se
    agrega evento u outbox hasta existir un consumidor real conforme a ADR-0013.

## Alternativas descartadas

- Usar sucursal como saldo o permitir ubicación nula: impide varios depósitos y
  propaga casos especiales; `GENERAL` mantiene una clave completa.
- Habilitar automáticamente todos los productos: mezcla política de inventario con
  el catálogo y no permite trazabilidad específica por empresa.
- Mantener un saldo editable como fuente principal: no explica entradas, salidas,
  transferencias, reservas o correcciones.
- Incluir costo en el movimiento inicial: acopla cantidades operativas con una
  política de valoración todavía no diseñada.
- Crear eventos preventivos: congela contratos sin productor y consumidor reales.

## Consecuencias

- `inventory` no puede registrarse en un catálogo físico si falta una versión 1.x
  compatible de `commercial_catalog`.
- La dependencia funcional del descriptor y la dependencia Maven del módulo son
  explícitas y tienen significados diferentes.
- J11-S8-03 podrá diseñar un esquema privado a partir de agregados e invariantes ya
  aceptados, sin cambiar los contratos por conveniencia de JPA.
- El descriptor queda sin capacidades, permisos, menús, migraciones o pantallas
  hasta sus historias propietarias.
- El WAR base y los perfiles actuales no incorporan aún los nuevos JAR; la
  composición física pertenece a J11-S8-06.

## Verificación

- pruebas unitarias de API, descriptor y agregados;
- validación de precisión, transferencias, negativos, reservas y conteos;
- ServiceLoader y CDI sobre una única definición;
- ArchUnit para API/dominio neutrales y prohibición de internos del catálogo;
- `mvn verify` del reactor completo.
