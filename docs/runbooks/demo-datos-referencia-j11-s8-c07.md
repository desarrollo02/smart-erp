# Demo de publicaciones completas y políticas de referencia — J11-S8-C07

- Fecha de verificación: 2026-08-05
- Baseline: `J11-S8-C07`
- Ruta inicial: `http://127.0.0.1:18080/logixone/app/`
- Perfil físico: `with-inventory-demo`
- Datos: exclusivamente ficticios

## Objetivo

Demostrar en el sistema real las publicaciones completas de países y monedas,
búsqueda/paginación en servidor, unidad menor no aplicable, política empresarial
versionada, consumo desde Socios Comerciales y autorización negativa.

## Prerrequisitos y preparación

1. Use sólo el Maven Wrapper, Java, navegador y temporales aprovisionados bajo
   `.tools/`; no use WildFly ni herramientas instaladas por IntelliJ.
2. Construya la pareja `logixone/app:j11-s8-c07-reference-data` y
   `logixone/migrator:j11-s8-c07-reference-data` con el perfil
   `with-inventory-demo`, según [docker-build.md](docker-build.md).
3. Levante Compose con la configuración local segura y espere PostgreSQL,
   Keycloak y aplicación saludables; el migrator debe terminar en código 0.
4. Inicie sesión con el usuario ficticio de demo que posea
   `reference_data.view`, `reference_data.policy.manage` y permisos de Socios
   Comerciales. Las credenciales pertenecen al archivo local de secretos y nunca
   se copian a este documento, capturas o logs.
5. Seleccione la empresa ficticia A y confirme que `reference_data` y
   `business_partners` estén activos.

## Recorrido principal

1. Abra **Datos de referencia**. Confirme 248 países y 178 códigos únicos de
   moneda/fondo, procedencia y publicación corriente.
2. Seleccione países, busque `para` y recorra páginas. Compruebe que ninguna
   respuesta contenga más de 50 filas y que el total sea estable.
3. Seleccione monedas, busque `XDR` y abra su detalle. Cuando la unidad menor no
   aplica, la pantalla debe mostrar `N.A.`, nunca cero.
4. Inhabilite XDR con la versión observada. Confirme el nuevo estado y abra
   **Historial**; debe existir una revisión append-only sin alterar la publicación.
5. Vuelva a habilitar XDR. Al finalizar la demo debe quedar habilitada; en la
   ejecución de cierre terminó en versión 22.
6. Abra **Socios comerciales**, inicie un alta y busque un país por código o
   nombre. Seleccione una opción del resultado; el formulario debe conservar sólo
   el código revalidado por el servidor, aun cuando la búsqueda ocurrió en un POST
   anterior.
7. Retire temporalmente `reference_data.policy.manage` del rol ficticio, renueve
   la sesión e intente administrar una política. Debe mostrarse acceso denegado y
   no debe existir cambio en PostgreSQL. Restaure el permiso y renueve la sesión.

## Responsive y evidencia

Repita directorio, detalle/historial y denegación en 375, 720 y 1280 px. El
contenido normal no debe producir overflow horizontal; en medio y compacto las
tablas deben adoptar tarjetas conservando labels, foco y acciones.

Las 30 capturas verificadas están en
[`docs/evidence/screenshots/J11-S8-C07/e2e`](../evidence/screenshots/J11-S8-C07/e2e/).
La evidencia textual y los digests exactos están en
[`J11-S8-C07-publicaciones-completas-reference-data.md`](../evidence/J11-S8-C07-publicaciones-completas-reference-data.md).

## Resultado esperado y restauración

- PostgreSQL conserva 248 países, 178 monedas/fondos y 13 unidades menores
  ausentes;
- XDR termina habilitada y su historia conserva todas las revisiones;
- el permiso temporal queda restaurado y la sesión se renueva;
- PostgreSQL y Keycloak conservan sus volúmenes al recrear sólo la aplicación;
- no se revelan secretos ni se promueven imágenes.

Si la demo se interrumpe, no modifique tablas manualmente. Restaure permiso y
política mediante las pantallas autorizadas, verifique readiness y registre el
estado final. Para detener sin pérdida use el procedimiento de
[Compose](compose.md) sin eliminar volúmenes.

## Limitaciones

Las publicaciones observadas son una referencia trazable del baseline, no una
certificación fiscal ni una garantía perpetua de vigencia o licencia. La demo no
autoriza producción, no crea códigos normativos y no sustituye G7 independiente,
la decisión del instalador ni los demás gates formales del Sprint.
