# ADR-0009 — Autoridad administrativa global y panel operativo del kernel

- Estado: Aceptado
- Fecha: 2026-07-28
- Historia: `J11-S4-00`
- Especializa: ADR-0005, ADR-0006 y ADR-0007

## Contexto

La candidata visual ya autentica personas, resuelve membresías, selecciona una
empresa y compone menús y pantallas desde plugins efectivos. El kernel también
contiene casos de uso tipados para empresas, activaciones, personalizaciones,
usuarios, membresías, roles y permisos, pero todavía no existe un adaptador
administrativo productivo que los orqueste.

Los datos de la demo se aprovisionan mediante automatización cerrada. Ese mecanismo
no debe convertirse en la operación cotidiana ni reemplazarse por SQL directo,
headers confiados, endpoints temporales o privilegios inferidos desde roles
empresariales. ADR-0006 ya determinó que la administración global será una
autoridad separada.

## Decisión

### 1. Autoridad global propiedad del kernel

El kernel será dueño de roles y permisos administrativos globales. No tendrán
`CompanyId` y no se derivarán de:

- roles o grupos genéricos de Keycloak;
- pertenecer a todas las empresas;
- conocer un identificador empresarial;
- variables enviadas por el navegador;
- ser el primer usuario que inicia sesión.

Keycloak continuará autenticando. La autorización administrativa exigirá una
identidad OIDC validada, un `AppUser` local activo y un permiso global efectivo
persistido en `core`.

### 2. Modelo global mínimo

La evolución `core` V4 será aditiva e incorporará:

- roles globales con código estable, nombre, estado, versión y marcas UTC;
- asignaciones entre usuarios locales y roles globales;
- concesiones de permisos administrativos a roles globales;
- restricciones de unicidad e integridad en PostgreSQL;
- auditoría del actor que crea, cambia o revoca autoridad.

Los permisos iniciales serán pequeños y explícitos:

- `kernel.company.manage`;
- `kernel.plugin.manage`;
- `kernel.security.manage`;
- `kernel.audit.view`;
- `kernel.system_administration.manage` para administrar la propia autoridad global.

Una ampliación futura deberá agregar códigos compatibles; no se usará un permiso
comodín implícito.

### 3. Primer administrador y protección contra bloqueo

El primer rol y administrador global se crearán mediante un bootstrap one-shot,
idempotente, cerrado por defecto y configurado externamente con la identidad OIDC
exacta. No contendrá contraseñas, no abrirá un endpoint anónimo y no concederá
autoridad al primer login.

Los casos de uso impedirán eliminar, desactivar o desasignar al último actor capaz
de administrar la autoridad global. La comprobación será transaccional y deberá
considerar concurrencia; no dependerá de que un botón permanezca visible.

### 4. Frontera web administrativa

El shell incorporará una zona `/admin/*` protegida por OIDC. Entrar a esa ruta,
mostrar navegación y ejecutar cada comando requieren autorización global en el
servidor. Un usuario empresarial sin permiso global recibirá denegación genérica
aunque manipule rutas, parámetros o formularios.

La UI continuará con Jakarta Faces y el sistema Material Design 3 definido en
ADR-0007. Cada pantalla será responsive en compacto, medio y expandido, tendrá
labels, foco visible, mensajes seguros y protección CSRF de Faces.

No se creará una SPA ni un API administrativo público en este Sprint. Si más
adelante aparece un consumidor no Faces, deberá reutilizar los mismos casos de uso
y guardas, definir autenticación de cliente y documentarse mediante otra historia
o ADR.

### 5. Operaciones expuestas

El panel podrá orquestar exclusivamente casos de uso del kernel:

1. registrar, activar e inactivar empresas;
2. consultar el catálogo físico de plugins sin modificarlo;
3. activar o desactivar plugins funcionales por empresa;
4. asignar o reemplazar la personalización exclusiva de una empresa;
5. registrar usuarios locales por identidad externa;
6. activar o inactivar usuarios y membresías;
7. administrar roles empresariales, permisos y asignaciones;
8. administrar roles y permisos globales con la protección del último administrador;
9. consultar auditoría con filtros acotados y sin datos sensibles.

Agregar o retirar físicamente un JAR continúa requiriendo reconstrucción y
redespliegue. El panel no implementará carga dinámica, edición de manifests ni
eliminación de tablas o datos de plugins.

### 6. Comandos, concurrencia y confirmaciones

