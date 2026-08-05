# Evidencia J11-S6-01 - Caracterización de `business_partners`

- Fecha: 2026-07-29
- Estado: Completa; aceptación de producto registrada
- Historia: [J11-S6-01](../sprints/sprint-06/J11-S6-01-caracterizacion-business-partners.md)
- Resultado: [base de conocimiento](../knowledge-base/business-partners/legacy-characterization.md)

## Alcance de la inspección

Se consultó únicamente el árbol
`C:\cosme\multienvios\miaterra\fuente\tag`. No se escribieron archivos en el
proyecto legado.

La búsqueda inicial encontró 302 rutas cuyos nombres contenían referencias a
persona, cliente, proveedor, contacto, dirección o tercero. Después se redujo el
análisis a las entidades, controladores, servicios y vistas directamente
relacionados con el maestro y sus dos roles comerciales.

## Fuentes contrastadas

| Área | Evidencia observada |
|---|---|
| persona | `BswPersonas`, `BswPersonasJuridicas`, controlador y formulario |
| hijos del maestro | `BswIdentPersonas`, `BswDirecPersonas`, `BswTelefPersonas` |
| cliente | `CcwClientes`, `CcwContactosClientes`, EJB, controlador y pantalla |
| proveedor | `CcwProveedores`, `CmwContactosProveedor`, EJB, controlador y pantalla |
| búsqueda | `SelectorPersonaService` y consultas por empresa |
| permisos | constantes de forma y controladores de consulta de permisos |

## Evidencias negativas relevantes

- `isDocDuplicado()` contiene un `TODO` y retorna siempre falso.
- el alta automática de código de cliente usa `MAX + 1`;
- la verificación de código de proveedor concatena SQL nativo;
- los controladores exponen eliminación física;
- desmarcar cliente/proveedor en persona no muestra una transición inversa;
- persona y sus hijos duplican RUC/cédula, dirección y canales de contacto;
- contactos de proveedor reutilizan campos con nombres incompatibles con el uso
  mostrado en la pantalla;
- entidades de rol contienen datos pertenecientes a numerosos dominios futuros.

Estos hallazgos justifican caracterizar comportamiento y no copiar clases o tablas.

## Archivos creados o modificados por la historia

- `docs/knowledge-base/business-partners/legacy-characterization.md`;
- `docs/sprints/sprint-06/J11-S6-01-caracterizacion-business-partners.md`;
- este documento de evidencia;
- índices, arquitectura, roadmap y guía de implementación relacionados.

No se creó módulo Maven, Java, SQL, migración, XHTML, CSS, imagen, volumen o dato.

## Validación aplicable

| Gate | Resultado |
|---|---|
| legado sin modificaciones | cumplido por procedimiento de solo lectura |
| trazabilidad fuente → observación → requisito | cumplido en la matriz de caracterización |
| separación entre dominios | cumplido en la matriz de responsabilidades |
| código y pruebas automatizadas | no aplica; no hubo cambios de código |
| aceptación BP-D01 a BP-D10 | confirmadas sin cambios por producto el 2026-07-29 |
| enlaces Markdown y UTF-8 | 147 Markdown, 560 enlaces locales, cero errores UTF-8 y cero enlaces rotos |

## Gate documental G0

Se recorrieron los Markdown bajo `docs/`, excluyendo herramientas, temporales y
artefactos de build. Cada archivo se decodificó con UTF-8 estricto, se buscaron
`U+FFFD` y secuencias comunes de texto dañado, y se resolvió cada enlace local desde
el directorio de su documento.

```text
MARKDOWN_FILES=147
BAD_FILES=0
LOCAL_LINKS=560
BROKEN_LINKS=0
```

El primer detector trató como daño una línea del manual de pruebas que contiene
literalmente el patrón didáctico `rg -n "\x{FFFD}|\x{00C3}|\x{00C2}" docs`. No era texto dañado.
Se restringió la búsqueda a secuencias de mojibake reales y se repitió todo el
gate; el resultado anterior es la repetición válida.

## Conclusión

La caracterización y sus diez decisiones fueron aceptadas. `J11-S6-02` queda
desbloqueada para dominio neutral y contratos públicos, sin adelantar persistencia,
migraciones, JPA o UI.
