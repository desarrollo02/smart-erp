# ADR-0028 - Gobierno de selectores y datos administrables

- Estado: Aceptado
- Fecha: 2026-08-01
- Decisión de producto: todo selector debe declarar el origen de sus opciones y
  proporcionar administración visible cuando represente datos configurables
- Alcance: kernel, shell, plugins actuales, plugins futuros y personalizaciones

## Contexto

Un selector sin un origen comprensible obliga al usuario a aceptar una lista
cerrada aunque el dato sea empresarial. También induce atajos como valores
hardcodeados en handlers, texto libre que no coincide con el catálogo o tablas de
configuración sin permiso, historial ni interfaz.

La auditoría del baseline encontró 18 selectores nativos del shell/kernel y 51
campos `SELECT` declarados por los tres plugins funcionales. Varios están
correctamente cerrados porque representan estados o reglas del dominio; otros
consumen maestros que deben poder administrarse. La interfaz neutral actual puede
renderizar opciones, pero no declara aún su procedencia ni una acción contextual
para gestionarlas.

## Decisión

### 1. Clasificación obligatoria

Cada selector tendrá exactamente una clasificación documentada:

1. **Estado cerrado:** enumera estados, operaciones o políticas cuyo conjunto es
   parte de una regla versionada, por ejemplo activo/inactivo, tipo de movimiento o
   estado de conteo. No permite agregar valores desde la UI.
2. **Catálogo empresarial:** maestro propiedad de una empresa, por ejemplo unidad,
   categoría, marca, perfil tributario, depósito, medio de pago o centro de costo.
   Debe tener alta, consulta, modificación permitida e inactivación.
3. **Referencia operativa:** selecciona una entidad creada en otro recorrido, por
   ejemplo artículo, usuario, rol, depósito o línea de conteo. Debe enlazar al
   administrador propietario cuando el actor tenga permiso.
4. **Catálogo normativo:** lista publicada por una autoridad o estándar, por
   ejemplo país, moneda o catálogo fiscal. El usuario no inventa códigos, pero una
   administración autorizada puede habilitar, deshabilitar o actualizar una
   versión verificada.
5. **Composición/despliegue:** opción físicamente disponible por build, como una
   personalización. Se administra reconstruyendo y redesplegando; la pantalla debe
   explicarlo y nunca fingir instalación dinámica.

Un valor cerrado no se convierte en catálogo editable solamente para mostrar un
botón “Agregar”. Si el negocio necesita un estado nuevo, se cambia el dominio,
migración, contrato y pruebas mediante una historia versionada.

### 2. Contrato neutral del selector

La evolución del contrato de pantalla deberá asociar a cada campo `SELECT`:

- identificador estable de la fuente y plugin propietario;
- clasificación y versión de la fuente;
- ruta de administración y permiso requerido, cuando corresponda;
- capacidad de crear, editar, inactivar o solamente consultar;
- política de opción vacía, valores inactivos e historial;
- estrategia de búsqueda/paginación para listas grandes;
- texto de ayuda cuando el conjunto sea cerrado o dependa del despliegue.

El shell, y no el plugin, renderizará el patrón común. Si el actor está autorizado,
mostrará una acción contextual **Administrar** o **Agregar** junto al selector. Al
volver debe conservar el borrador seguro, refrescar opciones y permitir elegir el
nuevo valor. Sin permiso, el selector sigue operativo pero no expone el atajo.

El contrato permanece neutral: ningún plugin inyecta XHTML, EL, CSS o JavaScript.

### 3. Propiedad, historial y seguridad

- el plugin dueño del dato implementa comandos, consultas, esquema y migraciones;
- otro plugin consume IDs y contratos públicos, nunca tablas ni entidades JPA;
- las opciones se consultan por empresa y sólo incluyen valores admisibles;
- inactivar no elimina datos ni reescribe documentos o movimientos históricos;
- una referencia histórica inactiva continúa siendo legible y se distingue de una
  opción válida para operaciones nuevas;
- crear, modificar o inactivar exige permiso de aplicación y auditoría;
- labels no son identidad: se persiste el ID o código estable y el snapshot cuando
  el dominio histórico lo requiera;
- listas extensas usan búsqueda remota/paginada y no cargan miles de opciones en el
  HTML.

### 4. Datos controlados que todavía son texto

La revisión de una pantalla no se limita a los controles que ya son `SELECT`.
País, moneda, tipo de identificación u otro dato gobernado no puede permanecer
como texto libre sólo para evitar implementar su fuente. Debe clasificarse y
convertirse a selector/autocompletado controlado cuando la caracterización confirme
el catálogo.

La propiedad compartida de países y monedas requería una decisión posterior,
resuelta luego por ADR-0038. Este
ADR no agrega silenciosamente un plugin `reference_data` ni aumenta nuevamente el
roadmap productivo.

### 5. Gate para historias y Sprints

Toda historia visual nueva o modificada debe adjuntar un inventario de selectores
con fuente, propietario, clasificación, ruta, permiso, estado vacío/inactivo y
prueba. No puede terminar con un catálogo empresarial sin administración.

Cada cierre de Sprint revisará el inventario acumulado y demostrará:

- creación o administración desde una ruta descubrible;
- retorno y actualización de las opciones;
- seguridad negativa sin el permiso;
- preservación de referencias inactivas e históricas;
- responsive y teclado en 375, 720 y 1280 px;
- ausencia de listas hardcodeadas que contradigan un catálogo abierto.

Las brechas del baseline actual se registran en la
[auditoría de selectores](../architecture/inventario-selectores-y-datos-administrables.md)
y deben resolverse antes de iniciar `purchasing`.

## Consecuencias

### Estado de implementación 2026-08-03