Los backing beans serán adaptadores delgados. No contendrán reglas de negocio ni
accederán directamente a JPA. Cada mutación se expresará mediante comandos
tipados, versión optimista y servicio transaccional existente o nuevo del kernel.

Operaciones de mayor impacto —inactivar una empresa, reemplazar su personalización,
revocar autoridad o desactivar un plugin— requerirán una confirmación explícita y
mostrarán el efecto previsto sin enumerar datos ajenos. Los conflictos de versión
se traducirán en un mensaje recuperable; no se sobrescribirán cambios silenciosamente.

### 7. Auditoría y datos sensibles

Toda mutación registrará actor, operación, resultado, empresa cuando corresponda,
recurso técnico y correlación. No se registrarán tokens, cookies, contraseñas,
secretos, claims completos ni valores ingresados innecesarios.

La consulta de auditoría usará paginación y filtros cerrados. No se expondrán SQL,
stacktraces, entidades JPA ni detalles que permitan enumerar empresas o usuarios
fuera de la autoridad concedida.

### 8. Estado del kernel

Al terminar y validar Sprint 4 se podrá declarar completo el alcance del
**kernel operativo inicial**: capacidades transversales más su administración
segura. Esto no significará kernel definitivo ni plataforma aprobada para
producción. Nuevos requisitos transversales podrán evolucionarlo mediante contratos,
migraciones y ADR compatibles.

## Alternativas consideradas

### Usar un rol de realm de Keycloak como autoridad final

Se descarta porque trasladaría autorización funcional al proveedor, duplicaría
fuentes de verdad y no permitiría auditoría transaccional coherente con el kernel.

### Tratar a un administrador como miembro de todas las empresas

Se descarta porque confunde autoridad global con roles empresariales, facilita
filtraciones y obliga a crear membresías ficticias.

### Administrar mediante SQL o scripts permanentes

Se descarta porque evita invariantes, concurrencia, auditoría y contratos de
aplicación. Los bootstrap one-shot son únicamente el mecanismo inicial de arranque.

### Exponer primero endpoints REST administrativos

Se difiere. La prioridad actual es una operación visual coherente con la demo JSF.
Un API futuro deberá tener clientes, scopes, versionado y seguridad definidos.

### Permitir desactivar al último administrador

Se descarta porque dejaría la instancia sin recuperación operativa normal. Cualquier
recuperación extraordinaria deberá ser externa, explícita y auditada.

## Consecuencias

### Positivas

- elimina la necesidad operativa de SQL y endpoints temporales;
- separa claramente autoridad global y permisos empresariales;
- reutiliza casos de uso, JTA, auditoría y controles ya implementados;
- permite administrar personalizaciones sin violar la exclusividad por empresa;
- conserva OIDC como autenticación y el kernel como fuente de autorización;
- completa una frontera necesaria antes de iniciar dominios ERP productivos.

### Costes y riesgos

- V4 agrega datos globales sensibles y nuevas reglas de concurrencia;
- una UI administrativa amplía la superficie de ataque;
- reemplazar personalizaciones o revocar permisos puede afectar operación en curso;
- la protección del último administrador requiere pruebas transaccionales y
  negativas específicas;
- la candidata seguirá sin ser productiva aunque el panel sea navegable.

## Plan de verificación pendiente

Por decisión de producto, las historias de implementación de Sprint 4 quedarán
`Implementada pendiente de pruebas` hasta `J11-S4-08`. El gate acumulado deberá
cubrir:

- unitarias de roles/permisos globales y protección del último administrador;
- ArchUnit para impedir Jakarta/JPA en módulos puros y lógica en backing beans;
- PostgreSQL V1→V4, restricciones, concurrencia, rollback y reejecución;
- JPA `validate` y transacciones JTA;
- OIDC y acceso administrativo positivo/negativo;
- CSRF, manipulación de IDs, versiones obsoletas y enumeración;
- Playwright a 375, 720 y 1280 px;
- Docker/Compose, migraciones, health, persistencia y secretos;
- regresión completa de la demo A/B.

Una prueba ejecutada y fallida bloquea el avance. Sprint 4 no se cierra ni se
declara terminado el kernel mientras esta matriz permanezca pendiente.

## Compatibilidad

Este ADR conserva los límites de ADR-0002, la propiedad de datos y migraciones de
ADR-0003, la personalización obligatoria de ADR-0005, el modelo OIDC de ADR-0006 y
la UI responsive de ADR-0007. `plugin-api` continúa sin Jakarta y los plugins no
obtienen acceso a tablas privadas del kernel.
