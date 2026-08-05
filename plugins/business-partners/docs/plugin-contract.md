# Contrato vigente de business_partners

- Estado: dominio, API pública `1.0.0`, aplicación, persistencia V1–V4 y UI productiva implementados; país normativo resuelto por `reference_data` en J11-S8-C03
- ADR: `docs/adr/0014-modelo-participante-comercial-y-contrato-publico.md` y
  `docs/adr/0015-persistencia-privada-business-partners.md`
- Historia: `J11-S6-02` a `J11-S6-07` y `J11-S8-C02`

## Propósito y propietario

- Capacidad empresarial: participantes, roles cliente/proveedor, identificaciones,
  direcciones, canales y contactos nominales.
- Responsable funcional: equipo propietario de `business_partners`.
- Responsable técnico: equipo de plugins funcionales de Logixone.
- Empresas o distribución objetivo: cualquier empresa que active la capacidad;
  composición física disponible mediante los perfiles explícitos de distribución.

## Contratos públicos

- Versión: `BusinessPartnerContractVersion.CURRENT = 1.0.0`.
- Capacidades del descriptor: `business_partners.directory` y
  `business_partners.administration`.
- Permisos: `view`, `manage`, `roles.manage` y `lifecycle.manage`.
- Menús: **Socios comerciales** y **Definiciones de socios**.
- Eventos publicados/consumidos: ninguno; no existe consumidor real.
- Puerto: `BusinessPartnerDirectory` por `CompanyId` y `BusinessPartnerId`.
- Tipos públicos: ID, tipo, estado, rol y `BusinessPartnerReference` mínima.
- Regla: consumidores dependen de `business-partners-api`, nunca de este módulo.
- Dependencia requerida: `reference_data [1.0.0,2.0.0)`; este módulo consume sólo
  `reference-data-api` para países.

## Datos

- Esquema privado: `plg_business_partners`
- Agregados: `BusinessPartner` y `BusinessPartnerDefinition`, Java puro y sin
  anotaciones de persistencia.
- Hijos: roles, identificaciones, direcciones, canales y contactos nominales.
- Identificadores externos: RUC/cédula y códigos se conservan como atributos, no
  como identidad técnica.
- Migraciones: V1–V4 inmutables; V2 agrega `business_partner_definition`, V3 su
  historial append-only y V4 incorpora tipos de identificación y tipos/propósitos
  de dirección con backfill y datos iniciales mínimos; historial Flyway propio.
- Persistencia: unidad `logixone-business-partners-pu`, diez entidades JPA
  privadas, DDL deshabilitado y validación de esquema.
- Repositorios: agregado, candidatos de identificación y secuencia transaccional;
  toda consulta exige empresa y no existe operación de baja física.
- Respaldo/recuperación: desactivar o retirar conserva esquema e información; un
  cambio destructivo futuro exige respaldo, recuperación y nueva migración.

## Pantallas responsive

- `business_partners:directory@1.0.0` en `/business-partners`.
- `business_partners:definitions@1.0.0` en
  `/business-partners/definitions`.
- Ambas declaran elementos neutrales y slots `directory_extensions` y
  `detail_extensions`; el shell es dueño del renderer.
- Las operaciones de personalización se limitan a las publicadas por cada
  elemento; no admiten XHTML, EL, CSS ni JavaScript del plugin.
- Evidencia obligatoria en 375/720/1280 px.

## Pruebas y operación

- Caracterización: `docs/knowledge-base/business-partners/legacy-characterization.md`.
- PostgreSQL/Testcontainers: 21 escenarios vigentes entre migraciones y repositorios,
  incluidos aislamiento, concurrencia, secuencias, backfill y resolución
  empresarial de las cuatro clases de definición.
- Arquitectura: API y dominio permanecen sin frameworks ni tablas cruzadas.
- Seguridad negativa: permisos, plugin inactivo y rutas se revalidan en servidor.
- Identificación: el país se ofrece y revalida por empresa/código mediante
  `reference-data-api`; un código ausente o inhabilitado se rechaza antes de
  persistir.
- Health/diagnóstico: incluidos en la composición y el gate de runtime.
- Demo visual: directorio/ficha y las cuatro clases de definición validados dentro
  de `J11-S8-C02` en 1280/720/375 px.
- Riesgos restantes: la publicación mundial completa y el perfilado de datos se
  resolverán en historias propietarias; la persistencia concurrente y unicidad
  física ya están cubiertas por PostgreSQL.
