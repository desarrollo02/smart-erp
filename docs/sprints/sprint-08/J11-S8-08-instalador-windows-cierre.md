# J11-S8-08 - Instalador Windows y cierre formal

- Estado: Implementada y validada internamente; cierre formal pendiente de matriz
  externa, Authenticode y G7 independiente
- Sprint: 8
- Fecha de planificación: 2026-07-31
- Tipo: distribución, diagnóstico, instalación y cierre
- Épica: [Instalador Windows reproducible](../../backlog/epica-instalador-windows-reproducible.md)
- Procedimiento: [metodología de cierre](../../runbooks/metodologia-instalador-windows-cierre-sprint.md)

## Objetivo

Crear la primera versión del instalador Windows de Logixone después de congelar el
baseline validado de Sprint 8. Debe evaluar la máquina sin modificarla, pedir
consentimiento y elevación mínima, instalar o reutilizar prerrequisitos, montar el
proyecto, ejecutar migración/Compose y demostrar health y persistencia.

Esta historia es el último gate. J11-S8-07 puede completar validación, demo,
retrospectiva y PDF, pero Sprint 8 no se declara formalmente cerrado hasta que
J11-S8-08 esté verde.

## Trabajo previsto

1. aprobar ADR de tecnología, compatibilidad Windows, perfiles, adquisición,
   actualización, desinstalación y firma;
2. crear fuente y manifiesto versionados del instalador;
3. implementar preflight de solo lectura y reporte de compatibilidad;
4. implementar lista de acciones, consentimiento, UAC mínimo y progreso;
5. instalar o reutilizar requisitos aprobados verificando licencias y hashes;
6. preparar el proyecto y ejecutar el recorrido oficial Docker/Compose;
7. validar migrator, liveness, readiness, ruta visual y detención sin pérdida;
8. soportar instalación previa, actualización, reparación, cancelación y fallo;
9. regenerar `current`, reemplazando únicamente el artefacto derivado anterior;
10. probar la matriz en máquinas/VM y registrar evidencia completa.

## Criterios de aceptación

- **CA-01:** el instalador identifica Sprint y baseline/digest exactos.
- **CA-02:** preflight ocurre antes de UAC o cualquier cambio.
- **CA-03:** compatible, advertencia y bloqueo tienen causas comprensibles.
- **CA-04:** el usuario ve componentes, versiones, descargas, licencias, espacio,
  rutas, puertos, reinicios y acciones antes de consentir.
- **CA-05:** UAC se solicita solo para la primera operación privilegiada.
- **CA-06:** toda descarga usa origen, versión y hash/firma aprobados.
- **CA-07:** progreso y resumen final distinguen instalado/reutilizado/omitido/fallido.
- **CA-08:** instalación limpia termina con migración y health verdes.
- **CA-09:** actualización/reparación conserva configuración, volúmenes y datos.
- **CA-10:** cancelación, rechazo UAC, red/hash inválido fallan de forma segura.
- **CA-11:** `current` contiene solo el nuevo instalador verificado.
- **CA-12:** fuentes, manifiestos, evidencias y releases publicados no se borran.
- **CA-13:** tamaño, SHA-256, firma y terceros/licencias quedan registrados.
- **CA-14:** pruebas incluyen VM limpia, incompatible e instalación previa.
- **CA-15:** el instalador externo posee Authenticode; si es interno y no firmado,
  queda claramente restringido y no se entrega a una empresa.
- **CA-16:** Linux permanece fuera del alcance.
- **CA-17:** la guía de implementación, manual técnico, VS Code y manual de usuario
  son revisados contra el flujo real final.

## Baseline de entrada congelado

J11-S8-07 quedó verde el 2026-08-01 y habilita esta historia. El instalador debe
consumir exactamente:

- aplicación `logixone/app:j11-s8-07-closing`,
  `sha256:a44293d0bc1a0df01e4e13025a6bc202266dec82fa6bb5f74f858cd70667d4fb`;
- migrador `logixone/migrator:j11-s8-07-closing`,
  `sha256:bcf5a51b535c30cb466a10d782f6059bc383ea8db8360575f01a52086451fd81`;
- perfil físico `with-inventory-demo`;
- [runbook oficial de demo](../../runbooks/demo-cierre-sprint-08.md);
- [evidencia técnica](../../evidence/J11-S8-07-validacion-demo-cierre.md).

## Resultado implementado

La primera edición interna se materializó como bootstrapper nativo Windows Forms y
CLI en `installer/windows/`, conforme a [ADR-0026](../../adr/0026-instalador-windows-bootstrapper-nativo.md).
El manifiesto fija requisitos, licencias, descargas, hashes, rutas, puertos y los
digests congelados. El preflight no escribe, clasifica la máquina y bloquea antes
del consentimiento cuando detecta una incompatibilidad.

Después del consentimiento ligado a la huella del plan, el motor reutiliza o
instala prerrequisitos, verifica el paquete, adopta secretos existentes sin
registrarlos, ejecuta migración y Compose sin `--volumes`, valida health y conserva
un estado reparable. `current` contiene una única edición `0.8.0-internal.1` con
ocho archivos declarados y hashes coherentes.

## Estado de criterios

| Criterios | Resultado |
|---|---|
| CA-01 a CA-07 | Cumplidos por manifiesto, preflight, plan, consentimiento, progreso y logs sin secretos |
| CA-08 | Validación real satisfactoria sobre la instalación local adoptada; la VM Windows limpia requerida por CA-14 sigue pendiente |
| CA-09 | Dos reparaciones reales conservaron configuración, cuatro secretos y nueve conteos de datos |
| CA-10 | Fallo de hash, cancelación y rechazo UAC cubiertos por pruebas deterministas; faltan rechazo UAC y cancelación en VM real |
| CA-11 a CA-13 | Cumplidos para el canal interno: ocho archivos, cero diferencias de hash, tamaño y SHA-256 registrados |
| CA-14 | Pendiente la matriz independiente en VM limpia e incompatible |
| CA-15 | `NotSigned`; edición restringida a `INTERNAL_UNSIGNED`, no entregable a una empresa |
| CA-16 | Cumplido; Linux no fue implementado |
| CA-17 | Manuales, guías, demo y evidencia actualizados contra el flujo interno real |

## Gates que mantienen abierto el Sprint

- firma Authenticode válida para una distribución externa;
- VM Windows limpia compatible, máquina/VM incompatible y escenarios reales de
  UAC, cancelación, actualización y requisito ausente;
- validación independiente G7 de la guía candidata;
- repetición de cualquier gate que resulte afectado por un hallazgo.

La evidencia detallada está en
[J11-S8-08](../../evidence/J11-S8-08-instalador-windows-cierre.md) y el recorrido
visual en el [runbook de demo](../../runbooks/demo-instalador-windows-sprint-08.md).
La existencia del EXE interno no autoriza cerrar Sprint 8, promover imágenes ni
entregar el instalador a una empresa.

## Evolución posterior planificada

El 2026-08-04 se incorporó
[WIN-I09](../../backlog/WIN-I09-seleccion-plugins-dependencias.md): el instalador
deberá mostrar la lista de plugins del baseline y permitir seleccionar la
composición física. Seleccionar un plugin incorporará recursivamente sus
dependencias requeridas, indicará por qué se agregaron y bloqueará ciclos,
ausencias o incompatibilidades antes del consentimiento.

Esta capacidad no está implementada en `0.8.0-internal.1`. Su implementación debe
conservar la composición única de ADR-0012: WAR y migrador tendrán exactamente el
mismo conjunto; no habrá carga dinámica y retirar un plugin no borrará datos.