`plugin-api` 0.4.2 distingue propietarios `PLUGIN` y `PLATFORM` mediante el
contrato común `SelectorSourceMetadata`. Los 59 selectores aportados por plugins
conservan `SelectorSourceDefinition`; los 18 controles directos de kernel/shell
publican `PlatformSelectorSourceDefinition` sin crear un plugin ficticio. Un
componente Faces propiedad del shell muestra origen/clase y sólo expone las rutas
nativas cuando la autoridad global actual contiene el permiso declarado. La
cobertura contractual queda 77/77. El renderer de plugins implementa retorno con
token opaco de un uso, borrador POST filtrado/normalizado, contexto efímero ligado
a sesión/usuario/empresa y refresco de opciones al volver. Los 11 usos nativos
administrables aplican el equivalente mediante una whitelist propia del shell,
binding a usuario/revisión de sesión y restauración explícita por formulario. Los
siete usos cerrados o de despliegue no crean contexto. El catálogo empresarial
`business_partners.channel_kind` permite además inactivar/reactivar con empresa,
versión esperada, auditoría y conservación física; las opciones inactivas permanecen
consultables pero no se ofrecen para nuevos canales. Edición, historia y ciclos
restantes, junto con publicación normativa completa, continúan como gates separados.

### Evolución de implementación 2026-08-04

La pantalla de definiciones del catálogo agregó los selectores cerrados
`definition_revision_unit_scale` y
`definition_revision_category_parent`, ambos con la misma fuente y propietario
que sus equivalentes de alta. El inventario ejecutable pasa a 61 selectores de
plugins y 18 nativos, 79/79 declarados. Unidades, categorías, marcas y etiquetas
ya permiten revisión explícita e historial append-only aislado por empresa; el
código y la identidad permanecen estables.

El decimoséptimo corte agrega los selectores cerrados
`definition_replacement_unit_scale` y
`definition_replacement_category_parent`. El inventario ejecutable pasa a 63
selectores de plugins y 18 nativos, 81/81 declarados. El reemplazo de una
definición simple crea una identidad sucesora del mismo tipo y empresa, inactiva
la anterior, conserva sus referencias históricas y registra un vínculo privado
unidireccional. Una definición ya reemplazada no puede revisarse, reactivarse ni
volver a reemplazarse.

El decimoctavo corte agrega `variant_revision_attribute_type` y
`variant_revision_attribute_required`. El inventario ejecutable pasa a 65
selectores de plugins y 18 nativos, 83/83 declarados. Una nueva revisión de familia
puede cambiar el nombre y reemplazar la estructura ordenada completa de atributos,
pero conserva empresa, identidad y código. V4 guarda cabecera y atributos por
versión en tablas append-only; las asignaciones existentes incorporan la versión
de familia y permanecen ligadas a la definición original. El ciclo activo/inactivo
continúa versionado y las altas futuras sólo admiten familias activas.

El decimonoveno corte agrega `item_variant_family`. El inventario ejecutable pasa
a 66 selectores de plugins y 18 nativos, 84/84 declarados. Artículos y servicios
ofrece únicamente familias activas de la empresa y conserva retorno contextual;
la aplicación vuelve a resolver y bloquear la revisión vigente, valida atributos
declarados, obligatorios y tipos, y persiste la revisión exacta. La UI no decide
el tipo ni puede convertir una familia ajena, inactiva u obsoleta en válida.

### Positivas

- los usuarios dejan de depender de SQL, datos seed o cambios de código para
  mantener maestros empresariales;
- cada lista conserva propietario, permiso y trazabilidad;
- un patrón único evita botones “+” inconsistentes en cada plugin;
- estados cerrados siguen protegidos contra configuraciones inválidas;
- las personalizaciones pueden cambiar presentación sin apropiarse del catálogo.

### Costes y riesgos

- `plugin-api` necesitará una evolución compatible del contrato de pantalla;
- la whitelist nativa debe mantenerse alineada con cada nuevo uso administrable y
  sus inputs seguros sin aceptar rutas o campos aportados por el navegador;
- varios maestros existentes tienen alta pero no modificación/inactivación;
- países y monedas quedan resueltos por ADR-0038 para el subconjunto inicial;
  publicación completa, políticas administrables y listas grandes siguen pendientes;
- pruebas de selector deberán cubrir listas vacías, grandes e inactivas.

## Alternativas descartadas

### Poner un botón “+” en todos los selectores

Se descarta porque permitiría inventar estados, permisos, códigos fiscales o
plugins físicos que requieren reglas y despliegue.

### Permitir texto libre como escape

Se descarta porque produce duplicados, errores ortográficos, referencias no
resolubles e historia ambigua.

### Crear un catálogo global dentro del kernel

Se descarta por ahora porque el kernel no debe acumular maestros empresariales. La
propiedad de datos normativos compartidos se resolvió explícitamente en ADR-0038.

## Actualización 2026-08-04

[ADR-0038](0038-plugin-datos-referencia-normativos.md) resuelve la decisión que
este ADR dejó abierta: `reference_data` es un plugin funcional R0 con API Java
pura, esquema privado y publicaciones trazables. País y moneda pasan a selectores
normativos en `business_partners` y `commercial_catalog`; el kernel no adquiere
maestros. El corte `PY/PYG/USD` es `BOOTSTRAP_SUBSET`, no una publicación mundial
completa.

## Referencias

- [Inventario de selectores y datos administrables](../architecture/inventario-selectores-y-datos-administrables.md)
- [Épica de gobierno de selectores](../backlog/epica-gobierno-selectores-datos-administrables.md)
- [ADR-0017 - Interacción visual neutral](0017-interaccion-visual-neutral-de-plugins.md)
- [ADR-0038 - Datos de referencia normativos](0038-plugin-datos-referencia-normativos.md)
