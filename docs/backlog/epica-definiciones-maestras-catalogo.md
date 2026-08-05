# Épica - Definiciones maestras del catálogo

- Estado: En progreso
- Plugin propietario: `commercial_catalog`
- Primer corte: `J11-S8-C01`, perfiles tributarios
- Permiso rector inicial: `commercial_catalog.definitions.manage`
- Gobierno transversal: [ADR-0028](../adr/0028-gobierno-de-selectores-y-datos-administrables.md)
- Estado del corte actual: alta y consulta de perfiles, definiciones simples y
  familias de variantes implementadas; los perfiles admiten revisión explícita,
  ciclo activo/inactivo e historial visible; las definiciones simples admiten
  revisión, historial y reemplazo seguro; y las familias admiten ciclo, revisión
  estructural e historial append-only. La asignación visual valida en el servidor
  empresa, estado, revisión y estructura de la familia y conserva la revisión
  exacta en el artículo

## Problema

Los formularios operativos necesitan seleccionar perfiles tributarios, unidades,
categorías, marcas, etiquetas y familias de variantes. Un selector sin una vía
visible para administrar sus opciones parece cerrado y obliga a depender de datos
precargados, soporte técnico o cambios directos en la base.

La solución no será aceptar texto libre ni crear registros silenciosamente desde
cualquier formulario. Cada definición controlada tendrá un maestro propio,
autorizado y auditable. El selector podrá enlazar a ese maestro cuando el usuario
tenga permiso, conservando su búsqueda o el borrador cuando sea técnicamente
seguro.

## Principios del patrón

- el dato pertenece a la empresa activa y nunca se mezcla con otra empresa;
- consultar o asignar una definición no implica permiso para administrarla;
- crear, editar, versionar o inactivar revalida empresa, plugin y permiso en el
  servidor;
- los registros usados históricamente no se eliminan para “limpiar” un selector;
- los cambios incompatibles crean una nueva versión o una nueva definición;
- un selector muestra solamente opciones válidas para el contexto y explica por
  qué una opción no está disponible;
- la UI presenta “Administrar …” sólo a usuarios autorizados; no simula un alta
  mediante una opción especial dentro de la lista;
- las pantallas usan contratos neutrales, Jakarta Faces y el shell Material Design
  3, sin XHTML, CSS, JavaScript ni EL aportados por el plugin;
- todo recorrido es usable con teclado y responsive en 375, 720 y 1280 px.

## Orden de implementación

1. **Perfiles tributarios:** directorio, alta, detalle y consumo inmediato desde
   artículos. Es el patrón vertical iniciado por `J11-S8-C01`.
2. **Unidades y conversiones:** administración de unidades base y conversiones
   explícitas, evitando factores ambiguos o cambios retroactivos.
3. **Categorías:** jerarquía controlada, prevención de ciclos y conservación de
   referencias históricas.
4. **Marcas y etiquetas:** maestros livianos, búsqueda, normalización y control de
   duplicados.
5. **Familias de variantes:** alta guiada, atributos ordenados, ciclo,
   revisión estructural e historia implementados sin convertir el catálogo en
   EAV; asignación visual/versionada a artículos implementada.
6. **Afinado transversal de selectores:** búsqueda, estados vacíos, acceso al
   maestro autorizado, retorno al formulario y conservación segura del borrador.

Cada paso debe quedar verde antes de iniciar el siguiente. El orden reduce riesgo:
primero resuelve el bloqueo visible y tributario, luego datos estructurales usados
por más operaciones y finalmente la experiencia común de todos los selectores.

## Criterios transversales de aceptación

- existe una ruta y menú o acceso contextual inequívoco para administrar cada
  maestro incluido en el corte;
- usuarios sin permiso pueden usar opciones autorizadas, pero no ven ni ejecutan
  acciones de administración;
- altas repetidas, vigencias inválidas y referencias en uso producen mensajes
  comprensibles sin revelar SQL;
- la nueva definición aparece en los selectores sin reiniciar el servidor;
- inactivar una definición impide nuevos usos sin alterar documentos o registros
  históricos;
- las pruebas cubren empresa A/B, autorización negativa, concurrencia relevante,
  teclado y los tres anchos responsive;
- manual de usuario, manual técnico, guía de implementación y demo se actualizan
  en el mismo corte.

## Límites tributarios

El maestro de perfiles tributarios representa clasificaciones internas del ERP.
No equivale por sí solo a una tasa oficial, regla de determinación, exención ni
configuración SIFEN certificada. La correspondencia por país, vigencia, régimen,
tipo de operación y versión normativa pertenecerá al plugin fiscal correspondiente
y se relacionará mediante contratos e identificadores públicos.
