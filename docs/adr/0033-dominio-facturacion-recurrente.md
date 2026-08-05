# ADR-0033 — Dominio independiente de facturación recurrente

- Estado: Aceptado
- Fecha: 2026-08-02
- Decisión de producto: incluir planes recurrentes, prorrateo y consumo medido
  como dominio independiente de la facturación masiva
- Modifica: condición futura de ADR-0031 y cantidad/orden del roadmap

> Nota vigente: esta ADR incorporó históricamente `recurring_billing` como orden
> 8 dentro de dieciocho reutilizables.
> [ADR-0034](0034-plugin-telemetria-vehicular.md) insertó después
> `vehicle_telemetry` como orden 7; `recurring_billing` ocupa ahora el orden 9 y el
> roadmap contiene diecinueve reutilizables (`19 + N`).

## Contexto

ADR-0031 asignó a `commercial_documents` la preparación y ejecución de lotes de
facturas, y dejó un posible `recurring_billing` condicionado a la existencia de
planes, suscripciones, prorrateo o consumo medido reutilizables. Producto confirmó
ahora expresamente esas tres capacidades.

El cálculo de cargos tiene estado, vigencias, correcciones e idempotencia propios.
Introducirlo dentro del documento haría que la factura poseyera contratos y
mediciones anteriores a su existencia. Separar también la emisión en otro plugin
duplicaría numeración e invariantes documentales.

## Decisión

### 1. Plugin y orden

Se agrega `recurring_billing` como plugin funcional reutilizable número **8**,
después de `commercial_documents` y antes de `sifen`. El roadmap pasa a dieciocho
plugins reutilizables:

1. `business_partners`;
2. `commercial_catalog`;
3. `inventory`;
4. `purchasing`;
5. `sales`;
6. `logistics`;
7. `commercial_documents`;
8. `recurring_billing`;
9. `sifen`;
10. `treasury`;
11. `point_of_sale`;
12. `fuel_station`;
13. `accounts_receivable`;
14. `accounts_payable`;
15. `accounting`;
16. `human_resources`;
17. `payroll`;
18. `payroll_paraguay`.

Una distribución completa para `N` empresas podrá contener `18 + N` plugins
productivos. Cada empresa activa solamente los necesarios y mantiene exactamente
su personalización al final.

### 2. Propiedad

`recurring_billing` será dueño de planes/versiones, suscripciones, ciclos,
cambios efectivos, prorrateo, registro de uso facturable, correcciones,
tarificación, cargos y corridas de cálculo.

No será dueño de participante, producto maestro, pedido, factura, numeración,
impuesto documental final, XML/CDC, cobro, deuda, asiento o medidor físico.

### 3. Dependencias

- requeridas: contratos públicos de `business_partners`, `commercial_catalog` y
  `commercial_documents`;
- opcional: `sales`, para activar o modificar una suscripción desde una
  contratación comercial;
- fuentes sectoriales opcionales: publican consumo mediante el contrato neutral
  de ingestión, sin dependencia inversa ni acceso a tablas;
- sin dependencia directa de `sifen`, `treasury`, `accounts_receivable` o
  `accounting`.

`commercial_documents` no depende de `recurring_billing`: su API genérica recibe
candidatos de cualquier origen. Así la facturación masiva manual o proveniente de
ventas continúa funcionando cuando el plugin recurrente no está compuesto o está
desactivado.

### 4. Corridas separadas

La corrida de cargos cierra entradas y calcula candidatos. El lote de documentos
prevalida, aprueba y emite cada factura. SIFEN forma después lotes técnicos de
documentos ya emitidos. Ninguna identidad o estado se reutiliza entre las tres
corridas.

### 5. Temporalidad y prorrateo

Cada plan y suscripción usa vigencias explícitas. La política de prorrateo declara
base temporal, límites inclusivos/exclusivos, zona horaria, precisión y redondeo.
Un cambio de plan divide el período en segmentos reproducibles y no reescribe un
ciclo cerrado.

### 6. Consumo medido

El plugin recibe registros de uso con fuente, referencia pública, unidad,
intervalo, instante observado/recibido, cantidad decimal, versión y clave
idempotente. El equipo físico y la telemetría pertenecen al plugin sectorial.

Registros tardíos o corregidos se aplican conforme a una política versionada. Si
ya existe una factura emitida, originan cargos de ajuste o candidatos de nota; no
mutan el documento ni su origen histórico.

### 7. Alcance inicial

La primera edición admite cargos fijos adelantados/vencidos, ciclos mensuales de
calendario o aniversario, cambios prorrateados, consumo simple con tarifa plana,
correcciones y corridas manuales/programadas. Rating escalonado, bolsas, mínimos,
tiempo real y reparto entre pagadores quedan fuera hasta nuevas decisiones.

## Consecuencias

### Positivas

- planes y consumo evolucionan sin contaminar documentos;
- el cálculo se reproduce y audita antes de emitir;
- cualquier plugin sectorial puede originar uso mediante un contrato estable;
- la emisión masiva y SIFEN continúan siendo capacidades independientes;
- correcciones tardías conservan historia en vez de reescribirla.

### Costes y riesgos

- el roadmap crece a dieciocho plugins y desplaza todos los órdenes posteriores;
- tiempo, redondeo y correcciones requieren una matriz de pruebas amplia;
- programar corridas exige locks, idempotencia, observabilidad y recuperación;
- una frontera de consumo demasiado genérica puede convertirse en EAV; el
  contrato deberá limitar unidades, dimensiones y extensiones versionadas.

## Alternativas descartadas

### Integrarlo completamente en `commercial_documents`

Se descarta porque el documento no debe poseer suscripciones, consumo ni cambios
anteriores a la emisión.

### Convertir `recurring_billing` en propietario de facturas

Se descarta porque duplicaría numeración, snapshots, correcciones y fiscalidad.

### Crear un plugin por cada tipo de plan

Se descarta para el núcleo reusable. Los sectores aportan consumo y contratos
específicos; el cálculo recurrente usa conceptos neutrales y acotados.

## Verificación futura obligatoria

La implementación deberá confirmar RB-D01–RB-D10, diseñar API Java pura,
migraciones privadas, precisión, temporalidad, permisos, scheduler recuperable,
outbox/inbox, matrices con plugins presentes/ausentes, PostgreSQL/Testcontainers,
ArchUnit, seguridad negativa, carga, UI responsive y demo. No comienza antes de
completar el orden 8.

## Referencias

- [ADR-0031 — Facturación masiva](0031-facturacion-masiva-en-documentos-comerciales.md)
- [Análisis del dominio](../knowledge-base/commercial-documents/recurring-billing-domain-analysis.md)
- [Épica de facturación recurrente](../backlog/epica-facturacion-recurrente.md)
- [Modelo canónico de documentos y SIFEN](0010-modelo-canonico-documentos-referencia-sifen.md)
- [ADR-0034 — Plugin de telemetría vehicular](0034-plugin-telemetria-vehicular.md)
