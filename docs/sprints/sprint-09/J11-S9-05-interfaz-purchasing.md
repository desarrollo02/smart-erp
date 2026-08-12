# J11-S9-05 — Interfaz neutral y responsive de `purchasing`

- Estado: Implementada y validada automáticamente; validación independiente pendiente
- Fecha: 2026-08-11
- Dependencia: J11-S9-04 implementada y validada automáticamente
- Decisión: [ADR-0044](../../adr/0044-recorridos-visuales-purchasing.md)
- Evidencia: [J11-S9-05](../../evidence/J11-S9-05-interfaz-purchasing.md)
- Manual: [Compras](../../user-guide/modules/compras.md)

## Objetivo

Entregar los recorridos de solicitudes, órdenes, recepciones, devoluciones y
seguimiento mediante contratos neutrales renderizados por el shell Jakarta Faces,
con datos gobernados, permisos exactos y documentación para una persona nueva.

## Alcance implementado

- cinco rutas, menús, definiciones de pantalla y handlers;
- alta, línea adicional y ciclo de solicitud, incluida copia con nuevas
  identidades;
- alta directa o asignada, línea adicional y ciclo de orden;
- preparación/confirmación de una línea de recepción o devolución;
- seguimiento de cantidades pedidas, recibidas, devueltas y pendientes;
- listados paginados y fichas de los cuatro agregados;
- búsqueda pública paginada de proveedores en `business-partners-api@1.1.0`;
- búsqueda paginada por alcance `PURCHASE` en `commercial-catalog-api@1.1.0`;
- búsqueda y recuperación exacta de depósitos/ubicaciones en
  `inventory-api@1.1.0`;
- fuente gobernada para cada selector y rutas Administrar del propietario;
- cinco especificaciones del renderer cerrado y textos de ayuda por control;
- manual 07 en fuente, web y PDF de 15 páginas.

## Criterios de aceptación

- **CA-01:** cada pantalla usa `ScreenDefinition` y `ScreenInteraction`; el plugin
  no aporta XHTML, CSS, JavaScript o EL.
- **CA-02:** las cinco rutas quedan registradas en el shell y el descriptor aporta
  menús sujetos a `purchasing.view`.
- **CA-03:** cada acción exige el permiso exacto en servidor, además de la
  visibilidad de UI.
- **CA-04:** todos los selectores declaran propietario, clase, permiso, ruta,
  política de vacío/inactivo y carga.
- **CA-05:** proveedor, artículo, moneda y almacenamiento se consultan solo por
  API pública y empresa autorizada.
- **CA-06:** las mutaciones preservan idempotencia y versión esperada.
- **CA-07:** las pantallas explican estados vacíos y errores recuperables sin
  exponer detalles sensibles.
- **CA-08:** el floorplan del shell permite apilar formularios y adaptar tablas en
  compacto, medio y expandido.
- **CA-09:** el manual define términos antes de usarlos, explica cada dato y
  contiene un bosquejo y diagrama de tablas por pantalla.
- **CA-10:** no se adelantan composición, demo oficial ni gates acumulados.

Los criterios ejecutables sin composición están cubiertos por pruebas de módulo,
shell, PostgreSQL y arquitectura. La validación responsive con Playwright se
ejecutará al componer estas rutas en J11-S9-06; la validación independiente sigue
pendiente.

## Pruebas automatizadas ejecutadas

El corte `.tools/tmp/validation/J11-S9-05-automated` ejecutó:

```powershell
.\mvnw.cmd -f .tools\tmp\validation\J11-S9-05-automated\pom.xml -pl plugins/purchasing -am test
.\mvnw.cmd -f .tools\tmp\validation\J11-S9-05-automated\pom.xml -pl web-shell -am test
.\mvnw.cmd -f .tools\tmp\validation\J11-S9-05-automated\pom.xml -pl tests/architecture-tests -am test
.\mvnw.cmd -f .tools\tmp\validation\J11-S9-05-automated\pom.xml verify
```

El módulo Compras, sus APIs y el shell terminaron verdes; `web-shell` ejecutó 59
pruebas, ArchUnit 32 y el `verify` completo recorrió 28 módulos. La primera
compilación detectó dos ternarios genéricos inferidos como `Object`; fueron
tipados explícitamente y la repetición quedó verde. Playwright 375/720/1280,
retorno Administrar y runtime de seguridad se activan en J11-S9-06, al existir
una URL de Compras navegable.

## Resultado

J11-S9-05 queda implementada y validada automáticamente dentro de su alcance no
compuesto. J11-S9-06 está habilitada para componer Compras en WAR/migrador y
ejecutar Docker/Compose, health, seguridad runtime y Playwright. Sprint 9 continúa
abierto y el corte todavía no es comercializable ni cuenta con aceptación
independiente.
