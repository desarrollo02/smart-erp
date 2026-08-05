# ADR-0005 — Contexto empresarial, activación y personalización obligatoria

- Estado: Aceptado
- Fecha: 2026-07-27
- Historia: `J11-S2-01`
- Reemplaza: ninguna decisión anterior

## Contexto

El Sprint 1 dejó un catálogo físico de plugins validado, determinista y neutral, pero todavía no existe empresa, contexto empresarial ni activación persistida. El producto agregó además una regla obligatoria: cada empresa tendrá un plugin de personalización distinto, capaz de extender pantallas y otras capacidades publicadas por plugins funcionales.

La decisión debe preservar simultáneamente:

- aislamiento entre empresas;
- límites físicos y de datos entre plugins;
- denegación segura ante configuración incompleta;
- disponibilidad de empresas sanas cuando otra tenga una personalización inválida;
- evolución reproducible desde el esquema `core` V1;
- futura integración con identidad sin confiar en datos HTTP no autenticados.

## Decisión

### 1. Identidad y ciclo de vida de empresa

`CompanyId` será un valor opaco basado en UUID. El modelo Java conservará el UUID como valor tipado e inmutable y su forma textual será la representación canónica en minúsculas con guiones. PostgreSQL usará el tipo nativo `uuid`.

El kernel genera el identificador mediante un puerto `CompanyIdGenerator`; el adaptador inicial utilizará UUID v4. Ningún nombre, RUC, código contable, hostname o dato aportado por una petición se incorporará al identificador.

El ciclo mínimo tendrá dos estados persistidos:

- `INACTIVE`: permite administración y preparación, pero ninguna operación funcional ni contribución empresarial es efectiva;
- `ACTIVE`: solicita operación normal, condicionada al estado operacional derivado.

Una empresa nueva se registra como `INACTIVE`. No existe borrado físico, fusión ni empresa predeterminada en Sprint 2. Cambiar a `INACTIVE` conserva activaciones, asignación, versiones y datos.

### 2. Personalización obligatoria desde el alta

Registrar una empresa exige seleccionar en la misma transacción un plugin físicamente presente, válido y de categoría `CUSTOMIZATION`. La relación se almacena en la fila de empresa como `customization_plugin_id NOT NULL UNIQUE`:

- el `NOT NULL` garantiza una personalización por cada empresa persistida;
- el `UNIQUE` impide compartir el mismo plugin de personalización entre empresas;
- el valor referencia un `PluginId`, no una entidad JPA ni una tabla de catálogo físico;
- el descriptor del JAR no contiene `CompanyId` ni datos comerciales de la empresa.

Puede haber plugins de personalización físicamente presentes y todavía no asignados para permitir alta, despliegue gradual o reemplazo. Un plugin asignado no se activa ni desactiva mediante el flujo de plugins funcionales.

Dar de alta una empresa requiere este orden operativo:

1. construir y probar una distribución que contenga su personalización;
2. desplegar el nuevo digest;
3. verificar que el catálogo físico sea válido;
4. registrar la empresa y la asignación en una sola transacción;
5. habilitar las capacidades funcionales y finalmente activar la empresa.

### 3. Categoría y reglas del catálogo

`PluginDescriptor` declarará obligatoriamente `PluginKind.FUNCTIONAL` o `PluginKind.CUSTOMIZATION`. La categoría no se infiere del nombre, paquete, capacidades ni ubicación Maven.

La categoría forma parte de la identidad semántica del plugin y no puede cambiar entre versiones bajo el mismo `PluginId`. Cambiarla exige un identificador nuevo y un procedimiento de sustitución.

Para garantizar la capa final:

- un plugin funcional no puede depender de uno de personalización;
- un plugin de personalización no puede depender de otro de personalización;
- una personalización puede declarar dependencias requeridas u opcionales hacia plugins funcionales;
- el orden conserva primero la topología completa de plugins funcionales y después las personalizaciones en orden determinista;
- en una composición empresarial solo participa la personalización asignada a esa empresa.

Agregar la categoría al descriptor es un cambio incompatible deliberado del contrato preliminar `0.1.0`. `J11-S2-02` deberá elevar `PluginApiVersion.CURRENT` a `0.2.0`, actualizar el plugin de referencia y demostrar que no existe un constructor o valor predeterminado que oculte la categoría.

### 4. Activación deseada y efectiva

La tabla de activación pertenece a `core` y solo acepta decisiones para plugins `FUNCTIONAL`. Su clave lógica es `(company_id, plugin_id)` y conserva como mínimo estado deseado, versión optimista y marcas temporales UTC.

Las reglas son:

