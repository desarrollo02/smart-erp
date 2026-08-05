# J11-S4-05 — UI de empresas, plugins y personalización

- Estado: Completada; validada en `J11-S4-08`
- Sprint: 4
- Fecha: 2026-07-28
- Dependencias: `J11-S4-01` a `J11-S4-04` completadas
- ADR rector: [ADR-0009](../../adr/0009-autoridad-administrativa-global-kernel.md)

## Objetivo

Entregar pantallas Jakarta Faces, Material Design 3 y responsive para operar las
empresas, consultar el catálogo físico de plugins, administrar la activación
deseada de plugins funcionales y asignar o reemplazar la personalización
obligatoria y exclusiva de cada empresa.

La interfaz es una frontera administrativa del kernel. No instala JAR, no edita
manifiestos, no borra datos y no sustituye las validaciones del dominio ni los
casos de uso transaccionales existentes.

## Alcance funcional

1. Listar empresas con identificador técnico, estado, personalización y versión.
2. Registrar una empresa inactiva seleccionando una personalización física libre.
3. Activar una empresa solamente cuando su composición sea operativa.
4. Inactivar una empresa mediante confirmación explícita.
5. Consultar el catálogo físico validado: identidad, nombre, tipo, versión y
   dependencias declaradas.
6. Consultar por empresa el estado deseado de cada plugin funcional.
7. Activar o desactivar plugins funcionales mediante versión optimista y las
   reglas de dependencia existentes.
8. Reemplazar la personalización obligatoria mediante confirmación explícita,
   exclusividad y validación de compatibilidad.

## Límites y decisiones de seguridad

- `/admin/companies.xhtml` exige `COMPANY_MANAGE` en el servidor.
- `/admin/plugins.xhtml` exige `PLUGIN_MANAGE` para consulta y cambio de
  activaciones.
- Reemplazar una personalización exige además `COMPANY_MANAGE` en la propia
  acción, aunque la pantalla ya haya sido renderizada.
- Cada comando vuelve a autorizar el permiso exacto; la visibilidad de botones
  no constituye autorización.
- Identificadores, versiones y estados recibidos desde JSF son candidatos no
  confiables y se vuelven a resolver en el caso de uso.
- La ausencia, el acceso prohibido y un identificador manipulado producen
  mensajes externos genéricos que no permiten enumeración.
- Los beans JSF no acceden a JPA, no conservan entidades y no implementan reglas
  de negocio.
- No se expone una API administrativa pública.
- No se permite instalar, retirar o recargar plugins dinámicamente.
- No se elimina una decisión, migración, tabla ni dato al desactivar un plugin.
- La personalización no se administra como una activación funcional ordinaria.

## Contrato de aplicación requerido

La capa web depende de un puerto neutral de administración. El adaptador Jakarta
implementa la transacción, resuelve el catálogo CDI validado y delega en los
servicios neutrales ya existentes. Las proyecciones de consulta contienen sólo
datos necesarios para presentación y versiones de concurrencia.

La consulta de empresas debe ser determinista. El repositorio neutral incorpora
un listado ordenado por identificador; la implementación JPA no expone entidades
fuera de infraestructura.

## Experiencia visual y accesibilidad

- Material Design 3 sobre Jakarta Faces, sin migrar la UI a otro framework.
- Composición usable a 375, 720 y 1280 px.
- Formularios con etiquetas persistentes, ayuda contextual y errores legibles.
- Tablas se transforman en tarjetas o regiones desplazables en ancho compacto.
- Foco visible, navegación por teclado y jerarquía semántica de encabezados.
- Acciones destructivas lógicas o de alto impacto usan confirmación explícita.
- El estado vacío y los conflictos de versión son recuperables sin SQL manual.

## Criterios de aceptación

- **CA-01:** un usuario sin `COMPANY_MANAGE` no puede consultar ni mutar empresas.
- **CA-02:** un usuario sin `PLUGIN_MANAGE` no puede consultar ni cambiar
  activaciones por empresa.
- **CA-03:** registrar sin personalización física, con tipo incorrecto o ya
  asignada es rechazado por aplicación y no sólo por la UI.
