# Instalador Windows de Logixone

Fuentes del bootstrapper interno regenerado para Sprint 9. La decisión técnica está en
[ADR-0026](../../docs/adr/0026-instalador-windows-bootstrapper-nativo.md) y el
procedimiento operativo en la
[metodología de cierre](../../docs/runbooks/metodologia-instalador-windows-cierre-sprint.md).

## Estructura

- `manifest/`: requisitos, baseline, componentes, licencias y archivos derivados;
- `src/`: núcleo de preflight, aplicación CLI/Windows Forms y motor de ejecución;
- `scripts/`: construcción y promoción acotada de la edición;
- `tests/`: pruebas deterministas sin modificar la máquina;
- `payload/`: inventario del contenido que se empaqueta;
- `current/`: edición derivada vigente; nunca contiene fuentes ni secretos.

La edición Sprint 9 es `INTERNAL_UNSIGNED`. No puede entregarse a una empresa hasta
aplicar y verificar Authenticode.

## Compatibilidad fijada

- Windows 11 x64, build 26100 o posterior;
- 8 GiB de RAM mínimos y 16 GiB recomendados;
- 30 GiB libres para instalación limpia, 5 GiB para reparación y 60 GiB
  recomendados;
- WSL 2.1.5 o posterior;
- Docker Engine 29.6 y Compose 5.3 o posteriores;
- puertos locales `18080` y `8180` disponibles o pertenecientes a Logixone.

La adquisición aprobada para una máquina sin Docker es Docker Desktop 4.84.0. Su
URL, tamaño, licencia y SHA-256 están fijados en el manifiesto. Un requisito
incompatible, volúmenes huérfanos o puertos ajenos bloquean antes de escribir o
pedir UAC.

## Construir y probar

Desde la raíz del repositorio:

```powershell
powershell -ExecutionPolicy Bypass -File installer\windows\scripts\build-bootstrapper.ps1 -Test
powershell -ExecutionPolicy Bypass -File installer\windows\scripts\build-installer.ps1
```

La primera orden compila el bootstrapper, el CLI y las pruebas deterministas. La
segunda vuelve a ejecutar esas pruebas, verifica los digests locales de aplicación
y migrador, crea el payload en temporal y promueve atómicamente los ocho archivos
declarados a `current`.

## Diagnosticar sin cambios

```powershell
.\installer\windows\current\Logixone-Installer.Cli.exe `
  --manifest .\installer\windows\current\installer-manifest.json `
  --preflight --plan --no-network
```

Los códigos `0`, `1` y `2` significan `COMPATIBLE`,
`COMPATIBLE_CON_ADVERTENCIAS` y `BLOQUEADA`. El código `1` permite continuar tras
revisar las advertencias; no representa un fallo del ejecutable.

Para la interfaz gráfica, ejecute:

```powershell
.\installer\windows\current\Logixone-Setup-0.9.0-internal.1.exe
```

Primero se muestra el diagnóstico. Después, **Revisar plan** enumera acciones,
licencias, descargas, rutas, puertos y reinicios. **Instalar Logixone** permanece
deshabilitado hasta marcar el consentimiento explícito.

## Resultado interno vigente

La edición vigente se genera para el baseline congelado J11-S9-07, con perfil
físico `with-purchasing-demo` e imágenes identificadas por digest. Su tamaño,
SHA-256, firma y cantidad de entradas se registran en la evidencia J11-S9-08.
La compilación y las pruebas deterministas no autorizan por sí solas una entrega
externa ni sustituyen la matriz independiente de instalación.

Siguen pendientes la matriz independiente en VM limpia e incompatible, rechazo
UAC/cancelación reales y la firma Authenticode. Consulte la
[evidencia J11-S9-08](../../docs/evidence/J11-S9-08-instalador-windows-cierre.md).
