# J11-S8-08 - Evidencia del instalador Windows interno

- Fecha: 2026-08-01
- Estado: validación interna satisfactoria; cierre formal pendiente
- Versión: `0.8.0-internal.1`
- Canal: `INTERNAL_UNSIGNED`
- Plataforma ejecutada: Windows 11 Pro 25H2 x64, build 26200
- ADR: [ADR-0026](../adr/0026-instalador-windows-bootstrapper-nativo.md)

## Alcance demostrado

Se implementó un bootstrapper nativo Windows Forms y una aplicación CLI que
comparten preflight, plan, consentimiento y motor de ejecución. El diagnóstico se
ejecuta antes de cualquier escritura o UAC; el consentimiento queda ligado al
SHA-256 del plan. La instalación usa el baseline congelado J11-S8-07, preserva
configuración y volúmenes, y valida migración, liveness y readiness.

Esta evidencia no declara cerrado Sprint 8 ni autoriza distribución externa. La
edición está sin firma y faltan ambientes independientes de la matriz obligatoria.

## Baseline congelado

| Componente | Identidad exacta |
|---|---|
| Perfil Maven | `with-inventory-demo` |
| Aplicación | `logixone/app:j11-s8-07-closing` |
| Digest aplicación | `sha256:a44293d0bc1a0df01e4e13025a6bc202266dec82fa6bb5f74f858cd70667d4fb` |
| Migrador | `logixone/migrator:j11-s8-07-closing` |
| Digest migrador | `sha256:bcf5a51b535c30cb466a10d782f6059bc383ea8db8360575f01a52086451fd81` |
| Proyecto Compose | `logixone` |
| Política de datos | `PRESERVE_VOLUMES` |

## Artefacto vigente

| Control | Resultado |
|---|---|
| Directorio | `installer/windows/current/` |
| Ejecutable | `Logixone-Setup-0.8.0-internal.1.exe` |
| Tamaño | 103936 bytes |
| SHA-256 | `E97E8C31240AA263E24E4FC86B93C92880F56CCC4CD2F52DD77528FD1F2BC37A` |
| Firma | `NotSigned` |
| Distribución externa | bloqueada por manifiesto (`externalDistributionAllowed=false`) |
| Archivos en `current` | 8, exactamente los declarados |
| Verificación `SHA256SUMS.txt` | 0 faltantes, 0 diferencias |
| Payload | 1352 entradas |

Los ocho archivos son el EXE gráfico, CLI, manifiesto, payload, avisos de terceros,
README operativo, sumas SHA-256 y `BUILD-INFO.json`. La promoción se repitió después
de los ajustes visuales: construyó primero en temporal, sustituyó únicamente los
derivados declarados y dejó cero residuos en `build`.

## Preflight real de solo lectura

Antes del diagnóstico se verificó que el destino no existía; después continuó
ausente. No hubo elevación ni consentimiento durante esa fase.

| Comprobación | Resultado |
|---|---|
| Windows | PASS - Windows 11 Pro 25H2 build 26200 |
| Arquitectura | PASS - x64 |
| RAM | WARNING - 15.7 GiB; 8 mínimo, 16 recomendado |
| Disco | WARNING - 12.0 GiB; reparación existente requiere 5, 60 recomendado |
| Virtualización/SLAT | PASS |
| WSL | PASS - 2.6.3.0 |
| Docker Engine | PASS - 29.6.2 |
| Docker Compose | PASS - 5.3.1 |
| Puertos 18080/8180 | INFORMATION - pertenecían a la instalación Logixone adoptable |
| Reinicio pendiente | PASS - no detectado |
| Permisos | INFORMATION - usuario estándar |
| Instalación previa | detectada y adoptable |
| Resultado | `COMPATIBLE_CON_ADVERTENCIAS` |

En el recorrido controlado la prueba de red/TLS se omitió mediante `--no-network`;
el UI la presenta como advertencia informativa, nunca como una verificación falsa.
La huella del plan observado fue
`074488439fc35c131402c0f76ff3692d4a61a78b560dd6327b97950969161883`.

## Instalación y reparación reales

Destino: `%LOCALAPPDATA%\Logixone\demo-local`.

1. Se aceptó el plan exacto y las licencias de terceros.
2. WSL, Docker y Compose compatibles se reutilizaron; no fue necesario UAC.
3. El payload pasó tamaño/hash y se extrajo con protección contra traversal.
4. Se reutilizaron cuatro secretos externos sin imprimir sus valores.
5. Aplicación y migrador coincidieron con sus digests congelados.
6. El migrador terminó con código `0`.
7. Compose terminó con código `0` y sin `--volumes`.
8. Liveness y readiness respondieron HTTP `200` y `UP`.
9. Se escribió `install-state.json` con `PRESERVE_VOLUMES`.
10. Dos reparaciones posteriores terminaron con código `0`.

