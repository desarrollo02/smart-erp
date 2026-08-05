# ADR-0016 - Autorización y auditoría de operaciones de plugins

- Estado: Aceptado
- Fecha: 2026-07-29

## Contexto

`business_partners` es el primer plugin productivo que necesita ejecutar comandos
y consultas con el actor y la empresa actuales. Un plugin no puede depender de
clases de `kernel-application`, `kernel-infrastructure-jakarta` ni internos del
shell; tampoco puede decidir autorización usando datos recibidos del navegador.

El kernel ya revalidaba por operación identidad OIDC, sesión empresarial,
membresía, empresa operacional, plugin efectivo y permiso vigente. Faltaba una
forma neutral de entregar ese resultado a adaptadores de plugins y una auditoría
central capaz de identificar recursos funcionales sin registrar datos personales.

## Decisión

1. `kernel-api` publica `CurrentCompanyAuthorization` y
   `AuthorizedCompanyOperation`. El shell implementa el primer contrato y emite el
   segundo únicamente después de `TrustedAccessPort.authorize`.
2. La autorización prueba exactamente un actor, empresa, plugin, permiso y
   correlación. No contiene token, cookie, claims del proveedor, roles ni listas de
   permisos reutilizables.
3. La aplicación del plugin recibe un contexto derivado de esa prueba y vuelve a
   exigir plugin y permiso exactos antes de acceder al repositorio. Consultar,
   administrar datos, administrar roles y cambiar ciclo de vida son permisos
   distintos.
4. `kernel-api` publica `TechnicalAudit`; los adaptadores de plugins envían un
   sobre técnico sin datos comerciales. `JpaTechnicalAuditStore` lo persiste en
   la auditoría append-only del kernel dentro de la transacción JTA.
5. La migración aditiva `core` V6 incorpora la categoría `PLUGIN_OPERATION` y
   `resource_type`/`resource_id`. Los valores son identificadores técnicos
   acotados; no nombres, documentos, direcciones, correos ni teléfonos.
6. Los plugins siguen siendo dueños de su aplicación y persistencia. La tabla de
   auditoría continúa siendo propiedad exclusiva del kernel.

## Alternativas descartadas

- Hacer que cada plugin dependa de `TrustedAccessPort`: viola la dirección de
  dependencias y expone internos del kernel.
- Guardar auditoría en una tabla privada por plugin: fragmenta una capacidad
  transversal y dificulta consulta, retención y controles append-only.
- Confiar en que la UI o el menú oculten acciones: no protege invocaciones directas
  ni cambios de autorización posteriores.
- Pasar todos los permisos del usuario al plugin: crea una autorización reutilizable
  y amplía innecesariamente el contexto sensible.

## Consecuencias

- Cada adaptador entrante funcional debe solicitar una autorización nueva para el
  permiso exacto y convertirla al contexto de su plugin.
- Las mutaciones y su auditoría confirman o revierten juntas; un fallo de auditoría
  impide confirmar la operación.
- Los rechazos de aplicación se expresan con códigos estables y pueden auditarse
  sin propagar mensajes internos o datos del comando.
- V6 es inmutable una vez aplicada. Las instalaciones en V1–V5 convergen al mismo
  esquema que una base vacía.
- La prueba neutral no sustituye el control de acceso del kernel: solo representa
  su decisión actual para una operación concreta.

## Verificación

- pruebas negativas de plugin o permiso incorrectos antes de tocar repositorios;
- aislamiento por empresa y conflicto optimista;
- auditoría sin datos comerciales;
- migración PostgreSQL desde base vacía y desde V1–V5, idempotencia y checksum;
- ArchUnit para impedir dependencias de aplicación hacia Jakarta o infraestructura.

