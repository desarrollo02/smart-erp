# Retrospectiva técnica de Sprint 10

- Fecha: 2026-08-20
- Incremento: floorplans operativos v2 sobre Inventario y Compras
- Estado: implementación y gates automáticos hasta J11-S10-06; validación
  independiente y decisión J11-S10-07 pendientes

## Funcionó bien

- los contratos neutrales permitieron diferenciar maestros, bandejas, editores,
  operaciones guiadas y consultas sin entregar XHTML desde plugins;
- la compatibilidad v1/v2 conservó las pantallas administrativas vigentes;
- Inventario y Compras migraron sobre sus rutas y menús existentes;
- el transporte semántico preservó borradores seguros y mantuvo tokens técnicos
  fuera de la transcripción del operador;
- Playwright convirtió accesibilidad, seguridad negativa y límites responsive en
  regresiones ejecutables.

## Hallazgos

- una vista request-scoped necesita transportar explícitamente sólo el contexto
  semántico declarado y revalidarlo en cada postback;
- el foco después de postback debe priorizar error, aviso y título, en ese orden;
- probar únicamente 375/720/1280 no reemplaza los límites 599/600/839/840;
- la revisión visual sigue encontrando defectos que una aserción de overflow no
  detecta: la barra empresarial se superponía en compacto;
- Compose debe recibir etiquetas explícitas y verificarse por imagen activa, no
  confiar en un `compose.env.local` que puede conservar un baseline anterior;
- el bootstrap administrativo de un solo uso debe fallar cerrado ante una
  identidad no exacta y deshabilitarse inmediatamente después del fixture.

## Acciones

1. reutilizar los floorplans cerrados en `sales` sin crear tipos visuales por
   conveniencia;
2. conservar foco, movimiento reducido y límites responsive en cada historia
   visual, no sólo en el cierre;
3. mantener pruebas negativas de plugin/permiso junto al recorrido feliz;
4. verificar siempre la identidad real de la imagen y el catálogo runtime;
5. completar la validación independiente antes de declarar una versión
   comercializable;
6. registrar en J11-S10-07 la decisión explícita sobre el instalador Windows.