- **CA-04:** toda empresa nueva queda `INACTIVE` y muestra la versión persistida.
- **CA-05:** activar una empresa no operativa informa un rechazo seguro.
- **CA-06:** inactivar requiere confirmación y conserva plugins y datos.
- **CA-07:** el catálogo físico es de sólo lectura y distingue `FUNCTIONAL` de
  `CUSTOMIZATION`.
- **CA-08:** las activaciones respetan dependencias, tipo y versión optimista.
- **CA-09:** reemplazar personalización exige `COMPANY_MANAGE`, confirmación,
  exclusividad y compatibilidad.
- **CA-10:** una versión obsoleta produce un mensaje recuperable y obliga a
  recargar el estado actual.
- **CA-11:** los backing beans no importan JPA ni clases internas de plugins.
- **CA-12:** las pantallas son navegables a 375, 720 y 1280 px.
- **CA-13:** no existe `DELETE`, carga de JAR ni edición arbitraria de archivos.
- **CA-14:** cada cambio aceptado o rechazado continúa auditado por los casos de
  uso existentes.

## Migraciones y datos

No se requiere una migración nueva. Se reutilizan `core.companies` y
`core.company_plugin_activation`; la auditoría técnica vigente continúa en logs
estructurados y recibe actor local y correlación de servidor. La persistencia y
consulta visual de auditoría se tratarán en `J11-S4-07`. La consulta y los
comandos conservan concurrencia optimista; no se cambia una migración aplicada.

## Documentación afectada

- este documento de historia;
- índice de Sprint 4;
- evidencia acumulada del Sprint 4;
- guía de implementación para administración de empresas y plugins;
- runbooks de shell y Compose para las nuevas rutas;
- navegación administrativa y siguiente trabajo autorizado.

## Matriz de pruebas ejecutada en J11-S4-08

La excepción de Sprint 4 acumuló para `J11-S4-08` los siguientes alcances, ya
ejecutados:

- unitarias de proyecciones, traducción de resultados y validación de entrada;
- integración JPA/JTA de listado, registro, estados, activaciones y reemplazo;
- concurrencia optimista y rollback;
- autorización positiva y negativa por cada permiso y comando;
- no enumeración con identificadores inexistentes o manipulados;
- ArchUnit para confirmar ausencia de JPA y dependencias internas en web;
- Playwright del recorrido a 375, 720 y 1280 px;
- regresión de la demo empresarial A/B y de persistencia tras recreación.

El gate acumulado terminó verde sin omisiones. La historia puede marcarse
completada; el Sprint continúa abierto por validación independiente.

## Resultado implementado

- `CompanyAdministrationPort` separa la UI del adaptador Jakarta y expone
  proyecciones administrativas sin entidades JPA.
- `CompanyAdministrationQueryService` lista empresas de forma determinista y
  compone estado deseado/efectivo de plugins funcionales.
- `JpaCompanyRepository.findAll()` materializa la consulta ordenada sin filtrar
  entidades hacia web.
- `TransactionalCompanyUseCases` implementa lecturas `SUPPORTS` y mutaciones JTA
  que delegan en los casos de uso neutrales existentes.
- `CompanyAuditContext` agrega `AppUserId` local y correlación de servidor a las
  acciones autenticadas; el log estructurado no incluye claims ni datos personales.
- `/admin/companies.xhtml` permite alta, activación e inactivación con versión y
  confirmación de impacto.
- `/admin/plugins.xhtml` permite seleccionar empresa, cambiar activaciones,
  reemplazar personalización con permiso adicional y consultar el catálogo físico.
- `AdminAuthorizationFilter` exige el permiso exacto al abrir cada nueva ruta y
  cada método de acción vuelve a autorizar antes del comando.
- `admin.css` adapta formularios, tarjetas y catálogo a anchos compacto, medio y
  expandido con tokens Material Design 3.

La evidencia reproducible se conserva en
[docs/evidence/J11-S4-05-ui-empresas-plugins-personalizacion.md](../../evidence/J11-S4-05-ui-empresas-plugins-personalizacion.md).

## Estado de aceptación

El código, la documentación y el WAR están empaquetados. Las pruebas automatizadas,
el arranque OIDC/PostgreSQL y la revisión Playwright a 375/720/1280 px quedaron
verdes en `J11-S4-08`. Esto completa la historia, pero no autoriza producción.
