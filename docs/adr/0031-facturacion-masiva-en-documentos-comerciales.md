# ADR-0031 — Facturación masiva dentro de documentos comerciales

- Estado: Aceptado; ampliado por ADR-0033
- Fecha: 2026-08-02
- Decisión de producto: analizar y ubicar la generación masiva de facturas
- ADR relacionados: [ADR-0010](0010-modelo-canonico-documentos-referencia-sifen.md),
  [ADR-0011](0011-roadmap-dependencias-plugins-productivos.md) y
  [ADR-0013](0013-eventos-integracion-outbox-por-plugin.md)

> Nota vigente: [ADR-0033](0033-dominio-facturacion-recurrente.md) confirma el
> dominio reutilizable previsto aquí y agrega `recurring_billing`. La preparación
> de cargos recurrentes pertenece al nuevo plugin; la preparación y emisión del
> lote de facturas permanece en `commercial_documents`.

## Contexto

Empresas de servicios, alquileres, membresías y contratos pueden acumular cargos de
un período y emitir cientos o miles de facturas en una fecha común. El legado de
Multienvíos implementa una secuencia de actividades, prefacturas y facturas
seleccionadas; Ingenio La Felsina demuestra además que un dominio distinto puede
originar un comprobante.

Una facturación masiva no es una sola factura grande. Es una ejecución empresarial
que crea documentos independientes, cada uno con receptor, líneas, impuestos,
numeración, resultado y ciclo fiscal propios. Tampoco debe confundirse con el lote
técnico mediante el que SIFEN recibe varios Documentos Electrónicos.

Crear un plugin separado sin definir estas propiedades duplicaría la autoridad de
`commercial_documents` sobre emisión, numeración e inmutabilidad. Integrarlo en
`sales`, `treasury` o el kernel convertiría a esos módulos en propietarios de
facturas que no les pertenecen.

## Decisión

### 1. Propietario inicial

La preparación y ejecución de lotes de facturas será una capacidad de
`commercial_documents`. No se agrega un plugin `bulk_billing` al roadmap.

El plugin será dueño de:

- identidad, estado y eventos del lote comercial;
- snapshot de cada candidato aprobado;
- idempotencia y resultado individual;
- emisión de cada factura canónica;
- asignación concurrente de numeración;
- permisos, auditoría, métricas y recuperación del proceso.

Los datos que originan el cobro continúan en su dominio. `sales`, nómina, contratos
u otro plugin entregan solicitudes tipadas mediante `commercial-documents-api`; no
se leen sus tablas ni se importan sus entidades o controladores.

### 2. Preparación antes de emisión

El flujo obligatorio será:

1. crear el lote para una empresa, período y fecha comercial propuesta;
2. recibir candidatos con clave y versión de origen;
3. prevalidar receptor, concepto, moneda, impuestos, totales, autorización,
   numeración disponible y fecha;
4. congelar un snapshot reproducible;
5. mostrar errores y exclusiones antes de aprobar;
6. aprobar mediante un permiso distinto de la mera consulta;
7. ejecutar en bloques pequeños y confirmar cada ítem en su propia transacción;
8. conservar éxito o fallo por ítem y permitir reintentos controlados;
9. publicar los hechos confirmados mediante el outbox del plugin.

No se numeran borradores durante la vista previa. Un cambio de criterio después de
congelar crea una nueva revisión o lote; no altera silenciosamente el aprobado.

### 3. Idempotencia y concurrencia

Cada ítem tiene una clave única por, al menos:

`(company_id, source_plugin_id, source_type, source_id, source_version,
billing_period, document_type)`.

La versión final deberá documentar cuándo una corrección del origen permite una
nueva factura y cuándo exige nota de crédito/débito. Repetir una solicitud, perder
la respuesta HTTP o reiniciar el trabajador no puede producir un documento doble.

La numeración usa un asignador atómico por ámbito autorizado. Se prohíben `MAX + 1`,
contadores en memoria y reservas masivas sin política documentada de huecos. Los
trabajadores reclaman ítems con control optimista o bloqueo explícito y nunca
mantienen una transacción abierta durante todo el lote.

### 4. Resultado parcial y recuperación

El lote admite éxito parcial porque las facturas son agregados independientes. Un
fallo no revierte documentos ya emitidos. Cada resultado se clasifica como:

- exitoso y asociado a un `DocumentId`;
- reintentable sin cambiar la entrada;
- bloqueado hasta corregir configuración o candidato;
- definitivo y no reintentable;
- omitido por decisión autorizada.

Reintentar, omitir, cancelar un lote todavía no ejecutado o reconstruir una
proyección requiere autorización y auditoría. Una factura emitida no se elimina ni
se “desfactura”; se aplica su ciclo correctivo.

### 5. Fechas separadas

Se modelan por separado:

- período de consumo o servicio;
- fecha/hora de corte de candidatos;
- fecha comercial solicitada para cada factura;
- instante real de creación y emisión;
- instante de firma y transmisión fiscal;
- vencimiento o calendario de cobro.

