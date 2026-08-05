# ADR-0015 - Persistencia privada de participantes comerciales

- Estado: Aceptado
- Fecha: 2026-07-29
- Historia: `J11-S6-03`
- Decisiones relacionadas: ADR-0003, ADR-0012 y ADR-0014

## Contexto

El dominio neutral de `business_partners` ya distingue participante, roles,
identificaciones, direcciones, canales y contactos nominales. Falta llevar esas
invariantes a PostgreSQL sin crear relaciones con tablas privadas del kernel u
otros plugins, sin convertir RUC/cédula en identidad técnica y sin reintroducir
bajas físicas ni generación de códigos mediante `MAX + 1`.

## Decisión

### Propiedad y unidad de persistencia

- El plugin es dueño exclusivo del esquema `plg_business_partners`.
- Su migración inicial vive en
  `classpath:db/migration/business_partners/V1__initialize_business_partners_schema.sql`.
- La unidad JPA se llama `logixone-business-partners-pu`, usa el datasource JTA
  `java:/jdbc/LogixoneCoreDS`, tiene DDL automático deshabilitado y valida el
  esquema creado por Flyway.
- `company_id` se conserva como UUID de ámbito en todas las filas, pero no se crea
  una clave foránea hacia `core.company`. La existencia y autorización de empresa
  se resuelven por contratos del kernel antes de invocar el repositorio.
- No existen asociaciones JPA ni claves foráneas hacia esquemas ajenos.

### Modelo relacional

La V1 crea ocho tablas:

1. `business_partner`: raíz, empresa, UUID opaco, código general, tipo, nombres,
   estado, versión optimista y marcas temporales;
2. `business_partner_role`: roles `CLIENT`/`SUPPLIER`, estado y código opcional;
3. `business_partner_identification`: valor presentado, valor normalizado, tipo,
   país, dígito verificador y vigencia;
4. `business_partner_address`: dirección tipada, finalidad, geografía y marca de
   principal;
5. `business_partner_channel`: canal general tipado y marca de principal;
6. `business_partner_contact`: contacto nominal liviano;
7. `business_partner_contact_channel`: medios propios del contacto;
8. `business_partner_code_sequence`: contador transaccional por empresa y ámbito
   para políticas automáticas futuras, sin inferir el siguiente valor de datos
   comerciales.

Todos los hijos llevan `company_id` y `business_partner_id` y usan claves foráneas
compuestas hacia el propietario de la misma empresa. Las tablas de detalle no se
reutilizan como catálogos genéricos.

### Restricciones e índices

- El código general es único por empresa.
- Un código de rol no nulo es único por empresa y tipo de rol.
- Las identificaciones no son únicas: se indexa su clave candidata por empresa
  para advertir duplicados, conforme a BP-D04.
- Solo puede existir una dirección primaria activa por empresa, participante,
  tipo y finalidad; se aplica la misma regla a canales generales y de contacto.
- Estados, tipos y versiones se protegen con restricciones `CHECK`.
- Las consultas previstas se indexan por empresa, estado, rol, código, nombre e
  identificación normalizada. Todo acceso del repositorio exige `company_id`.

### Actualización y retención

- La raíz usa `@Version`; una escritura obsoleta se convierte en un conflicto
  estable del puerto de persistencia.
- El repositorio inserta y sincroniza filas existentes, pero no expone una
  operación de borrado. La operación normal es inactivar.
- Quitar o desactivar el plugin no elimina esquema, historial ni datos.
- Las migraciones aplicadas son inmutables. Una corrección futura usa `V2` o
  superior y el patrón expandir-migrar-contraer cuando corresponda.

## Alternativas descartadas

### Una tabla única con columnas opcionales

Se descarta porque mezcla colecciones y roles, multiplica nulos y dificulta
restricciones de cardinalidad y trazabilidad.

### Relaciones JPA con `core.company`

Se descartan porque acoplan el plugin a una entidad interna del kernel y crean una
dependencia de persistencia cruzada que los contratos ya prohíben.

### Unicidad fuerte de RUC o cédula

Se pospone hasta perfilar datos y reglas oficiales. BP-D04 exige advertencia, no
rechazo universal.

### Borrado en cascada como operación normal

Se descarta por BP-D09 y porque otros dominios conservarán referencias históricas
por identificador.

## Consecuencias

- El plugin agrega Jakarta Persistence solo en su paquete de infraestructura; API,
  dominio y puertos permanecen neutrales.
- La composición física del WAR/migrador se realizará en `J11-S6-06`; la V1 ya es
  descubrible por el descriptor y puede probarse aisladamente desde ahora.
- Las reglas de autorización, auditoría y casos de uso siguen reservadas para
  `J11-S6-04`.
- No se crea outbox porque todavía no existe un consumidor real.

## Verificación obligatoria

1. migrar PostgreSQL vacío y repetir sin reaplicar;
2. validar el esquema con JPA sin crear ni modificar DDL;
3. probar unicidad de códigos y no unicidad de identificaciones;
4. probar aislamiento por empresa y claves foráneas compuestas;
5. probar primario único por categoría/finalidad;
6. probar round-trip del agregado y conflicto de versión obsoleta;
7. comprobar que no existe API de borrado y que los datos permanecen tras
   inactivación;
8. ejecutar pruebas de módulo, ArchUnit y `mvn verify`.
