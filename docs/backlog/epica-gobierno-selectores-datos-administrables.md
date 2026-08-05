# Épica - Gobierno de selectores y datos administrables

- Estado: En progreso; SEL-01 y metadatos 91/91, retorno seguro de plugins y los 11 usos nativos administrables, altas de SEL-03/SEL-05, ciclo activo/inactivo, revisión/historial append-only y reemplazo seguro de definiciones simples, ciclo/revisión/historial de perfiles, cuatro clases de definiciones de socios y familias, asignación versionada de familias a artículos y subconjunto normativo `PY/PYG/USD` implementados; publicación completa, paginación y cierre pendientes
- Fecha: 2026-08-04
- Decisión: [ADR-0028](../adr/0028-gobierno-de-selectores-y-datos-administrables.md)
- Fuente: [inventario del baseline](../architecture/inventario-selectores-y-datos-administrables.md)
- Prioridad: resolver antes de iniciar `purchasing`

## Objetivo

Garantizar que todo selector indique de dónde obtiene sus opciones y que todo
catálogo empresarial tenga una administración autorizada, auditable y descubrible,
sin convertir estados cerrados ni códigos normativos en texto libre.

## Historias propuestas

| Historia | Resultado |
|---|---|
| SEL-01 | contrato neutral versionado para fuente, clase, propietario, ruta y permiso — plugins desde `plugin-api` 0.4.1 y propietarios de plataforma/nativos 18/18 desde 0.4.2 |
| SEL-02 | renderer JSF Material Design 3 con acción contextual y retorno seguro implementado para plugins y los 11 usos nativos administrables |
| SEL-03 | centro de definiciones de catálogo: altas de unidades, categorías, marcas, etiquetas y familias implementadas; ciclo activo/inactivo, revisión permitida e historial append-only de todos esos maestros, reemplazo seguro de las cuatro definiciones simples y asignación visual/versionada de familias a artículos implementados |
| SEL-04 | perfiles tributarios: consulta, alta, revisión explícita de contenido/vigencia, historial visual de solo lectura e inactivación/reactivación versionada implementadas |
| SEL-05 | tipos de canal, tipos de identificación y tipos/propósitos de dirección con consulta, alta, revisión de nombre, historial visible append-only e inactivación/reactivación versionada implementados mediante V4 privada |
| SEL-06 | ADR-0038, `reference_data`, procedencia y selectores de país/moneda implementados para `PY/PYG/USD`; publicación completa y políticas administrables pendientes |
| SEL-07 | enlaces contextuales para empresas, usuarios, roles, depósitos, ubicaciones y artículos — metadatos, autorización y retorno validados |
| SEL-08 | conservación acotada de borrador implementada para plugins y nativos administrables; búsqueda/paginación, vacíos y opciones inactivas pendientes |
| SEL-09 | permisos, auditoría, seguridad negativa y límites arquitectónicos |
| SEL-10 | demo acumulada, manuales, fotografía, PDF y evidencia de cierre |

La división definitiva por Sprint se hará antes de tocar código. No se comprime en
una clase utilitaria global ni en una tabla genérica compartida por todos los
plugins.

## Criterios de aceptación

- **SEL-CE01:** cada `SELECT` declara clase y fuente estable.
- **SEL-CE02:** cada catálogo empresarial tiene propietario, ruta y permiso.
- **SEL-CE03:** el actor autorizado puede crear/administrar y volver al formulario
  sin perder un borrador válido.
- **SEL-CE04:** el actor sin permiso no ve el atajo y el servidor rechaza la
  operación directa.
- **SEL-CE05:** estados cerrados no ofrecen alta arbitraria y explican su gobierno.
- **SEL-CE06:** valores inactivos no aparecen en altas nuevas, pero las referencias
  históricas continúan legibles.
- **SEL-CE07:** opciones usan ID/código estable; el label no es identidad.
- **SEL-CE08:** listas grandes se buscan/paginan sin descargar el catálogo completo.
- **SEL-CE09:** plugins no acceden a tablas ni entidades privadas de otro.
- **SEL-CE10:** compacto, medio y expandido funcionan por teclado y sin overflow.
- **SEL-CE11:** la demo cubre valor nuevo, retorno, refresco, inactivación, historial
  y seguridad negativa.
- **SEL-CE12:** manual de usuario, manual técnico, guía de implementación, gráfico
  de dependencias y PDF quedan alineados con lo realmente disponible.

## Fuera de alcance

- editor universal de cualquier enum o columna;
- carga dinámica de plugins o permisos inventados por el usuario;
- reemplazar catálogos oficiales por códigos libres;
- compartir entidades JPA entre plugins;
- afirmar que las brechas están resueltas antes de implementar y probar las rutas.
