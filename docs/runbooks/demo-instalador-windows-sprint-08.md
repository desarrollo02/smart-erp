# Demo visual del instalador Windows interno - Sprint 8

- Versión demostrada: `0.8.0-internal.1`
- Canal: `INTERNAL_UNSIGNED`
- Estado: demo interna; no entregar a una empresa
- Duración sugerida: 8 a 12 minutos
- Evidencia: [J11-S8-08](../evidence/J11-S8-08-instalador-windows-cierre.md)

## Objetivo

Mostrar que Logixone ya posee un instalador Windows comprensible y seguro: primero
diagnostica sin tocar la máquina, luego informa todo lo que propone, exige
consentimiento y sólo entonces puede instalar o reparar preservando los datos.

No use esta demo para afirmar que existe un instalador productivo. La firma
Authenticode y la matriz independiente siguen pendientes.

## Preparación

1. Use Windows 11 x64 con Docker Desktop iniciado.
2. Confirme que `installer/windows/current/` contiene exactamente ocho archivos.
3. Verifique `SHA256SUMS.txt` y que el canal es `INTERNAL_UNSIGNED`.
4. Compruebe liveness y readiness si va a mostrar la instalación ya montada.
5. Cierre ventanas con contraseñas, tokens, archivos de secretos o datos reales.
6. No elimine volúmenes ni ejecute `docker compose down --volumes`.

## Paso 1 - Abrir el instalador

Ejecute desde la raíz:

```powershell
.\installer\windows\current\Logixone-Setup-0.8.0-internal.1.exe
```

Muestre el nombre, versión, Sprint y etiqueta `INTERNAL_UNSIGNED`. Explique que la
restricción visible evita confundir una candidata interna con una entrega firmada.

## Paso 2 - Explicar el diagnóstico

En la pestaña **Compatibilidad** muestre:

- sistema operativo y arquitectura;
- RAM y disco, con mínimo, recomendado y recuperación;
- virtualización, WSL, Docker y Compose;
- puertos, reinicio pendiente, permisos e instalación previa;
- resultado textual `COMPATIBLE`, `COMPATIBLE_CON_ADVERTENCIAS` o `BLOQUEADA`.

Resultado esperado: la pantalla declara expresamente que el diagnóstico todavía no
realizó cambios. Las advertencias no se comunican sólo con color.

En el equipo de referencia se esperan advertencias por 15.7 GiB de RAM y 12.0 GiB
de disco, pero la reparación es posible porque supera el mínimo de 5 GiB. Los
puertos 18080/8180 pertenecen a Logixone y no bloquean.

## Paso 3 - Revisar el plan antes de consentir

Pulse **Revisar plan**. Recorra las siete filas:

1. reutilizar WSL;
2. reutilizar Docker/Compose y mostrar su licencia;
3. actualizar el payload de Logixone;
4. reutilizar secretos locales;
5. verificar imágenes contra digests;
6. ejecutar migrador y Compose sin borrar volúmenes;
7. verificar liveness/readiness y ruta visual.

Muestre destino, descarga, UAC, reinicio y puertos. Señale que **Instalar Logixone**
está deshabilitado mientras la casilla de consentimiento está vacía.

Para una demo puramente visual, termine aquí y pulse **Cancelar**. Esto demuestra el
flujo sin volver a modificar la instalación.

## Paso 4 - Instalación o reparación controlada opcional

Ejecute este paso sólo en un ambiente ficticio preparado y después de leer todo el
plan. Marque el consentimiento y pulse **Instalar Logixone**. Muestre el progreso,
sin abrir archivos de secretos.

Resultado esperado:

- payload e imágenes verificados;
- secretos existentes marcados como reutilizados;
- migración y Compose con código `0`;
- liveness/readiness `UP`;
- estado final `PRESERVE_VOLUMES` y acceso visual habilitado.

Si aparece UAC, explique qué componente lo necesita antes de aceptar. Rechazar UAC
debe cancelar esa operación sin fingir éxito.

## Paso 5 - Mostrar Logixone

Abra `http://localhost:18080/logixone/faces/app/index.xhtml` o el acceso directo
creado. Presente brevemente el menú fusionado de Socios, Catálogo e Inventario y
aclare que el instalador no modifica la composición física congelada.

## Paso 6 - Cerrar sin pérdida

Use el acceso `Stop-Logixone.cmd` instalado o `docker compose down` sin
`--volumes`. No desinstale ni borre el directorio durante la demo.

## Recuperación

| Situación | Acción segura |
|---|---|
| Resultado `BLOQUEADA` | leer causa y recuperación; cancelar sin pedir UAC |
| Advertencia de red | revisar proxy/TLS; no desactivar controles corporativos |
| Puerto ajeno ocupado | detener o reconfigurar el propietario; no finalizar procesos sin autorización |
| Hash inválido | conservar log, descartar descarga y obtener el paquete desde el origen aprobado |
| Health no llega a `UP` | revisar logs sin secretos y ejecutar reparación sólo tras corregir la causa |
| Usuario cancela | conservar datos y reanudar más tarde con un preflight nuevo |

## Mensaje de cierre de la demo

“La edición interna ya diagnostica, explica el plan, exige consentimiento, monta el
baseline exacto y repara sin perder datos. Sigue restringida a evaluación interna
hasta completar las VM independientes y la firma Authenticode.”