- una fila ausente equivale a `DISABLED`;
- al desactivar una fila existente se conserva con estado `DISABLED`; no se elimina;
- retirar un JAR no altera la decisión persistida;
- si el mismo `PluginId` vuelve a una distribución compatible, se reevalúa la intención conservada;
- cambiar la categoría de un `PluginId` está prohibido y no transforma filas existentes;
- las dependencias opcionales no se habilitan automáticamente.

Un plugin funcional es efectivo para una empresa únicamente cuando:

1. la empresa existe, está `ACTIVE` y su estado operacional es disponible;
2. el plugin está físicamente presente y es `FUNCTIONAL`;
3. la decisión deseada está en `ENABLED`;
4. todas sus dependencias requeridas están presentes, son compatibles y efectivas para la misma empresa.

Habilitar una decisión exige que sus dependencias requeridas estén también deseadas y sean físicamente válidas. Deshabilitar se rechaza si otro plugin deseado o la personalización asignada lo requiere. Esto mantiene configuraciones preparadas de empresas `INACTIVE` igualmente consistentes.

### 5. Estado operacional y readiness

El estado persistido `ACTIVE` expresa intención administrativa; la disponibilidad empresarial es derivada y no se guarda como segunda fuente de verdad.

Una empresa `ACTIVE` queda operacionalmente no disponible cuando su personalización:

- no está físicamente presente;
- no declara categoría `CUSTOMIZATION`;
- está asignada también a otra empresa;
- tiene una dependencia requerida no efectiva para esa empresa;
- contiene un overlay aplicable inválido o incompatible.

En cualquiera de esos casos se deniegan antes de ejecutar lógica todas las operaciones, menús, permisos, tareas y composiciones de esa empresa. No se entrega silenciosamente la pantalla estándar.

La política de salud distingue dos niveles:

- liveness no cambia y nunca consulta catálogo, empresas o PostgreSQL;
- readiness global queda `DOWN` ante catálogo físico inválido, configuración inválida, base inaccesible, migración pendiente o fallo de validación JPA;
- una asignación empresarial ausente, retirada o incompatible no baja por sí sola readiness global: pone únicamente a la empresa afectada en estado no disponible.

La aplicación registra y mide el diagnóstico empresarial sin exponerlo en el endpoint público de salud. Esta cuarentena evita que una configuración de una empresa retire del balanceador instancias capaces de atender a las demás. Incluso si ninguna empresa está disponible, el proceso puede permanecer listo para diagnóstico y recuperación administrativa; la aptitud de una operación siempre se verifica por empresa.

Un descriptor físicamente presente que invalide el catálogo común sí afecta readiness global, aunque el plugin todavía no esté asignado.

### 6. Contexto empresarial y frontera de confianza

`kernel-api` contendrá `CompanyId` y un puerto neutral de lectura de contexto empresarial. El contrato no ofrecerá setters globales ni dependerá de HTTP, CDI, seguridad Jakarta o almacenamiento local de hilo.

- los comandos administrativos reciben `CompanyId` explícito y no lo deducen de estado global;
- las operaciones funcionales obtienen el identificador desde un adaptador confiable del puerto de contexto;
- `CompanyId` identifica el ámbito, pero nunca constituye prueba de autorización;
- todo caso de uso vuelve a validar empresa, estado operacional, plugin y permiso en el servidor.

Durante Sprint 2 solo existirán adaptadores de prueba o arneses internos ausentes del WAR normal. Un header, parámetro, cookie, subdominio o campo JSON no autenticado no es una fuente confiable. Sprint 3 podrá establecer el contexto después de autenticar identidad, pertenencia y autorización.

### 7. Concurrencia, idempotencia y transacciones

Empresas y decisiones de activación utilizarán versión optimista `BIGINT`. Todo comando que cambie estado recibirá la versión observada.

- si el estado solicitado ya coincide con el actual, el resultado es `UNCHANGED`, no incrementa versión y puede auditarse como intento idempotente;
- si el estado difiere y la versión esperada no coincide, se devuelve un conflicto tipado sin escribir;
- activar, desactivar, cambiar estado empresarial y sustituir personalización son transacciones independientes y atómicas;
- un fallo de validación, concurrencia, persistencia o auditoría obligatoria revierte la transacción completa.

La personalización se sustituye mediante una operación dedicada:

1. cargar y bloquear/versionar la empresa;
2. comprobar que el nuevo `PluginId` es distinto, está presente, es `CUSTOMIZATION` y no pertenece a otra empresa;
3. validar dependencias y contratos contra la composición empresarial prevista;
4. actualizar una sola vez `customization_plugin_id` y la versión;
5. emitir auditoría después de confirmar según el mecanismo transaccional adoptado.

