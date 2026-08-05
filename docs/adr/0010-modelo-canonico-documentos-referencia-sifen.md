# ADR-0010 — Modelo canónico de documentos y SIFEN como referencia estructural

- Estado: Aceptado; propiedad y facturación masiva precisadas por ADR-0011 y ADR-0031
- Fecha: 2026-07-28
- Decisión de producto: analizar SIFEN para diseñar persistencia de documentos
- Fuente analizada: Manual Técnico SIFEN versión 150, 2019-09-10

## Contexto

Factura, nota de crédito, nota de débito y nota de remisión comparten gran cantidad
de información, pero también tienen reglas y ciclos de vida propios. El manual
SIFEN organiza el Documento Electrónico en grupos comunes, grupos específicos,
ítems, pagos, transporte, totales, referencias, firma y eventos. Esa organización
es una buena fuente para descubrir entidades, cardinalidades y datos históricos.

El manual proporcionado es la versión 150 de 2019 y declara que puede sufrir
modificaciones. Copiar su XSD como modelo de dominio acoplaría el ERP a una edición
externa, produciría tablas frágiles y mezclaría operación comercial con transmisión
fiscal.

## Decisión

### 1. Uso acotado de SIFEN

SIFEN se analizará para identificar estructura, cardinalidades, condicionalidad,
documentos relacionados, evidencia y ciclos de vida. No se copiará código ni se
convertirá cada nodo XML en una propiedad central del ERP.

La versión 150 es una referencia histórica. Antes de implementar o certificar se
deberá verificar la versión oficial vigente del manual, XSD, catálogos y reglas.

### 2. Modelo canónico independiente

Los plugins funcionales usarán un modelo comercial canónico independiente de
SIFEN. El agregado tendrá una cabecera común y colecciones tipadas para:

- participantes y direcciones como snapshots históricos;
- ítems, precios, descuentos, anticipos e impuestos;
- pagos, crédito y cuotas;
- totales calculados y snapshot emitido;
- documentos asociados;
- logística y transporte cuando corresponda;
- extensiones 1:1 específicas por tipo.

Factura, nota de crédito/débito y remisión no se persistirán en una tabla única con
cientos de campos opcionales.

### 3. Persistencia relacional y artefacto fiscal

Las consultas y reglas operativas usarán tablas relacionales versionadas. XML/JSON
no será la única fuente operativa. El payload fiscal generado y firmado se
conservará como artefacto inmutable, junto con hash, versión, CDC, numeración,
envíos, respuestas y eventos.

Los importes usarán tipos decimales explícitos. Numeración, serie, RUC, códigos y
otros valores con formato significativo no se convertirán indiscriminadamente a
números que pierdan ceros o representación.

### 4. Inmutabilidad y snapshots

Los datos de emisor, receptor, direcciones, productos y condiciones quedan
congelados al emitir. Cambiar un maestro no modifica el documento histórico. Una
corrección posterior se expresa mediante otro documento o evento relacionado.

Estado comercial, fiscal y logístico se modelarán por separado. Los eventos serán
append-only y el estado actual se derivará de transiciones autorizadas.

### 5. Frontera del adaptador SIFEN

Un adaptador traduce el agregado canónico al formato SIFEN vigente y traduce
respuestas/eventos a contratos del plugin. El vocabulario de nodos XML no invade el
dominio. El adaptador es versionable y puede coexistir temporalmente con más de una
versión durante una migración controlada.

### 6. Propiedad modular

El kernel no será dueño de facturas, notas ni remisiones. Pertenecerán a plugins
funcionales con esquemas y migraciones propios. Si varios plugins necesitan
referenciar documentos, usarán IDs públicos y contratos; no relaciones JPA ni
lectura directa de tablas privadas.

ADR-0011 asignó después el agregado a `commercial_documents` y el adaptador a
`sifen`. ADR-0031 precisó que la facturación masiva permanece dentro del primero y
que sus lotes comerciales no son los lotes técnicos del segundo. Se evita así un
módulo compartido genérico sin dueño.

## Consecuencias

### Positivas

- el ERP no queda atado a un XSD fiscal específico;
- se reutiliza estructura común sin mezclar particularidades de cada documento;
- los documentos históricos son reproducibles y auditables;
- cambios de catálogos o versiones SIFEN se aíslan en el adaptador;
- PostgreSQL conserva integridad y capacidad de consulta;
- plugins mantienen propiedad clara de datos.

### Costes y riesgos

- se necesita una capa explícita de mapeo canónico-SIFEN;
- snapshots duplican deliberadamente datos maestros;
- coexistir con versiones fiscales exige versionado y pruebas de regresión;
- separar estados comercial, fiscal y logístico agrega diseño inicial;
- una referencia de 2019 puede ocultar cambios estructurales posteriores.

## Alternativas descartadas

### Persistir el XML como único documento

Se descarta porque dificulta reglas de negocio, búsquedas, integridad referencial,
evolución y reportes, aunque el XML firmado sí se conserva como evidencia.

### Una tabla universal con todas las columnas SIFEN

Se descarta por nulabilidad masiva, acoplamiento a versión, constraints condicionales
frágiles y mezcla de facturación con logística.

### EAV o mapa libre para todos los campos

Se descarta porque pierde tipos, cardinalidades, reglas, índices y trazabilidad.

### Colocar el modelo en el kernel

Se descarta porque son dominios funcionales y fiscales, no capacidades
transversales de identidad, empresas, seguridad o plugins.

## Verificación futura obligatoria

La historia que implemente documentos deberá:

1. registrar versión y checksum del manual/XSD oficial vigente;
2. caracterizar factura, nota de crédito, nota de débito y remisión;
3. demostrar round-trip entre agregado, XML, firma y respuesta;
4. probar cardinalidades, precisión, redondeo, concurrencia e inmutabilidad;
5. verificar relaciones documentales y eventos;
6. ejecutar migraciones y rollback con PostgreSQL;
7. mantener contratos entre plugins sin acceso JPA cruzado;
8. actualizar la guía de implementación y su PDF de cierre.

## Referencia interna

El análisis de campos, grupos y recomendaciones se conserva en
[SIFEN v150 como referencia estructural](../knowledge-base/sifen-v150-estructura-documentos.md).
