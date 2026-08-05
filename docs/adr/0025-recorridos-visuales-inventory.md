# ADR-0025 - Recorridos visuales de `inventory`

- Estado: Aceptado
- Fecha: 2026-07-31
- Historia: `J11-S8-05`

## Contexto

`inventory` reúne estructura física, inscripción de productos, disponibilidad,
movimientos, reservas y conteos. Colocar búsqueda, altas y todos los formularios en
una única página repetiría el desplazamiento excesivo y la falta de jerarquía que
ADR-0018 eliminó del shell. A la vez, separar cada comando en un menú distinto
fragmentaría el trabajo operativo y haría difícil encontrar el estado relacionado.

La aplicación de J11-S8-04 ya diferencia siete permisos y casos de uso. La UI debe
representarlos sin inventar autoridad, aceptar empresa desde el navegador ni
incorporar XHTML, CSS, JavaScript o EL procedentes del plugin.

## Decisión

1. El plugin publica tres pantallas interactivas:
   `inventory:stock` en `/inventory`, `inventory:warehouses` en
   `/inventory/warehouses` e `inventory:counts` en `/inventory/counts`.
2. Las tres usan el floorplan `directory`, `create` y `detail`. El usuario ve una
   tarea principal por vez; directorio, alta y ficha no se apilan.
3. `Existencias` usa el artículo inventariable como recurso de navegación. Su ficha
   resume físico, reservado y disponible y separa datos generales, consulta de una
   clave, movimientos y reservas en pestañas.
4. La primera UI contabiliza cantidades en la unidad base del artículo. El contrato
   público conserva conversiones completas para integraciones, pero la pantalla no
   pide al operador factor ni versión de catálogo.
5. `Depósitos` administra alta, nombre, ubicaciones y ciclo de vida. La ubicación
   `GENERAL` continúa siendo automática y protegida por dominio.
6. `Conteos` administra alcance, líneas, captura y transiciones. Contabilizar el
   cierre exige `inventory.adjustments.post`; preparar y revisar usa
   `inventory.counts.manage`.
7. El shell conserva un registro cerrado de rutas, regiones, pestañas y textos. El
   descriptor del plugin aporta únicamente contratos neutrales, menús y handlers.
8. Cada acción vuelve a solicitar empresa y permiso exactos. UUID, versión, modo,
   pestaña, claves idempotentes y demás valores del navegador se validan y nunca
   conceden autoridad.
9. Los tres contratos exponen slots tipados `directory_extensions` y
   `detail_extensions` para la personalización empresarial futura.
10. J11-S8-05 valida contratos, handlers, renderer y responsive estructuralmente.
    J11-S8-06 compone el JAR real, prepara datos ficticios y ejecuta Playwright.

## Alternativas descartadas

- Una única pantalla de inventario: mezcla maestros, operación y control, y vuelve
  a generar formularios largos difíciles de comprender.
- Un menú por cada entrada, salida, reserva o ajuste: fragmenta el contexto del
  artículo y multiplica navegación sin crear agregados distintos.
- Editar directamente el saldo: elimina trazabilidad y contradice el libro
  append-only aceptado.
- Solicitar factor y versión de catálogo al operador: expone detalles técnicos y
  permite inconsistencias; la UI inicial usa la unidad base conocida.
- XHTML propio del plugin: rompe la propiedad visual, responsive y accesible del
  shell.

## Consecuencias

- El menú fusionado agregará tres entradas únicamente cuando el plugin esté
  físicamente presente, activo y el rol tenga `inventory.view`.
- Se incorpora una proyección privada de lectura para directorios y totales; no
  crea tablas, FKs ni contratos públicos nuevos.
- `plugin-api` permanece en `0.4.0` y `inventory-api` en `1.0.0`.
- La composición física, fixtures, Docker y demo navegable continúan en J11-S8-06.

## Verificación

- descriptor con tres menús, pantallas, permisos y slots;
- búsquedas empresariales y totales sobre el esquema privado;
- handlers con autorización exacta, validación y casos de uso reales;
- renderer cerrado y XHTML único del shell;
- regresión de socios y catálogo;
- Playwright en 375, 720 y 1280 px y límites 599/600 y 839/840 una vez compuesta
  la candidata.