Ante cualquier fallo se conserva la asignación anterior. El despliegue seguro contiene temporalmente ambas personalizaciones; primero se cambia la asignación y solo una distribución posterior retira el JAR antiguo. El rollback invierte la asignación mientras ambos JAR sigan presentes.

### 8. Diagnósticos y auditoría

El dominio y la aplicación usarán códigos estables, al menos:

- `COMPANY_NOT_FOUND`;
- `COMPANY_INACTIVE`;
- `COMPANY_VERSION_CONFLICT`;
- `PLUGIN_NOT_PRESENT`;
- `PLUGIN_NOT_FUNCTIONAL`;
- `PLUGIN_DISABLED`;
- `REQUIRED_DEPENDENCY_NOT_EFFECTIVE`;
- `ACTIVE_DEPENDENT_EXISTS`;
- `CUSTOMIZATION_REQUIRED`;
- `CUSTOMIZATION_NOT_PRESENT`;
- `CUSTOMIZATION_WRONG_KIND`;
- `CUSTOMIZATION_ALREADY_ASSIGNED`;
- `CUSTOMIZATION_INCOMPATIBLE`;
- `CUSTOMIZATION_CONTRACT_INVALID`;
- `CUSTOMIZATION_VERSION_CONFLICT`.

Un llamador funcional no autorizado recibirá una denegación genérica y no podrá usar los códigos para enumerar otras empresas. Los detalles administrativos solo estarán disponibles mediante un adaptador autorizado futuro.

Los eventos de auditoría contendrán identificadores técnicos, operación, resultado, código, versión anterior/nueva, instante UTC y correlación cuando exista. Antes de Sprint 3 el actor será explícitamente `SYSTEM` o `TEST`; no se inventará usuario. No se registran nombres comerciales, payloads de pantalla, SQL, secretos ni datos personales innecesarios.

### 9. Contratos públicos de pantalla

El mecanismo común de pantalla residirá en `plugin-api` como Java puro. Los puertos empresariales específicos de una acción o validador residirán en el módulo API del plugin funcional propietario, nunca en su implementación.

El contrato neutral incluirá, como mínimo:

- `ScreenId` compuesto por plugin propietario e identificador local estable;
- versión explícita del contrato de pantalla;
- elementos y slots con identificadores estables;
- operaciones de personalización permitidas y restricciones no relajables;
- overlay inmutable declarado por el plugin `CUSTOMIZATION`.

La composición sigue este orden:

1. definición base del plugin propietario;
2. extensiones funcionales públicas, si una historia futura las autoriza;
3. overlay del único plugin de personalización asignado.

Todo está cerrado por defecto. Una pantalla solo puede permitir cambios declarados como texto, ayuda, visibilidad, habilitación, solo lectura, mayor exigencia de obligatoriedad, orden dentro de una región o contenido propio en un slot. Una personalización nunca puede:

- relajar un campo requerido por el servidor;
- habilitar una operación no autorizada;
- suprimir validación de dominio, auditoría o guarda;
- referenciar clases, beans, expresiones EL o rutas internas del plugin propietario;
- reemplazar XHTML, clases, CSS o JavaScript mediante colisiones;
- acceder a entidades, repositorios o tablas ajenas.

Las estructuras inválidas dentro de un descriptor se rechazan con el catálogo global. Las referencias cruzadas de overlay se validan para la composición empresarial; un fallo pone en cuarentena solo a esa empresa y no produce resultado parcial.

### 10. Propiedad por módulo

| Elemento | Propietario |
|---|---|
| categoría, descriptor y contratos técnicos de pantalla | `plugin-api` |
| `CompanyId` y puerto público de contexto | `kernel-api` |
| empresa, activación efectiva, asignación, reglas y diagnósticos internos | `kernel-domain` |
| comandos, resultados, repositorios como puertos y guardas | `kernel-application` |
| entidades JPA, repositorios PostgreSQL, JTA, CDI y contexto runtime | `kernel-infrastructure-jakarta` |
| adaptación futura a Jakarta Faces/PrimeFaces | `web-shell` y adaptadores UI de cada plugin |
| reglas empresariales y puertos de extensión específicos | plugin funcional propietario |
| overlays y comportamiento exclusivo de una empresa | plugin de personalización asignado |

El kernel no depende de implementaciones de plugins y una personalización no recibe privilegios arquitectónicos especiales.

### 11. Evolución de datos y recuperación

V1 permanece byte por byte inmutable. V2 será aditiva y creará las estructuras de empresa y activación dentro de `core`.

