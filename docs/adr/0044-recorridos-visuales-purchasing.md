# ADR-0044 — Recorridos visuales de `purchasing`

- Estado: Aceptada
- Fecha: 2026-08-11
- Historia: J11-S9-05
- Decisiones relacionadas: ADR-0017, ADR-0018, ADR-0028, ADR-0041, ADR-0043

## Contexto

El dominio y la aplicación de Compras ya separan solicitud, orden, recepción y
devolución. Faltaba una interfaz que permitiera operar esos casos sin transferir
XHTML, beans internos o acceso a tablas hacia el shell. La UI también necesita
proveedores, artículos, monedas, depósitos, ubicaciones y documentos previos en
selectores con propietario y autorización explícitos.

## Decisión

`purchasing` publica cinco `ScreenDefinition` neutrales y cinco handlers
`ScreenInteraction`:

1. `requests`: alta con primera línea, línea adicional, envío, decisión,
   cancelación y clonación;
2. `orders`: alta directa/asignada, línea adicional, emisión, cancelación y
   cierre de pendientes;
3. `receipts`: comprobante de una línea y confirmación con movimiento de stock;
4. `returns`: devolución de una línea recibida y confirmación con salida de stock;
5. `tracking`: consulta de cumplimiento neto por orden y línea.

El shell registra las rutas y especificaciones cerradas, pero sigue siendo dueño
de Jakarta Faces, XHTML, Material Design 3, responsive, foco, etiquetas y
representación de tablas. El plugin solo entrega datos, acciones y avisos
neutrales.

Todos los `SELECT` declaran `SelectorSourceDefinition`. Estados, tipos y
condiciones son listas cerradas; moneda es normativa; artículo/proveedor son
catálogos empresariales; depósitos, ubicaciones y documentos son referencias
operativas. Las rutas Administrar pertenecen al plugin propietario y revalidan
permiso/empresa en servidor.

Para paginar sin importar internos se amplía `business-partners-api` a 1.1 con
búsqueda pública de referencias y se amplía el contrato 1.1 de Inventario con un
directorio autorizado de depósitos/ubicaciones. `commercial-catalog-api` pasa a
1.1 para filtrar `PURCHASE` antes de contar y paginar. Compras exige esos tres
plugins desde 1.1. No se agregan relaciones JPA ni SQL entre esquemas.

Los listados de Compras usan un puerto privado y proyecciones escalares; las
fichas recuperan el agregado por el caso de uso autorizado. Cada mutación conserva
versión esperada e idempotencia determinada por empresa, actor, acción y valores
estables del intento.

## Alternativas descartadas

- **XHTML y managed beans aportados por el plugin:** rompe el control del shell y
  permite inyectar presentación o EL arbitraria.
- **Consultar tablas de socios, catálogo o inventario:** viola propiedad del dato
  y vuelve frágiles las migraciones.
- **Cargar todos los catálogos en línea:** no escala y contradice búsqueda en
  servidor para listas grandes.
- **Una sola pantalla con todas las operaciones:** mezcla responsabilidades,
  permisos y estados, especialmente en ancho compacto.
- **Editar documentos confirmados:** elimina trazabilidad y contradice dominio y
  triggers inmutables.

## Consecuencias

- El descriptor publica cinco menús y cinco pantallas, todos protegidos por
  `purchasing.view`; cada acción vuelve a exigir su permiso exacto.
- La distribución oficial todavía no incorpora el plugin: J11-S9-06 debe componer
  WAR y migrador de forma atómica antes de que las rutas sean navegables.
- Los recorridos se diseñan para 375, 720 y 1280 px mediante el renderer genérico;
  Playwright y seguridad negativa permanecen pendientes por la excepción de
  pruebas acumuladas.
- El manual 07 explica datos, estados, recuperación y tablas por pantalla. Sus
  diagramas se derivan de V1–V2; no se consultó una base local sin autorización.
