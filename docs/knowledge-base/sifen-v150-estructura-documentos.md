# SIFEN v150 como referencia estructural para documentos comerciales

- Fecha de análisis: 2026-07-28
- Fuente: `Manual+Técnico+Versión+150.pdf`
- Título interno: `Manual Técnico Sistema Integrado de Facturación Electrónica Nacional (SIFEN)`
- Versión declarada: 150
- Fecha declarada por el manual: 2019-09-10
- Autor declarado: Equipo de Proyecto SIFEN
- Extensión: 217 páginas, 5.204.470 bytes
- SHA-256: `976CD88C05C31041EE86DB1667E1B426E2D9DDF675D941973D5918CBDC5427C6`
- Estado de uso: el PDF local continúa como referencia histórica; el portal oficial
  confirmó versión 150 vigente el 2026-08-02, complementada por notas técnicas
  acumulativas hasta NT-027

## Propósito del análisis

Usar la organización del Documento Electrónico (DE) para decidir cómo conviene
persistir facturas, notas de crédito, notas de débito, remisiones y documentos
relacionados. No se pretende copiar código, reproducir el XSD como tablas ni afirmar
cumplimiento con una edición de 2019.

La portada advierte que el documento puede sufrir modificaciones. Antes de una
integración SIFEN real se deberá obtener y verificar la edición oficial vigente,
sus XSD, catálogos, reglas y servicios.

## Verificación oficial posterior

