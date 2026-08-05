# J11-S6-01 - Caracterización de `business_partners`

- Estado: Completada
- Sprint: 6
- Fecha: 2026-07-29
- Tipo: conocimiento del legado y requisitos
- Dependencia: [J11-S6-00](J11-S6-00-gobierno-planificacion.md)
- Evidencia principal: [caracterización del legado](../../knowledge-base/business-partners/legacy-characterization.md)

## Objetivo

Entender el comportamiento real de personas, clientes, proveedores, contactos y
direcciones en el legado y convertirlo en casos de uso, límites e invariantes
neutrales antes de diseñar el primer plugin productivo.

## Estado inicial

Sprint 5 dejó verdes los habilitadores técnicos de composición, migraciones y
plantilla. El nombre y la frontera inicial de `business_partners` estaban
planificados, pero no existían requisitos aceptados, modelo, módulo, esquema ni
persistencia.

El legado fue consultado exclusivamente en
`C:\cosme\multienvios\miaterra`. No se modificó y no se copió código.

## Trabajo realizado

1. Se localizaron entidades, controladores, EJB, servicios de selección,
   formularios, listas y permisos relacionados con persona, cliente y proveedor.
2. Se reconstruyó la relación entre el maestro `BswPersonas`, sus colecciones y
   los roles `CcwClientes`/`CcwProveedores`.
3. Se documentaron altas, modificaciones, búsquedas, estados, creación rápida,
   bajas y validaciones observadas.
4. Se separaron los datos neutrales de participante de ventas, compras, logística,
   tesorería, cuentas por cobrar/pagar, contabilidad, documentos y SIFEN.
5. Se identificaron duplicaciones y deudas que no deben heredarse: `MAX + 1`, SQL
   concatenado, baja física, validación de documentos sin implementar y campos de
   contacto con nombres incompatibles.
6. Se propusieron once casos de uso, cuatro permisos, diez invariantes y diez
   decisiones de producto.
7. Se registraron riesgos para una migración futura, que queda fuera de Sprint 6.

## Hallazgos determinantes

- Una persona puede ser simultáneamente cliente y proveedor; ambos son roles, no
  maestros independientes.
- Persona, cliente y proveedor tienen códigos diferentes en el modelo legado,
  aunque algunos flujos los igualan inicialmente.
- El rol cliente acumula crédito, precios, vendedor, cobrador y rutas; el proveedor
  acumula finanzas, bancos, contabilidad y especializaciones logísticas. Esas
  responsabilidades se excluyen del plugin base.
- RUC, cédula, correo, teléfono y dirección poseen representaciones duplicadas.
- La búsqueda útil está aislada por empresa y considera código, nombre, RUC y
  cédula.
- Los permisos visibles son de pantalla/ABM. El nuevo plugin requiere permisos
  públicos y autorización de servidor por operación.
- La baja física observada no es compatible con la conservación histórica que
  necesitarán documentos y movimientos futuros.

## Alcance funcional candidato

`business_partners` administrará, si las decisiones son aceptadas:

- persona natural u organización;
- identidad técnica opaca y código legible;
- nombres visible, legal y comercial según tipo;
- identificaciones externas;
- canales de contacto, direcciones y contactos nominales;
- roles cliente y proveedor coexistentes, con estado independiente;
- búsqueda, consulta, alta, modificación, inactivación y reactivación;
- vista pública mínima por ID para otros plugins.

No administrará crédito, saldo, precios, vendedor, cobrador, rutas, bancos, cuentas
contables, condiciones comerciales, documentos ni artefactos SIFEN.

## Casos de uso candidatos

| ID | Nombre |
|---|---|
| BP-UC-01 | buscar participantes por criterios de la empresa actual |
| BP-UC-02 | consultar detalle autorizado |
| BP-UC-03 | registrar persona natural u organización |
| BP-UC-04 | modificar con control de versión |
| BP-UC-05 | administrar identificaciones |
| BP-UC-06 | administrar direcciones y canales de contacto |
| BP-UC-07 | administrar contactos nominales |
| BP-UC-08 | asignar, activar o inactivar roles |
| BP-UC-09 | inactivar o reactivar el participante |
| BP-UC-10 | resolver una referencia pública mínima |
| BP-UC-11 | advertir posibles duplicados para revisión humana |

El detalle, actores y resultados esperados están en la
[base de conocimiento](../../knowledge-base/business-partners/legacy-characterization.md#casos-de-uso-neutrales-candidatos).

## Decisiones aceptadas para `J11-S6-02`

El responsable de producto confirmó BP-D01 a BP-D10 sin cambios el 2026-07-29:

1. permitir participantes sin rol comercial;
2. usar código general obligatorio y códigos de rol opcionales;
3. admitir código manual o secuencia transaccional, nunca `MAX + 1`;
4. advertir duplicados de RUC/cédula antes de imponer unicidad fuerte;
5. no exigir correo a todo participante;
6. modelar contactos nominales como hijos livianos en el primer corte;
7. representar geografía con códigos estándar y texto sin inventar un módulo
   compartido;
8. limitar estados iniciales a activo/inactivo y dejar crédito fuera;
9. usar inactivación, sin baja física normal;
10. excluir migración del legado e integración DNIT del Sprint.

Las decisiones completas BP-D01 a BP-D10 y su impacto están en la
[matriz de decisiones](../../knowledge-base/business-partners/legacy-characterization.md#decisiones-pendientes).

## Criterios de aceptación

- **CA-01:** las fuentes del legado están identificadas y se consultaron en modo de
  solo lectura. **Cumplido.**
- **CA-02:** existe glosario neutral y no se trasladan nombres como contratos.
  **Cumplido.**
- **CA-03:** alta, consulta, modificación, búsqueda, estados y baja están
  caracterizados. **Cumplido.**
- **CA-04:** cliente/proveedor se analizan como roles coexistentes. **Cumplido.**
- **CA-05:** datos neutrales y responsabilidades de otros plugins están separados.
  **Cumplido.**
- **CA-06:** casos de uso, permisos, invariantes, riesgos y decisiones están
  documentados. **Cumplido.**
- **CA-07:** el responsable de producto acepta o modifica BP-D01 a BP-D10.
  **Cumplido: confirmadas sin cambios el 2026-07-29.**
- **CA-08:** la caracterización aceptada autoriza explícitamente `J11-S6-02`.
  **Cumplido.**

## Pruebas y validación

No se modificó código; por tanto no correspondía ejecutar Maven, PostgreSQL,
Docker o Playwright. El gate aplicable es documental: existencia, trazabilidad,
enlaces locales y UTF-8. Su resultado queda en la
[evidencia de J11-S6-01](../../evidence/J11-S6-01-caracterizacion-business-partners.md).

## Resultado

La inspección, transformación a requisitos y aceptación están completas.
`J11-S6-02` queda autorizada para implementar dominio neutral y contratos públicos
versionados. Persistencia, migraciones, JPA y UI continúan fuera de ese alcance.