Los hashes de los cuatro secretos antes y después coincidieron 4/4. Los conteos
persistentes se mantuvieron iguales después de instalación y reparaciones:

| Dato ficticio | Antes | Después |
|---|---:|---:|
| Socios comerciales | 28 | 28 |
| Artículos/servicios de catálogo | 18 | 18 |
| Listas de precios | 8 | 8 |
| Precios | 7 | 7 |
| Depósitos | 9 | 9 |
| Artículos inventariables | 9 | 9 |
| Movimientos | 7 | 7 |
| Reservas | 5 | 5 |
| Conteos físicos | 5 | 5 |

El log más reciente registró `PAYLOAD hash=verified`, cuatro `SECRET reused`,
`IMAGE digests=verified`, migración/arranque con código `0`, health `UP` y
`COMPLETE ... PRESERVE_VOLUMES`. No contiene valores de secretos.

## Pruebas automatizadas e integridad

| Prueba | Resultado |
|---|---|
| `build-bootstrapper.ps1 -Test` | verde; `PREFLIGHT_TESTS_OK assertions=54` |
| Smoke del ejecutable CLI final | verde; `UI_SMOKE_OK` |
| Hash incorrecto de paquete | rechazo cerrado |
| ZIP traversal | rechazo cerrado |
| Plan modificado tras consentir | rechazo cerrado |
| Máquina bloqueada | no consentimiento, no escritura |
| Cancelación entre fases | terminación segura |
| Rechazo UAC simulado | terminación comprensible |
| Fallo de una operación | no ejecuta fases posteriores |
| Integridad de `current` | 8 archivos, 0 diferencias |

Las pruebas deterministas cubren `COMPATIBLE`, advertencias, bloqueo de SO, puerto
ajeno, Docker ausente, requisitos incompatibles, instalación previa, disco de
reparación, volúmenes huérfanos, consentimiento, ejecución ordenada, cancelación,
UAC y seguridad del paquete.

## Evidencia visual

| Vista | Archivo | Resultado de revisión |
|---|---|---|
| Diagnóstico | `screenshots/J11-S8-08-installer/installer-diagnostico.png` | estado completo, controles legibles y sin recorte vertical |
| Plan y consentimiento | `screenshots/J11-S8-08-installer/installer-plan-consentimiento.png` | siete acciones visibles, consentimiento explícito y botón bloqueado |

La ventana final fue inspeccionada a 1080 x 690. La demo visual reproducible está
en [demo-instalador-windows-sprint-08.md](../runbooks/demo-instalador-windows-sprint-08.md).

## PDF obligatorio actualizado

El PDF de estructura se regeneró contra este corte interno porque J11-S8-08 agregó
una carpeta mantenida, ADR, fuentes, pruebas, runbooks y estado nuevo.

| Control | Resultado |
|---|---|
| Ruta | `docs/output/pdf/guia-estructura-repositorio-logixone.pdf` |
| Páginas | 64 |
| Tamaño | 309965 bytes |
| SHA-256 | `7551B630B9B65876ADA03587C74AAD2D3B4BD25E5B0C81F6A95139039357C8F3` |
| Texto | 296100 caracteres extraíbles; 0 páginas vacías; 0 caracteres de reemplazo |
| Metadatos | título/tema de Corte interno Sprint 8; A4; sin cifrado ni JavaScript |
| Render | 64/64 páginas convertidas a PNG |
| Revisión visual | portada, índice, tablas, inventario, comandos, pendientes y pie sin recortes, solapes ni glifos dañados |

El inventario canónico excluye `installer/windows/bin`, `build` y `current` porque
son derivados, pero documenta sus fuentes y registra por separado el artefacto
vigente, tamaño, hash y firma.

## Pendientes bloqueantes

- VM Windows limpia compatible con Docker/WSL ausentes;
- máquina o VM realmente incompatible;
- puerto ajeno ocupado, UAC rechazado y cancelación en ambientes reales;
- actualización desde una edición `current` anterior publicada;
- descarga/hash inválido usando un servidor de prueba controlado;
- firma Authenticode válida para entrega externa;
- revisión independiente G7 de la guía candidata.

Hasta resolverlos, J11-S8-08 queda validada sólo para evaluación interna y Sprint 8
permanece abierto.
