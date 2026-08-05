# Épica — Personalización obligatoria por empresa

- Estado: Base empresarial, selección exclusiva, contrato neutral y adaptadores visuales A/B implementados y técnicamente verdes; cada empresa recibirá su plugin de personalización concreto al final de su composición funcional
- Fecha: 2026-07-27
- Origen: decisión explícita del usuario durante la planificación del Sprint 2

## Necesidad

Cada empresa puede requerir cambios propios sobre pantallas pertenecientes a plugins funcionales, por ejemplo sobre la pantalla de facturación. Esas diferencias no deben introducir bifurcaciones del producto, modificaciones directas en el plugin funcional ni condicionales por cliente dispersos por el código.

## Decisión de producto confirmada

1. Cada empresa debe tener exactamente un plugin de personalización propio.
2. El plugin de personalización es obligatorio para que una empresa quede operativa.
3. Un plugin de personalización pertenece a una sola empresa y no se comparte entre empresas.
4. La distribución puede contener varios plugins de personalización, pero para cada empresa solo es efectivo el que tenga asignado.
5. La personalización se aplica como última capa, después de todos los plugins funcionales efectivos.
6. Su objetivo principal inicial es modificar pantallas de otros plugins mediante contratos públicos y versionados de extensión; también podrá contener otros cambios específicos de la empresa cuando exista un contrato público equivalente.

## Modelo arquitectónico

### Tipo e identidad

El descriptor debe declarar explícitamente si el plugin es `FUNCTIONAL` o `CUSTOMIZATION`; no se inferirá por nombre ni paquete. El JAR de personalización declara su identidad técnica y compatibilidad, pero no incrusta el `CompanyId`: la relación exclusiva con una empresa pertenece al estado persistido del kernel.

### Asignación

El kernel conserva una relación uno a uno entre empresa y plugin de personalización. La asignación, sustitución y diagnóstico son transaccionales y auditables. Una personalización obligatoria no se desactiva por el flujo normal de activación; se reemplaza mediante una operación explícita que no puede dejar a la empresa en un estado intermedio válido sin personalización.

ADR-0005 decidió que un JAR asignado ausente o incompatible pone en cuarentena únicamente a la empresa afectada. `J11-S2-06` aplica esa política devolviendo una composición vacía y segura sin degradar silenciosamente a una interfaz estándar.

### Contrato de pantalla

Un plugin funcional que admita personalización publica un contrato estable compuesto, como mínimo, por:

- identificador de pantalla y versión del contrato;
- regiones o slots extensibles;
- identificadores estables de elementos;
- propiedades y acciones que admite personalizar;
- invariantes funcionales, de autorización y auditoría que ninguna personalización puede relajar.

El plugin de personalización declara un overlay tipado contra ese contrato. El compositor valida existencia, versión y compatibilidad antes de aplicarlo.

### Cambios permitidos

El contrato concreto de cada pantalla podrá autorizar, de forma explícita:

- textos, etiquetas y ayudas;
- visibilidad, habilitación o modo de solo lectura;
- requisitos más estrictos que el estándar;
- orden, agrupación y posición de elementos;
- valores iniciales, columnas, filtros y acciones declarados personalizables;
- fragmentos y recursos propios dentro de slots publicados;
- validadores o acciones adicionales mediante puertos públicos.

### Cambios prohibidos

La capacidad de modificar pantallas no autoriza al plugin de personalización a:

- importar beans, controladores, DTO, entidades, repositorios o clases internas de otro plugin;
- acceder directamente a tablas o esquemas privados de otro plugin;
- reemplazar archivos XHTML, clases o recursos mediante colisiones de classpath;
- inyectar CSS o JavaScript global que afecte pantallas no declaradas;
- eliminar controles de autorización, validaciones de negocio, auditoría o guardas operativas;
- aplicar su overlay a una empresa distinta de la asignada.

La pantalla es una capa de presentación. Ocultar o habilitar un elemento nunca sustituye la autorización y validación del caso de uso en el servidor.

### Personalizaciones no visuales

El plugin empresarial no queda limitado para siempre a pantallas. Futuras personalizaciones de reportes, validadores, flujos, cálculos o integraciones deberán incorporarse mediante puertos o contratos de extensión específicos, versionados y propiedad del plugin funcional correspondiente. Cada nuevo tipo de extensión requiere historia, pruebas y, si cambia límites arquitectónicos, ADR propio; la etiqueta `CUSTOMIZATION` no concede acceso general al sistema.

## Orden y compatibilidad

- Todos los plugins funcionales se resuelven antes que cualquier plugin de personalización.
- El compositor empresarial selecciona únicamente la personalización asignada a la empresa consultada y la aplica al final.
- La personalización declara dependencias y rangos compatibles de los plugins y contratos de pantalla que modifica.
- Una referencia inexistente, duplicada o incompatible produce un diagnóstico estable y nunca una aplicación parcial silenciosa.
- El reemplazo de una personalización exige validar previamente el catálogo completo y sus contratos objetivo.

## Entrega por etapas

### Sprint 2

- decidir mediante ADR las invariantes, disponibilidad y sustitución;
- modelar tipo de plugin, asignación uno a uno y orden final;
- persistir y proteger la asignación;
- introducir contratos neutrales de pantalla y overlays, sin Jakarta Faces ni PrimeFaces;
- demostrar el modelo con un plugin funcional de referencia y un plugin de personalización de referencia.

La pantalla es el primer tipo de extensión demostrado. `J11-S2-07` implementó definiciones versionadas, overlays tipados, composición atómica y dos personalizaciones de referencia. Otros tipos de personalización empresarial quedan para historias posteriores sobre la misma frontera arquitectónica.

### Antes de la primera pantalla productiva

- implementar el adaptador de composición para Jakarta Faces/PrimeFaces;
- definir precedencia entre plantilla estándar, contribuciones funcionales y overlay empresarial;
- verificar renderizado, accesibilidad y comportamiento con Playwright.

### En cada plugin funcional

- publicar las pantallas, slots, elementos e invariantes que admite personalizar;
- versionar esos contratos y documentar compatibilidad;
- probar la pantalla estándar y la variante personalizada;
- impedir que una personalización convierta una operación no autorizada en ejecutable.

## Medida de éxito

Dos empresas sobre la misma distribución reciben composiciones de pantalla distintas y deterministas. Cada una usa exclusivamente su plugin de personalización obligatorio; ninguna personalización accede a internos de otro plugin, cruza empresas ni puede debilitar la seguridad o las reglas del servidor.
