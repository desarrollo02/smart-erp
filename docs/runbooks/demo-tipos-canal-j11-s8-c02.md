# Demo candidata de tipos de canal - J11-S8-C02

- Estado: histórico; sustituido por la demo unificada de definiciones de socios del vigésimo corte
- Fecha: 2026-08-03
- Perfil físico: `with-inventory-demo`
- Usuario ficticio: `demo.empresas.ab`
- Permiso: `business_partners.manage`
- Evidencia: `../evidence/screenshots/J11-S8-C02-channel-history/e2e/`
- Aplicación verificada: `logixone/app:j11-s8-c02-channel-history`
- Digest de aplicación: `sha256:b13a97f263661e315c64ca7961437ae96b40ab5a5a30a93723b7bfb899986b48`
- Migrador verificado: `logixone/migrator:j11-s8-c02-channel-history`
- Digest de migrador: `sha256:0a908b12b8b5384755ae0c2ea556114a42f7468eae74f6ee018799d3903d4f63`
- Migración: V3 de `business_partners` aplicada una vez; repetición con cero cambios

## Objetivo

Demostrar que cada empresa puede registrar un medio de contacto propio, revisar su
nombre conservando el código, consultar el historial append-only, inactivarlo sin
borrarlo, reactivarlo y utilizarlo en la ficha de un socio, sin SQL manual, reinicio
ni una lista fija en Jakarta Faces.

La candidata permite consulta, alta, revisión del nombre visible, historial de
solo lectura e inactivación/reactivación versionadas. No presente como disponibles
cambio de código, reemplazo de identidad ni eliminación de tipos o revisiones.

## Resultado verificado

El 2026-08-03 el recorrido automatizado final terminó con 1 prueba verde, 0 fallos,
0 errores y 0 omitidas en 39,48 s. Registró un tipo de canal empresarial, creó una
revisión de nombre, leyó ambas versiones en **Historial**, recorrió el ciclo
activo/inactivo, utilizó el nombre revisado en la ficha de un socio y restauró la
activación temporal del plugin. Se revisaron 23 PNG —3.137.997 bytes— en 1280, 720
y 375 px. Liveness y readiness permanecieron HTTP 200/`UP` y 829 líneas de log
tuvieron cero errores severos o excepciones.

## Preparación segura

1. Use únicamente empresas, usuarios y contactos ficticios.
2. Compruebe que `business_partners` está activo y que el rol posee
   `business_partners.manage`.
3. Levante el perfil `with-inventory-demo` sin eliminar volúmenes.
4. Verifique liveness y readiness en:
   - `http://localhost:18080/logixone/health/live`;
   - `http://localhost:18080/logixone/health/ready`.
5. Ambos deben responder HTTP 200 y estado `UP`.

## Guion para presentar

1. Inicie sesión y seleccione la empresa ficticia A.
2. Muestre que **Socios comerciales** y **Tipos de canal** aparecen fusionados en
   el mismo menú, aunque provienen del contrato del plugin.
3. Abra **Tipos de canal** y muestre directorio, filtros y valores iniciales.
4. Pulse **Nuevo tipo de canal**.
5. Registre un valor ficticio único; por ejemplo código `telegram_demo` y nombre
   **Telegram empresarial**.
6. Confirme el mensaje **Tipo de canal registrado** y el detalle creado.
7. Abra **Nueva revisión**, cambie el nombre a **Telegram prioritario** y pulse
   **Guardar revisión**.
8. Abra **Historial** y compruebe que existen dos filas: la versión actual con el
   nombre nuevo y la histórica con **Telegram empresarial**.
9. Revise el historial en 1280, 720 y 375 px; no debe existir overflow horizontal.
10. Abra **Estado**, pulse **Inactivar tipo** y confirme el mensaje de éxito.
11. Vuelva a **Resumen** y compruebe que el registro sigue visible como **Inactivo**.
12. Vuelva a **Estado**, pulse **Reactivar tipo** y compruebe **Activo** en el resumen.
13. Abra **Socios comerciales**, seleccione un socio ficticio y vaya a **Contacto**.
14. Abra **Tipo de canal** y muestre que **Telegram prioritario** está disponible.
    Ingrese un dato ficticio y pulse **Agregar canal**.
15. Explique que el código persistido es estable, el nombre es revisable, cada
    versión se conserva y el catálogo pertenece sólo a la empresa activa; un tipo
    inactivo no aparece en altas nuevas.
16. Repita la revisión en 1280, 720 y 375 px. No debe aparecer overflow horizontal
    normal ni perderse la acción principal.

## Ejecución automática

```powershell
$projectRoot=(Resolve-Path '.').Path

.\mvnw.cmd -Pvisual-e2e -pl tests/e2e-tests `
  "-Dit.test=BusinessPartnersVisualIT" `
  "-Dlogixone.business-partners.e2e=true" `
  "-Dlogixone.app-url=http://localhost:18080/logixone/faces/app/index.xhtml" `
  "-Dlogixone.admin-url=http://localhost:18080/logixone/faces/admin/index.xhtml" `
  "-Dlogixone.demo-user-password-file=$projectRoot/.tools/secrets/demo-user-password.txt" `
  "-Dlogixone.evidence-dir=$projectRoot/docs/evidence/screenshots/J11-S8-C02-channel-history/e2e" `
  "-Dlogixone.playwright.executable=$projectRoot/.tools/playwright/chromium-1228/chrome-win64/chrome.exe" `
  verify
```

## Resultado esperado y recuperación

- el tipo se registra y aparece en el selector de la misma empresa;
- una nueva revisión cambia sólo el nombre visible y el historial conserva ambas
  versiones en orden descendente;
- inactivar conserva el detalle y aumenta la versión sin borrar la fila;
- el tipo inactivo no se ofrece en canales nuevos y reactivar lo vuelve a habilitar;
- otra empresa no recibe el valor por filtración accidental;
- un código duplicado se rechaza sin sobrescribir el registro;
- la pantalla conserva labels, foco visible y layout responsive;
- health permanece `UP` después del recorrido.

Si falla, conserve pantalla, hora, mensaje, logs y capturas. No corrija tablas
manualmente y no ejecute `docker compose down --volumes`. Compruebe activación,
permiso, empresa seleccionada y health antes de repetir una operación.

Esta demo histórica no cierra J11-S8-C02 ni Sprint 8. El recorrido vigente está en
[Definiciones de socios](demo-definiciones-socios-j11-s8-c02.md). Siguen pendientes
fuentes normativas, estrategia de listas grandes, recongelación, PDF y la decisión
de producto sobre un instalador nuevo al llegar al cierre.