- `core.company` contiene `company_id uuid`, estado, `customization_plugin_id` no nulo y único, versión y marcas temporales;
- `core.company_plugin_activation` contiene la clave empresa/plugin, estado deseado, versión y marcas temporales;
- la clave foránea de activación solo apunta a `core.company`;
- no existe clave foránea ni entidad JPA hacia el catálogo físico o tablas de plugins;
- JPA usa `validate` y nunca crea, actualiza o elimina DDL.

Como V1 no contiene empresas, V2 no requiere inventar personalizaciones para datos existentes. El artefacto anterior puede ignorar las tablas aditivas durante un rollback, pero las migraciones y datos V2 no se eliminan. Antes de cualquier cambio destructivo futuro se exigirá respaldo y procedimiento específico.

## Alternativas consideradas

### Identificador secuencial o código comercial

Se descarta porque facilita enumeración, mezcla identidad técnica con negocio y complica importaciones. UUID conserva opacidad y generación independiente.

### Personalización compartida con configuración por empresa

Se descarta porque contradice la decisión de producto de un plugin distinto por empresa y volvería a concentrar condicionales de clientes en una implementación común.

### Tabla de asignación opcional separada

Se descarta para el primer modelo porque permitiría persistir empresas sin personalización. La columna no nula y única en `core.company` expresa directamente la cardinalidad obligatoria.

### Desactivar personalizaciones como plugins normales

Se descarta porque crearía una empresa aparentemente activa sin la capa que define su comportamiento contratado. Solo se admite sustitución validada.

### Readiness global en `DOWN` por cualquier empresa inválida

Se descarta porque una configuración empresarial defectuosa causaría indisponibilidad para todas las demás. Se adopta cuarentena empresarial con denegación total para la afectada.

### Empresa obtenida desde un header HTTP

Se descarta hasta que identidad y membresía autenticadas permitan validar la selección. Un identificador aportado por el cliente no demuestra autorización.

### Reemplazo directo de XHTML, beans o recursos

Se descarta porque depende de detalles internos, vuelve frágiles las actualizaciones, permite colisiones y puede omitir controles del servidor.

## Consecuencias

### Positivas

- cada empresa tiene una personalización inequívoca y aislada;
- el fallo de una personalización no causa una caída multiempresa;
- los plugins funcionales evolucionan detrás de contratos públicos versionados;
- activación, sustitución y recuperación tienen semántica transaccional;
- la futura identidad puede integrarse sin cambiar el dominio.

### Costes y riesgos aceptados

- cada empresa nueva exige construir, probar y redesplegar una imagen que contenga su JAR;
- la cantidad de JAR y combinaciones de compatibilidad crecerá con el número de empresas;
- una actualización de pantalla exige verificar todos los overlays compatibles antes de promoción;
- el cambio de `plugin-api` 0.1.0 a 0.2.0 requiere migrar en conjunto los plugins existentes;
- operadores necesitarán observabilidad por empresa además de readiness global;
- retirar un JAR antes de sustituir su asignación deja a esa empresa en cuarentena hasta recuperar el artefacto o completar el reemplazo.

Si la cantidad de empresas vuelve inviable el modelo de un JAR por empresa, cualquier cambio hacia configuración compartida, generación declarativa o carga dinámica necesitará un ADR nuevo y una estrategia de migración; no se anticipa en Sprint 2.

## Plan de verificación

- unitarias para UUID, estados, ausencia de fila, efectividad y orden;
- matriz de dos empresas con plugins funcionales y personalizaciones distintas;
- conflictos optimistas, idempotencia y rollback real;
- Testcontainers para unicidad, aislamiento y transacciones PostgreSQL;
- ArchUnit para Java puro, dirección de dependencias y prohibición de internos;
- builds con catálogo vacío y con plugins funcionales/personalizaciones de referencia;
- casos de cuarentena empresarial sin degradar readiness global;
- casos de catálogo físico inválido que sí degradan readiness;
- overlays válidos, incompatibles y prohibidos sin aplicación parcial;
- `mvnw.cmd -B clean verify` y evidencia por historia.

## Compatibilidad con decisiones anteriores

Este ADR especializa, sin reemplazar, [ADR-0002](0002-arquitectura-plugins.md) y [ADR-0003](0003-persistencia-migraciones.md). Mantiene composición física, activación lógica, contratos Java puros, propiedad del esquema `core`, migraciones inmutables y ausencia de relaciones JPA entre plugins.

La aceptación de este ADR cierra la decisión documental de `J11-S2-01` y autoriza iniciar `J11-S2-02` respetando su secuencia de pruebas.
