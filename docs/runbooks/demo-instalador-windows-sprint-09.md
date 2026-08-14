# Demo segura del instalador Windows interno - Sprint 9

- Versión: `0.9.0-internal.1`
- Historia: J11-S9-08
- Canal: `INTERNAL_UNSIGNED`
- Fecha de verificación: 2026-08-14
- Instalación real durante el cierre: no ejecutada; equipo bloqueado por puertos

## Objetivo

Mostrar que la edición vigente identifica Sprint 9, diagnostica antes de escribir,
explica bloqueos y conserva deshabilitada la ejecución cuando la máquina no es
compatible. Esta demo no autoriza instalar ni distribuir el artefacto.

## Prerrequisitos

1. Windows 11 x64.
2. Los ocho archivos de `installer/windows/current` juntos.
3. No ejecutar como administrador.
4. No usar `--execute` durante este recorrido.

## Verificación de integridad

```powershell
Get-FileHash -Algorithm SHA256 `
  installer\windows\current\Logixone-Setup-0.9.0-internal.1.exe

.\installer\windows\current\Logixone-Installer.Cli.exe `
  --manifest .\installer\windows\current\installer-manifest.json `
  --ui-smoke
```

Resultado esperado: SHA-256
`E7E2036D130AE4D8A10E821C18B9558279E71E6E15CBA8A0323155A83E83509A`
y `UI_SMOKE_OK`.

## Diagnóstico CLI sin cambios

```powershell
.\installer\windows\current\Logixone-Installer.Cli.exe `
  --manifest .\installer\windows\current\installer-manifest.json `
  --preflight --plan --no-network
```

La cabecera debe indicar `0.9.0-internal.1 | J11-S9-08`. Los resultados posibles
son `COMPATIBLE`, `COMPATIBLE_CON_ADVERTENCIAS` o `BLOQUEADA`. En el ambiente de
cierre, 18080 y 8180 estaban ocupados y el resultado esperado fue `BLOQUEADA`,
código 2, seguido de `No se realizó ningún cambio`.

## Recorrido visual

Abrir `Logixone-Setup-0.9.0-internal.1.exe` sin elevación. Verificar:

1. título con la versión vigente;
2. encabezado `J11-S9-08 · Sprint 9 · INTERNAL_UNSIGNED`;
3. tabla de sistema, arquitectura, memoria, disco, WSL, Docker, puertos y permisos;
4. estado bloqueado cuando exista un blocker;
5. botón de instalación deshabilitado y ausencia de UAC;
6. cierre con **Cancelar**, sin aceptar plan ni iniciar cambios.

En una máquina compatible puede revisarse la segunda pestaña para inspeccionar
acciones, licencias, descargas, rutas, puertos y reinicios, pero la ejecución real
pertenece a la matriz independiente pendiente.

## Restauración

El recorrido no crea archivos ni cambia servicios. Cerrar la ventana es suficiente.
No detener la demo, no ejecutar `docker compose down --volumes` y no modificar una
instalación previa para conseguir un estado compatible.
