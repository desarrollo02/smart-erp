# Retrospectiva técnica de Sprint 9

- Fecha: 2026-08-14
- Incremento: `purchasing` integrado con socios, catálogo, referencia e inventario
- Estado: gates automatizados y documentales verdes; instalador interno creado;
  G7, Authenticode y matriz Windows independiente pendientes

## Funcionó bien

- la frontera pública permitió confirmar recepciones y devoluciones con Inventario
  dentro de JTA sin importar entidades ni consultar tablas privadas;
- separar solicitud, orden, recepción, devolución y seguimiento mantuvo estados e
  invariantes claros y evitó convertir la orden en factura, deuda o asiento;
- los snapshots preservan proveedor, moneda, artículo, unidad y condiciones del
  momento, aunque los maestros cambien después;
- la composición única mantuvo sincronizados WAR, migrador y las ocho definiciones
  físicas del perfil;
- Playwright ejercitó los cinco plugins productivos y dejó 170 capturas reales en
  los tres anchos exigidos;
- recrear únicamente `app` conservó conteos de negocio y activaciones.

## Hallazgos

- las suites acumuladas no deben depender de códigos semilla como `EA` o `IVA`:
  cada recorrido que crea datos debe aprovisionar sus propias referencias;
- los selectores repetidos requieren localizar el control por su identificador de
  pantalla y no sólo por una etiqueta visible compartida;
- al agregar dependencias nuevas, las pruebas deben desactivar consumidores antes
  que proveedores y restaurar en el orden inverso;
- las páginas y tarjetas cargadas de forma asíncrona necesitan esperas semánticas,
  especialmente al paginar países o cambiar activaciones;
- el kernel rechazó correctamente cada composición incompatible; los primeros
  fallos E2E revelaron supuestos obsoletos del arnés, no una relajación necesaria
  del producto;
- WildFly emite dos advertencias esperadas al autogenerar el keystore local; no se
  observaron errores, excepciones ni advertencias funcionales posteriores.

## Acciones

1. conservar datos E2E autocontenidos y evitar dependencias implícitas de semillas;
2. mantener el orden de dependencias como parte de las pruebas de activación;
3. usar los floorplans de Sprint 10 para reducir carga vertical en operaciones y
   aprobaciones, sin reabrir los límites de Compras;
4. completar la validación independiente antes de declarar una primera versión
   comercializable;
5. decisión `SÍ` registrada e instalador `0.9.0-internal.1` creado en J11-S9-08;
   resta completar su matriz externa.
