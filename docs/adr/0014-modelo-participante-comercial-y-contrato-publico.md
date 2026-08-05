# ADR-0014 — Modelo de participante comercial y contrato público

- Estado: Aceptado
- Fecha: 2026-07-29
- Historia: `J11-S6-02`
- Decisión de producto: BP-D01 a BP-D10 confirmadas sin cambios el 2026-07-29

## Contexto

La caracterización del legado encontró un maestro de personas y roles separados de
cliente/proveedor, pero también datos duplicados, reglas de otros dominios, bajas
físicas, códigos `MAX + 1` y validaciones incompletas. Los plugins posteriores
necesitan referenciar participantes sin importar entidades, repositorios o tablas
de `business_partners`.

ADR-0002 exige que todo contrato empresarial público resida en un módulo
`<plugin>-api` Java puro separado de la implementación. BP-D01 a BP-D10 aceptan
participantes sin rol, roles coexistentes, códigos normalizados, duplicados bajo
revisión, correo opcional, contactos livianos, geografía por código/texto,
inactivación sin baja física y exclusión de migración/DNIT en Sprint 6.

## Decisión

### 1. Módulos y dirección de dependencias

Se crean dos módulos:

- `business-partners-api`: contrato público Java puro, sin Jakarta, JPA, JDBC,
  Hibernate, UI ni clases internas de la implementación;
- `business-partners`: plugin desplegable, dominio y futuros adaptadores; depende
  de su API, `plugin-api` y puertos públicos indispensables del kernel.

Los consumidores dependen únicamente de `business-partners-api`. El kernel no
depende de ninguno de los dos módulos. La composición física del plugin se
incorporará en `J11-S6-06`, no en esta decisión.

### 2. Identidad y empresa

- `BusinessPartnerId` encapsula un UUID canónico y es la única referencia entre
  plugins.
- RUC, cédula, código general y códigos de rol nunca son identidad técnica.
- El agregado conserva `CompanyId` y todos los puertos públicos reciben empresa de
  forma explícita. Los adaptadores deberán contrastarla con contexto confiable.
- Ninguna consulta puede devolver un participante perteneciente a otra empresa.

### 3. Agregado y ciclo de vida

`BusinessPartner` es el agregado raíz. Conserva tipo `NATURAL_PERSON` u
`ORGANIZATION`, código general, nombre visible, nombres legal/comercial opcionales,
estado `ACTIVE/INACTIVE`, versión optimista, roles, identificaciones, direcciones,
canales y contactos nominales.

- Puede existir sin roles.
- Puede tener simultáneamente `CLIENT` y `SUPPLIER`.
- Cada rol tiene estado independiente y código opcional.
- Inactivar el participante no borra ni reescribe hijos; impide nuevas altas de rol
  o mutaciones operativas salvo reactivación/corrección expresamente autorizada.
- No existe operación de baja física en el dominio.

### 4. Códigos y nombres

El código general es obligatorio. Código general y códigos de rol se normalizan
con Unicode NFKC, recorte, mayúsculas invariantes y un máximo de 64 caracteres;
no admiten espacios ni caracteres de control. La unicidad se aplicará por empresa
y ámbito en la persistencia de `J11-S6-03`. La generación futura podrá ser manual o
una secuencia transaccional; nunca `MAX + 1`.

Los nombres se recortan y colapsan espacios. Toda clase exige nombre visible. Una
organización puede conservar además nombre legal y comercial. El dominio no exige
correo universalmente.

### 5. Identificaciones, canales, direcciones y contactos

- Una identificación conserva tipo, país opcional, valor presentado, valor
  normalizado, dígito verificador opcional y vigencia opcional.
- El dominio produce una clave candidata de duplicado, pero no fusiona ni rechaza
  universalmente RUC/cédula; la política de alta deberá advertir coincidencias.
- Canales y direcciones tienen tipo/finalidad extensibles mediante códigos
  validados, no enums fiscales cerrados.
- Existe como máximo un elemento primario activo por categoría y finalidad; marcar
  otro reemplaza de forma explícita al anterior dentro del agregado.
- País usa código ISO cuando exista; departamento, ciudad y texto histórico se
  conservan sin crear un catálogo transversal genérico.
- Un contacto nominal es un hijo liviano. No se convierte automáticamente en otro
  participante ni expone su información en el contrato público mínimo.

### 6. Contrato público mínimo y versión

La primera versión semántica del contrato es `1.0.0`. Publica únicamente:

- `BusinessPartnerId`;
- tipo, estado y roles comerciales;
- `BusinessPartnerReference`, una proyección inmutable mínima;
- `BusinessPartnerDirectory`, un puerto de lectura por `CompanyId` e ID;
- `BusinessPartnerContractVersion.CURRENT`.

La referencia no contiene entidades, direcciones, correos, documentos completos,
datos de crédito ni detalles fiscales. Un consumidor que necesite más información
debe justificar y versionar una proyección explícita.

### 7. Eventos y contribuciones técnicas

Esta historia no publica eventos: no existe consumidor real. Tampoco declara
migraciones, menús, permisos o pantallas. El descriptor `business_partners@1.0.0`
nace funcional y compatible con `plugin-api` 0.3.x; las contribuciones se agregarán
en las historias propietarias.

## Alternativas descartadas

### Copiar las entidades del legado

Se descarta porque mezcla ventas, crédito, compras, logística, tesorería y
contabilidad, además de duplicar datos y deudas técnicas.

### Un módulo único para API e implementación

Se descarta porque permitiría dependencias accidentales hacia el agregado, JPA o
adaptadores y contradice ADR-0002.

### Usar RUC o código como clave primaria

Se descarta por mutabilidad, formatos externos, duplicados históricos y necesidad
de referencias estables entre plugins.

### Fusionar duplicados automáticamente

Se descarta hasta disponer de perfilado, reglas de supervivencia, referencias,
auditoría, respaldo y recuperación.

## Consecuencias

- Aparecen dos módulos Maven por la separación física de contratos.
- Los adaptadores futuros deben mapear entre entidades privadas y tipos públicos.
- La proyección mínima protege datos personales y reduce compatibilidad accidental.
- Un cambio incompatible requiere nueva versión mayor del contrato y rango de
  dependencia explícito en consumidores.
- Persistencia, concurrencia PostgreSQL, índices de unicidad, endpoints y UI siguen
  pendientes y no pueden inferirse como implementados por este ADR.

## Verificación

- unitarias de tipos públicos, normalización y agregado;
- pruebas de roles coexistentes, estado independiente, participantes sin rol,
  primarios únicos y concurrencia por versión;
- ArchUnit para API/dominio sin frameworks y ausencia de dependencias prohibidas;
- descriptor CDI/SPI compatible, sin migraciones, permisos, menús o pantallas;
- `mvn verify` del reactor antes de cerrar `J11-S6-02`.

