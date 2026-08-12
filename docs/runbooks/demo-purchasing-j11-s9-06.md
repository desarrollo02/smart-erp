# Demo reproducible de Compras — J11-S9-06

- Fecha de verificación: 2026-08-12
- Estado: candidata navegable validada automáticamente; aceptación independiente pendiente
- URL inicial: `http://localhost:18080/logixone/`
- Proyecto Compose: `logixone-j11-s9-06`
- Aplicación: `logixone/app:j11-s9-06-purchasing-demo-r5`
- Migrador: `logixone/migrator:j11-s9-06-purchasing-demo`

## Prerrequisitos

- Docker/Compose disponible como puente de plataforma;
- secretos locales válidos bajo `.tools/secrets/`;
- navegador con acceso a `localhost:18080` y `localhost:18180`;
- usuario ficticio `demo.empresas.ab`; para aprobar de forma independiente se usa
  `demo.empresa.a` en la misma empresa;
- plugins `reference_data`, `business_partners`, `commercial_catalog`, `inventory`
  y `purchasing` activos, y rol de demo con los permisos del flujo.

No copie ni muestre las contraseñas. El ambiente usa datos ficticios y no debe
conectarse a servicios configurados manualmente en el IDE.

## Recorrido manual sugerido

1. Abra la URL inicial e inicie sesión con `demo.empresas.ab`.
2. Seleccione la empresa compartida con `demo.empresa.a` si aparece el selector.
3. Confirme que el menú contiene Solicitudes, Órdenes, Recepciones, Devoluciones y
   Seguimiento de Compras.
4. En Solicitudes cree un borrador con un producto activo, diez unidades y moneda
   PYG; luego envíelo a aprobación.
5. Cierre sesión, ingrese con `demo.empresa.a`, abra la misma empresa y apruebe la
   solicitud. El sistema debe impedir que el solicitante se apruebe a sí mismo.
6. Regrese con `demo.empresas.ab`, cree una orden directa o asignada y emítala.
7. Prepare y confirme una recepción parcial de seis unidades, eligiendo depósito,
   ubicación y condición.
8. Prepare y confirme una devolución de dos unidades desde esa recepción.
9. Abra Seguimiento y verifique: pedida 10, recibida 6, devuelta 2 y pendiente 6.

Rutas directas del shell, útiles para soporte:

```text
http://localhost:18080/logixone/faces/app/index.xhtml?route=%2Fpurchasing%2Frequests&mode=directory
http://localhost:18080/logixone/faces/app/index.xhtml?route=%2Fpurchasing%2Forders&mode=directory
http://localhost:18080/logixone/faces/app/index.xhtml?route=%2Fpurchasing%2Freceipts&mode=directory
http://localhost:18080/logixone/faces/app/index.xhtml?route=%2Fpurchasing%2Freturns&mode=directory
http://localhost:18080/logixone/faces/app/index.xhtml?route=%2Fpurchasing%2Ftracking&mode=directory
```

## Resultados esperados

- cada búsqueda conserva las selecciones previas del formulario;
- líneas y ubicaciones se recalculan después de elegir orden o depósito;
- una referencia manipulada o ajena a la empresa es rechazada por el servidor;
- confirmar recepción suma recibido; confirmar devolución suma devuelto y reabre
  pendiente;
- los estados y errores tienen texto comprensible y foco/navegación accesibles;
- en 375, 720 y 1280 px no existe desplazamiento horizontal normal.

## Recuperación ante errores

- Si una opción no aparece, revise empresa, estado activo y permiso; use
  `Administrar` solo si su rol lo permite.
- Si el documento cambió de versión, vuelva al directorio, recárguelo y repita la
  operación sobre la versión vigente.
- Si readiness no está `UP`, no continúe: revise `docker compose ps` y los logs de
  `migrator`, `app`, `postgres` y `keycloak`, sin mostrar secretos.
- Si Compras está desactivado, su menú y rutas deben quedar denegados; restaure la
  activación desde Administración antes de continuar.

## Restauración del estado

La prueba automatizada restaura la activación de Compras, pero conserva los datos
ficticios creados para permitir auditoría. No elimine volúmenes para “limpiar” una
demo. Para repetirla, use números y códigos nuevos. Una restauración destructiva
de la base requiere una decisión separada y un respaldo verificable.

## Evidencia

Las capturas verificadas están en
`docs/evidence/screenshots/J11-S9-06/e2e/`. El detalle de pruebas, imágenes y
pendientes está en
[`docs/evidence/J11-S9-06-integracion-composicion-purchasing.md`](../evidence/J11-S9-06-integracion-composicion-purchasing.md).
