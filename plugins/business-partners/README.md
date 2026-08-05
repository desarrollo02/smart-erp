# Business partners

- Artifact Maven: `business-partners`
- Plugin ID: `business_partners`
- Tipo: `FUNCTIONAL`
- Versión inicial: `1.0.0`
- Compatibilidad Plugin API: `[0.4.0,0.5.0)`

Este módulo fue creado por el generador versionado de Logixone. `J11-S6-02`
completó el dominio neutral, `J11-S6-03` agregó persistencia, `J11-S6-04` cerró
aplicación/seguridad, `J11-S6-05` incorporó su primera interfaz productiva y
`J11-S8-C02` agregó definiciones empresariales para tipos de identificación,
tipos/propósitos de dirección y tipos de canal, con ciclo versionado, revisión de
nombre e historial visible append-only. J11-S8-C03 conecta el país de
identificación a `reference-data-api` y exige `reference_data` 1.x.

- `business-partners-api` contiene el contrato público Java puro `1.0.0`.
- este JAR contiene descriptor, dominio, aplicación, puertos, JPA y handler visual;
- declara dos capacidades, cuatro permisos, dos menús, pantallas `directory` y
  `definitions`, y dos slots por pantalla;
- continúa sin eventos porque no existe todavía un intercambio asíncrono real;
- otros plugins sólo pueden depender de `business-partners-api`.
- este plugin consume `reference-data-api`; nunca tablas o clases internas de
  `reference_data`.

## Registro en el reactor

Los módulos `business-partners-api` y `business-partners` están registrados en el
reactor, `dependencyManagement` y los perfiles físicos de demo. WAR y migrador
deben construirse siempre con el mismo perfil; el WAR base continúa sin incorporar
plugins productivos por accidente. La presencia física sigue siendo una decisión
revisable de composición y requiere reconstrucción/redespliegue.

## Dominio neutral disponible

- identidad opaca `BusinessPartnerId` y empresa explícita `CompanyId`;
- código normalizado, tipo persona/organización y nombres;
- estado activo/inactivo y versión optimista;
- cero, uno o ambos roles cliente/proveedor con estado independiente;
- identificaciones con valor presentado y clave candidata de duplicado;
- direcciones, canales y contactos nominales livianos;
- un primario activo por categoría/finalidad dentro del agregado;
- inactivación sin baja física.

El puerto público `BusinessPartnerDirectory` sólo devuelve
`BusinessPartnerReference`; no expone hijos, documentos completos, datos
financieros ni clases internas.

## Persistencia

Las migraciones inmutables V1–V4 crean diez tablas relacionales bajo
`plg_business_partners`. V1 conserva participante, detalles y secuencias; V2 agrega
`business_partner_definition` para tipos de canal con empresa, clase, código,
estado y versión. V3 agrega `business_partner_definition_revision`, retroalimenta
la versión vigente de los catálogos existentes y preserva cada alta, revisión de
nombre o cambio de estado por empresa, clase, código y versión. V4 amplía las
clases admitidas, retroalimenta códigos existentes y siembra valores mínimos para
identificación y dirección sin agregar tablas. El descriptor declara la ubicación y el migrador la descubre
mediante el mismo proveedor SPI.

La unidad `logixone-business-partners-pu` usa el datasource JTA administrado,
deshabilita generación de DDL y valida el esquema Flyway. El repositorio exige
`CompanyId` en cada lectura, no expone borrado físico y convierte unicidad o
versión obsoleta en resultados estables. Los códigos automáticos futuros usan
`business_partner_code_sequence`; nunca se calcula `MAX + 1`.

No modifique V1, V2, V3 ni V4 después de aplicadas. Todo cambio usa V5 o superior. No agregue
relaciones JPA, FKs ni consultas hacia tablas privadas de otro propietario.

## Interfaz

La ruta `/business-partners` resuelve la pantalla pública
`business_partners:directory` `1.0.0`. El handler neutral entrega inputs, opciones,
tabla, detalle, selección/versionado y avisos mediante `ScreenInteraction`; el
shell conserva XHTML, Jakarta Faces, Material Design 3 y responsive.

La ruta `/business-partners/definitions` resuelve
`business_partners:definitions`, protegida por `business_partners.manage`. Permite
consultar, registrar, revisar el nombre, leer el historial e inactivar/reactivar
`CHANNEL_KIND`, `IDENTIFICATION_TYPE`, `ADDRESS_TYPE` y `ADDRESS_PURPOSE`. La
ficha principal ofrece sólo opciones activas de la empresa y la aplicación vuelve
a validar empresa, clase y estado antes de persistir. El nombre visible no
reemplaza el código estable y las revisiones históricas son de solo lectura.

Los slots `directory_extensions` y `detail_extensions` quedan disponibles para una
personalización futura compatible. Un plugin nunca reemplaza XHTML ni importa
beans privados de otro plugin. Playwright valida 375, 599, 600, 720, 839, 840 y
1280 px sin overflow horizontal.

Consulte [plugin-contract.md](docs/plugin-contract.md) antes de agregar lógica.

## Prueba local

```powershell
.\mvnw.cmd -B -pl plugins/business-partners -am test
.\mvnw.cmd -B -pl plugins/business-partners -am verify "-Dlogixone.postgres.integration=true"
.\mvnw.cmd -B -pl tests/architecture-tests -am test
.\mvnw.cmd -B verify
```
