# Retrospectiva técnica de Sprint 8

- Fecha: 2026-08-01
- Incremento: `inventory` integrado y demostrado con catálogo y socios
- Estado: retrospectiva completada; instalador nuevo descartado para este baseline;
  G7 independiente pendiente

## Funcionó bien

- la dependencia funcional explícita de inventario hacia catálogo permitió que el
  kernel rechazara una composición inválida antes de ocultar o romper pantallas;
- separar depósitos, existencias y conteos en tres recorridos evitó una página
  vertical única y conservó acciones comprensibles en móvil;
- API pública, referencias estables y snapshots mantuvieron inventario fuera de las
  tablas y entidades privadas del catálogo;
- WAR y migrador derivados de `with-inventory-demo` mantuvieron sincronizados código
  y esquemas `plg_*`;
- Playwright ejecutó movimientos, reservas, disponibilidad y conteos reales y dejó
  evidencia responsive reutilizable para la demo;
- recrear la aplicación sin tocar PostgreSQL ni Keycloak demostró conservación de
  los datos y de los volúmenes.

## Hallazgos

- una prueba antigua asumía que catálogo podía desactivarse de forma aislada; al
  incorporar inventario, el rechazo es el comportamiento correcto y la prueba debe
  verificar primero la dependencia y luego el orden seguro de desactivación;
- el perfil base sigue siendo indispensable: demuestra que el kernel no depende de
  implementaciones aunque la demo completa ya contenga seis plugins;
- la ruta pública de salud es `/logixone/health/*`; `/logixone/api/health/*` entra
  en la frontera protegida y no debe usarse como diagnóstico;
- los datos creados por E2E son ficticios y persistentes por diseño; la demo no debe
  limpiar tablas directamente ni presentar esos conteos como datos de producción;
- desde este Sprint el instalador es un gate adicional y debe consumir un baseline
  ya congelado, no reconstruir decisiones funcionales durante el empaquetado.

## Acciones acordadas

1. mantener `with-inventory-demo` como composición reproducible congelada de Sprint
   8 y probar siempre también la variante base;
2. entregar a J11-S8-08 los digests, hashes, runbook y restricciones exactas de
   esta historia;
3. implementar preflight antes de UAC o cambios, y bloquear máquinas incompatibles
   sin modificarlas;
4. conservar volúmenes y configuración en instalación, actualización, reparación,
   cancelación y fallo;
5. no comenzar el siguiente plugin productivo hasta completar el instalador y
   registrar el estado formal de Sprint 8;
6. resolver la validación independiente antes de promoción, guía `1.0` o producción.

## Seguimiento J11-S8-C06/C07 — 2026-08-05

- La publicación completa confirmó que los catálogos con más de 100 opciones
  necesitan búsqueda/paginación en servidor; enviar hasta 50 resultados conserva
  rendimiento y una alternativa responsive comprensible.
- Un bean Faces `@RequestScoped` no conserva la página dinámica entre dos POST. El
  selector ahora envía sólo la opción solicitada y el servidor reconstruye la
  vista, revalida empresa/permiso/fuente y resuelve otra vez el código antes de
  aceptarlo; el hallazgo quedó cubierto por regresión y Playwright.
- Mantener originales, JDK, Maven, navegador y temporales bajo `.tools` permitió
  repetir generación, módulos, reactor y E2E sin depender de IntelliJ ni de un
  WildFly instalado en la máquina.
- Los gates finales quedaron verdes: PostgreSQL 5/5, `clean verify` 26/26 con 498
  pruebas y 28 ArchUnit, Compose/health/OIDC, JTA aislado 12/12 y Playwright 1/1
  con 30 capturas. La base terminó con 248 países, 178 monedas/fondos y XDR
  habilitada en versión 22.
- Producto decidió no crear otro instalador hasta disponer de una versión
  comercializable útil para al menos un tipo de negocio. Esto evita empaquetar
  cortes técnicos sin valor operativo suficiente; `current` se conserva como
  evidencia histórica y no representa C07.

## Seguimiento de J11-S8-08

Las acciones 2 a 4 quedaron implementadas para el canal interno: el manifiesto
consume los digests congelados, el preflight antecede a consentimiento/UAC y la
instalación/reparación preservó secretos, volúmenes y datos. La acción 5 continúa
vigente porque faltan VM limpia/incompatible y Authenticode; la acción 6 sigue
pendiente por G7.
