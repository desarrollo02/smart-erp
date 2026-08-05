# ADR-0007 — Material Design 3 y pantallas responsive sobre Jakarta Faces

- Estado: Aceptado
- Fecha: 2026-07-28
- Historia: adenda transversal de `J11-S3-06` y criterio obligatorio desde `J11-S3-07`
- Reemplaza: ninguna decisión anterior

## Contexto

El primer shell de Logixone ya usa Jakarta Faces 4.1 con renderizado server-side y
CSS propio. El producto confirmó que todas las pantallas deben adoptar Material
Design y ser responsive, sin abandonar JSF. La regla debe ser común al kernel visual,
plugins funcionales y personalizaciones empresariales para evitar temas paralelos,
fragmentos incompatibles y vistas que sólo funcionen en escritorio.

Material Design se toma como sistema de diseño, no como autorización implícita para
incorporar una biblioteca. Los contratos neutrales de pantalla tampoco deben empezar
a transportar clases CSS, JavaScript, XHTML o detalles de un framework visual.

## Decisión

### 1. Tecnología y sistema de diseño

Jakarta Faces 4.1 continúa siendo la tecnología de interfaz server-side. Material
Design 3 define el lenguaje visual, los roles de color, tipografía, forma, elevación,
espaciado, componentes, estados e interacción.

La implementación inicial se realizará con HTML semántico producido por JSF y una
capa CSS propia centralizada en `web-shell`. No se agrega PrimeFaces, Material Web,
Web Components, biblioteca JavaScript, fuente remota ni paquete de iconos por esta
decisión.

Incorporar una biblioteca visual futura exige un ADR que documente versión, licencia,
necesidad no cubierta, compatibilidad con Jakarta Faces, impacto en accesibilidad,
seguridad, peso, actualización y mantenimiento.

### 2. Tokens y propiedad

El shell es dueño de los tokens `--md-sys-*`, layouts y renderers permitidos. Los
tokens representan roles semánticos y no nombres de una empresa concreta. Los
componentes deben consumir esos roles para que una evolución del tema no obligue a
reescribir cada vista.

Un plugin publica `ScreenDefinition`, elementos, slots y operaciones neutrales. El
plugin de personalización empresarial puede cambiar únicamente lo autorizado por
esos contratos. Ningún plugin inyecta CSS global, JavaScript, EL, beans o XHTML.

Una personalización de marca futura se resolverá mediante valores validados y
acotados que el shell convierta a tokens permitidos; no mediante una hoja de estilos
arbitraria.

### 3. Responsive obligatorio

Cada pantalla se diseña y acepta en los tres rangos definidos para Logixone:

| Rango | Ancho CSS | Comportamiento esperado |
|---|---:|---|
| Compacto | `0–599px` | una columna cuando corresponda, acciones alcanzables y sin desplazamiento horizontal normal |
| Medio | `600–839px` | composición adaptada para tableta y densidad intermedia |
| Expandido | `840px` o más | uso del espacio adicional sin estirar contenido hasta perder legibilidad |

Los rangos son una convención explícita del producto. Una vista puede agregar
breakpoints internos cuando su contenido lo justifique, pero no puede omitir ninguno
de estos tres escenarios.

Tablas, formularios, diálogos, navegación y grupos de acciones deben declarar su
estrategia compacta: reordenamiento, apilado, scroll contenido, resumen/detalle u otro
patrón adecuado. La página completa no debe depender de scroll horizontal para su
contenido normal.

### 4. Estados y accesibilidad

Cada componente interactivo debe representar al menos sus estados aplicables:
habilitado, deshabilitado, hover, foco y pulsado, además de carga, vacío, error o
denegación cuando correspondan. El estado no se comunica sólo mediante color.

Las vistas conservan HTML semántico, labels asociados, orden de encabezados,
navegación por teclado, foco visible, mensajes comprensibles, contraste suficiente y
respeto de `prefers-reduced-motion`. El área táctil y la separación de acciones deben
seguir siendo utilizables en compacto.

### 5. Criterios y verificación

Toda historia que cree o modifique una pantalla incluirá criterios de aceptación para:

1. los rangos compacto, medio y expandido;
2. teclado y foco;
3. estados funcionales y negativos relevantes;
4. ausencia de filtración entre empresas o personalizaciones;
5. ausencia de overflow horizontal de página en contenido normal.

Playwright ejecutará esas matrices antes del cierre. Durante la excepción de Sprint 3
pueden diferirse a `J11-S3-08`, pero la historia sólo queda `Implementada pendiente de
validación`.

## Alternativas consideradas

### Reemplazar JSF por una SPA

Se descarta. Agregaría otra arquitectura de sesión, autorización, build y
dependencias sin ser necesaria para demostrar composición server-side.

### Incorporar inmediatamente una biblioteca Material

Se descarta por ahora. El shell actual puede expresar los principios y tokens
necesarios sin aceptar todavía una nueva licencia, runtime JavaScript o superficie de
mantenimiento.

### Permitir que cada plugin entregue su propio tema

Se descarta porque rompe consistencia, responsive, accesibilidad, aislamiento y
personalización segura. El plugin aporta semántica; el shell controla la presentación.

### Diseñar primero para escritorio y adaptar después

Se descarta. Responsive forma parte del criterio de aceptación de cada pantalla, no
una fase posterior.

## Consecuencias

### Positivas

- lenguaje visual coherente entre shell, plugins y empresas;
- JSF y los límites server-side se conservan;
- la personalización puede evolucionar sobre roles validados;
- móvil, tableta y escritorio se consideran desde cada historia;
- no se agrega una dependencia o licencia de software en este corte.

### Costes y riesgos aceptados

- el shell debe mantener tokens, componentes y documentación visual;
- cada pantalla necesita tres escenarios de diseño y prueba;
- tablas y flujos densos del ERP requerirán patrones responsive específicos;
- decir “Material Design” no basta: la conformidad real depende de revisión visual,
  accesibilidad y pruebas pendientes.

## Plan de verificación

- inspección estática de tokens y ausencia de recursos remotos;
- empaquetado de `web-shell` y del WAR con Maven Wrapper;
- parseo de vistas XHTML;
- Playwright en `375px`, `720px` y `1280px` como viewports representativos de los
  tres rangos, además de límites `599px`, `600px`, `839px` y `840px`;
- prueba de teclado, foco, reduced motion, overflow horizontal y estados negativos;
- comparación A/B para asegurar que la personalización no rompe layout ni filtra
  estilos entre empresas.

`J11-S3-08` ejecutó la primera matriz runtime: Playwright comprobó las variantes A/B
en `375px`, `720px` y `1280px`, ausencia de overflow horizontal y revisión visual de
las capturas. Los límites exactos, teclado, foco y reduced motion permanecen como
criterios que cada pantalla futura debe ejercer según sus interacciones.

## Compatibilidad con decisiones anteriores

Este ADR especializa la UI server-side de
[ADR-0006](0006-identidad-oidc-membresia-autorizacion.md) y conserva el aislamiento
de [ADR-0002](0002-arquitectura-plugins.md) y
[ADR-0005](0005-contexto-empresarial-activacion-personalizacion.md). No cambia los
contratos neutrales de `plugin-api`, la autorización server-side ni el orden final de
la personalización empresarial.

## Referencias verificadas

- [Material Design 3](https://m3.material.io/), consultada el 2026-07-28.
- [Material Design 3 — canonical layouts](https://m3.material.io/foundations/layout/canonical-examples/overview), consultada el 2026-07-28.
- [Material Design 3 — interaction states](https://m3.material.io/foundations/interaction/states/overview), consultada el 2026-07-28.