El 2026-08-02 se consultó la
[documentación técnica oficial de e-Kuatia](https://ekuatia.set.gov.py/web/e-kuatia/documentacion-tecnica).
El portal mantiene el Manual Técnico versión 150 y publica correcciones/mejoras
mediante notas técnicas 001–027. La última visible, NT-027, está fechada
2026-03-09. Por ello el número `150` continúa vigente, pero el PDF local de 2019 por
sí solo no representa todo el baseline aplicable.

También se revisó la guía oficial de mejores prácticas de octubre de 2024. Para el
servicio asíncrono documenta lotes de hasta 50 DE, con un único RUC emisor y tipo
documental, mensaje comprimido de hasta 1.000 KB, respuesta de recepción y consulta
posterior del resultado por documento. Esta verificación sustenta la separación
entre el lote comercial de generación y los lotes técnicos de `sifen` definida en
[ADR-0031](../adr/0031-facturacion-masiva-en-documentos-comerciales.md).

No se descargaron ni congelaron los artefactos regulatorios durante este análisis.
Antes de implementar se deben guardar en `.tools/`, comprobar los checksums
oficiales y registrar manual, todas las notas aplicables, catálogos, XSD y WSDL.

## Secciones estructurales observadas

La tabla de grupos de la página PDF 59 (página impresa 58) organiza el DE en capas:

| Grupo | Concepto observado | Lectura para el ERP |
|---|---|---|
| AA/A | contenedor, versión, identificador y campos firmados | identidad técnica y versión fiscal separadas del ID interno |
| B | datos inherentes a la operación del DE | metadatos de emisión y seguridad fiscal |
| C | timbrado, tipo y numeración | asignación fiscal y secuencia, no identidad primaria interna |
| D/D1 | fecha y condiciones generales de la operación | cabecera comercial común |
| D2/D3 | emisor y receptor | snapshots históricos de participantes y direcciones |
| E | bloque específico por tipo | extensión 1:1 según factura, nota o remisión |
| E7 | condición, forma de pago, crédito y cuotas | condiciones de pago y colecciones repetibles |
| E8 | ítems, precios, descuentos e impuestos | detalle 1:N y cálculos por línea |
| E10 | transporte, salida, entrega, vehículos y transportista | agregado logístico opcional, esencial para remisión |
| F | subtotales y totales | resumen calculado y snapshot fiscal de importes |
| G | información comercial complementaria | extensiones controladas, no bolsa arbitraria por defecto |
| H | documentos asociados | relaciones tipadas entre documentos |
| I/J | firma y campos externos a la firma | artefacto fiscal inmutable y metadatos de transmisión |

El capítulo 11 trata eventos que modifican o afectan el estado de un DE/DTE, como
inutilización, cancelación, ajustes y manifestaciones. Esto confirma que el estado
no debe modelarse únicamente como una columna mutable: se necesita historial de
eventos y auditoría.

## Tipos de documento relevantes

La tabla de timbrado distingue, entre otros:

- factura electrónica;
- nota de crédito electrónica;
- nota de débito electrónica;
- nota de remisión electrónica.

Los grupos específicos revisados muestran:

- factura: datos particulares de presencia y, cuando aplica, compras públicas;
- nota de crédito/débito: motivo de emisión y vínculo con documentos afectados;
- remisión: motivo de traslado, responsable, distancia, fecha futura, transporte,
  origen, destino, vehículo y transportista;
- todos los tipos reutilizan participantes, ítems y referencias comunes, pero no
  todos utilizan operación comercial, pagos o totales de la misma manera.

## Modelo de persistencia recomendado

El diseño inicial debe usar un agregado canónico y relacional. Los nombres finales
se decidirán en la historia del plugin, pero la separación conceptual será:

### Cabecera común

`commercial_document`:

- UUID interno y `CompanyId`;
- tipo de documento y estado de ciclo de vida;
- fecha/hora comercial de emisión;
- moneda y condición cambiaria;
- versión optimista;
- referencias de numeración y fiscalización sin usarlas como PK;
- marcas de creación, emisión y actualización.

### Participantes históricos

`document_party_snapshot` y `document_address_snapshot`:

- rol del participante: emisor, receptor, vendedor, transportista u otro;
- identificador fiscal y tipo de persona/documento;
- razón social/nombre de fantasía;
- dirección y códigos geográficos tal como fueron emitidos;
- referencia opcional al maestro original solamente por ID público.

El snapshot es obligatorio para preservar la representación histórica aunque el
cliente, empresa, sucursal o transportista cambien después.

### Ítems, importes e impuestos

`document_line`, `document_line_adjustment` y `document_line_tax`:

- orden estable dentro del documento;
- código y descripción histórica del producto/servicio;
- unidad, cantidad y país de origen cuando corresponda;
- precio, tipo de cambio por ítem, descuentos, anticipos y total de línea;
- tratamiento, tasa, base e importe tributario;
- precisión decimal explícita con `NUMERIC`, nunca `float`/`double`.

Los totales se recalculan mediante reglas de dominio antes de emitir, pero también
se persiste el snapshot total firmado para poder auditar y reproducir el documento.

### Pagos y crédito

`document_payment_condition`, `document_payment` y `document_installment`:

- contado/crédito;
- medios de pago repetibles;
- entrega inicial;
- cuotas 0:N con moneda, monto y vencimiento;
- estado de cobranza separado del valor fiscal emitido.

### Extensiones por tipo

Evitar una tabla gigante con columnas nulas. Usar una cabecera común y tablas 1:1
para los datos realmente específicos:

- `invoice_detail`;
- `credit_debit_note_detail` con motivo y naturaleza del ajuste;
- `remittance_detail` con motivo y responsable del traslado;
- `document_transport`, `document_transport_location`, `document_vehicle` y
  `document_carrier` cuando exista logística.

### Relaciones entre documentos

`document_reference` debe almacenar:

- documento origen y documento relacionado por IDs internos cuando ambos existen;
- tipo de relación: corrige, debita, acredita, remite, reemplaza, cancela u otro
  vocabulario cerrado;
- CDC, número, fecha y tipo externos como snapshot cuando el documento relacionado
  no pertenece a la misma base;
- orden y motivo de la relación.

No crear una relación JPA directa si los documentos terminan en plugins distintos.
En ese caso se usan IDs y un contrato público de consulta.

### Frontera fiscal SIFEN

El modelo canónico no debe tener nodos como `gCamItem` o `gTotSub` como nombres de
dominio. Un adaptador SIFEN transforma el agregado al XSD vigente y conserva:

- versión de manual/XSD y catálogos;
- CDC, dígito verificador, código de seguridad y timbrado;
- establecimiento, punto, número y serie preservando ceros iniciales;
- XML generado, XML firmado o referencia inmutable al artefacto;
- hash, firma, certificado técnico identificable sin guardar secretos;
- lote, envío, respuesta, protocolo, códigos y mensajes;
- eventos fiscales append-only y estado derivado.

El XML firmado sirve como evidencia y reproducción; no reemplaza las tablas
operativas relacionales del documento.

## Reglas de datos derivadas

1. Usar UUID interno; CDC y numeración son claves naturales/fiscales únicas, no PK.
2. Guardar números con ceros iniciales como texto canónico cuando su formato sea
   parte de la identidad externa.
3. Usar códigos estables y catálogos versionados; las descripciones son snapshots.
4. Modelar cardinalidades explícitas: ítems 1:N, cuotas 0:N, actividades 1:N,
   referencias 0:N y vehículos/transportistas según el tipo.
5. Validar reglas condicionales por tipo en dominio/aplicación y reforzar en base
   las invariantes que PostgreSQL pueda expresar sin acoplarse a una versión de XSD.
6. Un documento emitido/fiscalizado es inmutable; las correcciones se representan
   mediante nota, cancelación u otro evento/documento relacionado.
7. Separar estado comercial, estado fiscal y estado logístico para evitar que una
   respuesta externa sobrescriba el ciclo de vida interno.
8. Toda operación pertenece a una empresa y debe respetar autorización del servidor.

## Antipatrones descartados

- copiar cada campo del XML a una única tabla de cientos de columnas;
- guardar solo XML o JSON y consultar el negocio desde el payload;
- usar EAV para evitar diseñar tipos y cardinalidades;
- usar nombres SIFEN como lenguaje ubicuo de todo el ERP;
- recalcular un documento histórico desde maestros actuales;
- editar filas emitidas en vez de producir eventos/documentos correctivos;
- colocar facturación, remisiones o integración fiscal dentro del kernel;
- crear relaciones JPA entre entidades privadas de plugins distintos.

## Pendientes antes de implementar

- descargar y congelar con checksum el manual 150, notas, XSD, catálogos y WSDL
  vigentes al iniciar la implementación;
- materializar `commercial_documents` como propietario del agregado canónico y
  `sifen` como adaptador, según ADR-0011 y ADR-0031;
- comparar factura, notas y remisión vigentes campo por campo;
- definir precisión monetaria, zonas horarias, numeración y concurrencia;
- diseñar ciclo de vida comercial/fiscal/logístico y política de inmutabilidad;
- producir migraciones, casos de uso y pruebas de caracterización;
- documentar retención, respaldo y recuperación de artefactos firmados.
