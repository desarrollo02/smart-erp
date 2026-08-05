# Retrospectiva técnica de Sprint 7

- Fecha: 2026-07-31
- Incremento: `commercial_catalog` integrado y demostrado junto con
  `business_partners`
- Estado: retrospectiva completada; G7 independiente permanece pendiente

## Funcionó bien

- separar API pública, dominio, persistencia, aplicación, UI y composición permitió
  probar cada frontera antes de sumar el siguiente nivel;
- reutilizar el floorplan directorio/alta/ficha evitó volver a una pantalla vertical
  enorme y mantuvo el mismo lenguaje visual entre plugins;
- derivar WAR y migrador de un único perfil eliminó la posibilidad de migrar tablas
  de un plugin ausente o desplegarlo sin su esquema;
- administrar activación y permisos desde las pantallas reales demostró que el menú
  fusionado no depende de enlaces escritos a mano;
- Playwright dejó datos ficticios persistidos y restauró las activaciones, lo que
  hizo verificable la conservación sin borrar el volumen.

## Hallazgos

- los identificadores Unicode de fixtures necesitan literales PostgreSQL
  independientes de la codificación del cliente para ser reproducibles;
- un fixture idempotente debe evitar actualizaciones vacías, no sólo conflictos de
  clave, para que una segunda ejecución informe cero cambios reales;
- la demo de catálogo necesita probar artículos y precios como tareas separadas;
  combinarlas volvería a introducir una vista difícil de comprender;
- una consulta incidental a `/api/health/*` recibió correctamente `401`; las rutas
  públicas semánticas son `/health/live` y `/health/ready` y deben documentarse de
  forma consistente;
- la página de auditoría crece verticalmente por diseño; mantener filtros,
  paginación y ancho correcto es preferible a ocultar eventos.

## Acciones acordadas

1. conservar `with-commercial-catalog-demo` como composición reproducible del
   corte y probar siempre también la variante base;
2. iniciar Sprint 8 con gobierno y caracterización de `inventory`, sin diseñar
   tablas hasta confirmar las decisiones IN-D01 a IN-D10;
3. definir la dependencia pública mínima desde inventario hacia catálogo, sin JPA,
   joins privados ni lectura directa de `plg_commercial_catalog`;
4. mantener stock, movimientos y reservas fuera de catálogo y fuera del kernel;
5. resolver G7 mediante un implementador independiente antes de promoción,
   publicación `1.0` o producción.
