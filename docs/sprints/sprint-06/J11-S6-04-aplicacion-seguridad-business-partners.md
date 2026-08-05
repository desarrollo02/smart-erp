# J11-S6-04 - Aplicación y seguridad de `business_partners`

- Estado: Completa
- Fecha: 2026-07-29
- Gate: G3 aplicación/seguridad
- Dependencia: J11-S6-03 verde

## Objetivo

Exponer casos de uso neutrales y transaccionales del primer plugin productivo sin
abrir todavía una pantalla o endpoint. Cada operación parte de una empresa y actor
autenticados, exige el permiso exacto del plugin y registra auditoría técnica sin
datos comerciales sensibles.

## Permisos públicos

| Permiso | Operaciones protegidas |
|---|---|
| `business_partners.view` | buscar, consultar detalle y resolver candidatos duplicados |
| `business_partners.manage` | registrar y modificar datos generales, identificaciones, direcciones y contactos |
| `business_partners.roles.manage` | asignar y cambiar el estado de cliente/proveedor |
| `business_partners.lifecycle.manage` | inactivar o reactivar el participante completo |

El descriptor declara los cuatro permisos. El permiso se vuelve efectivo solamente
si el kernel revalidó actor, empresa, plugin activo y asignación actual. La capa de
aplicación vuelve a comprobar que la autorización recibida pertenece a
`business_partners` y al permiso exigido; una empresa o permiso diferente se
rechaza antes de consultar o mutar el repositorio.

## Casos de uso incluidos

- búsqueda paginada por texto, identificación, rol y estado dentro de la empresa;
- detalle completo y directorio público mínimo;
- alta con código manual o secuencia transaccional;
- cambio de nombre o código con versión esperada;
- alta de identificación con advertencias de posibles duplicados, sin rechazo;
- alta de dirección, canal general y contacto nominal;
- asignación y cambio de estado independiente de cliente/proveedor;
- inactivación y reactivación sin borrado físico.

Corregir o vencer una identificación existente y editar/desactivar detalles se
mantienen fuera de este corte: requieren conservar historia de cada versión, no
sobrescribir filas como si nunca hubieran existido. La UI de J11-S6-05 no debe
simular esas capacidades.

## Auditoría

Las mutaciones exitosas o rechazadas generan un sobre técnico central con operación,
resultado, actor, empresa, plugin, permiso, identificador técnico del participante,
versiones y correlación. No se registran nombres, documentos, direcciones, correos,
teléfonos ni credenciales. La auditoría participa en la misma transacción JTA que
la mutación.

## Criterios de aceptación

1. aplicación, dominio, contratos y puertos no importan Jakarta ni infraestructura;
2. todas las consultas están aisladas por `CompanyId` y paginadas con límites;
3. cada comando exige su permiso específico y versión esperada cuando modifica;
4. un permiso, plugin o empresa incorrectos fallan antes de tocar repositorios;
5. el alta automática usa contador transaccional, nunca `MAX + 1`;
6. coincidencias de identificación son advertencias y no bloquean el alta;
7. las mutaciones auditan éxito y rechazo sin datos sensibles;
8. JPA/JTA, PostgreSQL, pruebas unitarias, ArchUnit y `mvn verify` quedan verdes;
9. no se agregan aún XHTML, menús, endpoints ni composición del WAR base.

## Resultado

La historia quedó completa con los casos de uso incluidos en el corte, cuatro
permisos públicos, autorización actual neutral, límite JTA, búsqueda paginada,
directorio público y auditoría central `PLUGIN_OPERATION`. La migración aditiva V6
quedó verificada desde base vacía y desde V1–V5; V1–V5 permanecen inmutables.

El reactor de 20 módulos ejecutó 229 pruebas unitarias sin fallos en 47,244 s. PostgreSQL real
ejecutó 14 escenarios del plugin, 12 del migrador y 13 del repositorio central del
kernel. ArchUnit ejecutó 13 reglas de límites. La evidencia reproducible se encuentra en
[`docs/evidence/J11-S6-04-aplicacion-seguridad-business-partners.md`](../../evidence/J11-S6-04-aplicacion-seguridad-business-partners.md).

No se agregó UI ni el plugin al WAR base. El siguiente corte autorizado es
`J11-S6-05`, primera pantalla visual del plugin.