Una fecha común es una entrada empresarial, no un permiso para retrofechar. Antes
de emitir se revalidan zona horaria, período abierto, autorización fiscal,
timbrado/establecimiento/punto, moneda y reglas vigentes.

### 6. Frontera SIFEN

`commercial_documents` emite facturas y publica su proyección fiscal. `sifen` firma,
transmite y consulta resultados. Sus lotes son técnicos y separados del lote
comercial.

Según la documentación oficial verificada el 2026-08-02, el Manual Técnico vigente
continúa en versión 150, complementado por notas técnicas hasta la número 27. La
guía oficial de envío asíncrono indica lotes de hasta 50 DE, un solo RUC emisor, un
solo tipo documental y un mensaje comprimido de hasta 1.000 KB. El resultado se
consulta posteriormente y no se reenvía un CDC que siga sin estado definitivo.

Por tanto:

- un lote comercial de 2.000 facturas puede producir múltiples lotes SIFEN;
- el éxito de emisión no equivale a aprobación fiscal;
- protocolo, CDC, respuesta y reintentos pertenecen a `sifen`;
- cada documento conserva estados comercial y fiscal independientes.

### 7. Seguridad y operación

Los permisos mínimos previstos son:

- `commercial_documents.invoice_batches.view`;
- `commercial_documents.invoice_batches.prepare`;
- `commercial_documents.invoice_batches.approve`;
- `commercial_documents.invoice_batches.execute`;
- `commercial_documents.invoice_batches.retry`;
- `commercial_documents.invoice_batches.cancel`.

La política empresarial podrá exigir separación entre quien prepara y quien
aprueba/ejecuta. La auditoría registra empresa, actor, lote, revisión, conteos,
operación y códigos de resultado, sin incorporar líneas, RUC, nombres ni otros
datos personales innecesarios en logs.

La UI Jakarta Faces mostrará criterios, vista previa paginada, errores agrupados,
confirmación, progreso, resultados y reintentos. No descargará miles de registros
al navegador ni dependerá de mantener abierta la sesión web. Los tres rangos
responsive y la navegación por teclado son criterios de aceptación.

### 8. Frontera con `recurring_billing`

ADR-0033 agrega el plugin porque producto confirmó un dominio reutilizable de:

- planes y calendarios recurrentes;
- suscripciones o contratos facturables;
- prorrateos, altas/bajas dentro del período;
- consumo medido y consolidación;
- reglas de agrupación independientes de un único origen.

Ese plugin produce candidatos y depende del contrato público de
`commercial_documents`; nunca poseería facturas, numeración, XML, CDC ni tablas del
documento.

## Consecuencias

### Positivas

- una sola autoridad mantiene las invariantes de emisión individual y masiva;
- el proceso puede reanudarse y auditarse sin duplicar facturas;
- cualquier dominio puede originar cargos sin compartir entidades;
- el envío fiscal escala de forma independiente;
- la ampliación del roadmap ocurre solo después de confirmar un dominio autónomo.

### Costes y riesgos

- `commercial_documents` necesitará trabajadores recuperables y observabilidad;
- la definición de idempotencia exige versionar correctamente cada origen;
- el éxito parcial requiere UX, permisos y soporte operativo cuidadosos;
- separar los lotes comercial y fiscal agrega estados, pero evita mezclar emisión
  con transmisión;
- planes, prorrateo y consumo complejo evolucionan en `recurring_billing` sin
  agrandar el agregado documental.

## Alternativas descartadas

### Plugin `bulk_billing` desde el inicio

Se descarta porque no sería dueño del documento ni demuestra todavía planes o
reglas propios. Sería un orquestador obligatorio para una operación que también
debe existir al emitir pocas facturas.

### Facturación masiva dentro de `sales`

Se descarta porque no todas las facturas nacen de pedidos y ventas no debe poseer
numeración, snapshots emitidos ni estados fiscales.

### Lote global en el kernel

Se descarta porque criterios, errores e ítems son datos de negocio privados, no una
responsabilidad transversal del kernel.

### Una transacción para todo el lote

Se descarta por duración, bloqueo, memoria, recuperación y riesgo de repetir
facturas después de una respuesta incierta.

## Verificación futura obligatoria

La épica deberá probar idempotencia, concurrencia, numeración, precisión,
snapshots, reinicio, éxito parcial, reintentos, seguridad negativa, outbox,
PostgreSQL, SIFEN asíncrono, observabilidad y UI responsive. Antes del corte fiscal
se congelarán con checksum el manual, notas, catálogos, XSD y WSDL vigentes.

## Referencias

- [Caracterización de facturación masiva](../knowledge-base/commercial-documents/facturacion-masiva-legacy-characterization.md)
- [Épica de facturación masiva](../backlog/epica-facturacion-masiva.md)
- [ADR-0033 — Dominio independiente de facturación recurrente](0033-dominio-facturacion-recurrente.md)
- [Documentación técnica oficial e-Kuatia](https://ekuatia.set.gov.py/web/e-kuatia/documentacion-tecnica)
