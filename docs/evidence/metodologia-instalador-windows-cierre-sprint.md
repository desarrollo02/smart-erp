# Evidencia - Instalador Windows obligatorio al cerrar cada Sprint

- Fecha: 2026-07-31
- Tipo: decisión de metodología de producto
- Estado: incorporada al plan; implementación pendiente
- Vigencia: desde Sprint 8

## Decisión

El responsable de producto estableció que cada Sprint debe generar un instalador
Windows nuevo después de congelar su baseline. El instalador anterior del
directorio generado `current` debe reemplazarse, preservando código fuente,
manifiestos, evidencias e historia.

El instalador debe evaluar primero la máquina, indicar compatibilidad o bloqueo,
mostrar todas las acciones, solicitar consentimiento y permisos, instalar o
reutilizar requisitos, montar Logixone y mostrar progreso y resultado.

## Salvaguardas incorporadas

- diagnóstico de solo lectura antes de elevar o cambiar el equipo;
- tres estados: compatible, compatible con advertencias y bloqueada;
- consentimiento explícito y UAC mínimo;
- descargas fijadas, verificadas y con licencia declarada;
- preservación de instalación previa, configuración, volúmenes y datos;
- reemplazo acotado únicamente al artefacto `current`;
- pruebas en VM limpia, incompatible y con instalación previa;
- firma exigida para distribución externa;
- Linux diferido a una planificación posterior.

## Archivos creados o actualizados

- `AGENTS.md` y Definition of Done;
- épica del instalador Windows;
- metodología reproducible en `docs/runbooks/`;
- estrategia de pruebas y plan de Sprint 8;
- guía de implementación, manual técnico e índice documental.

El manual de usuario se revisó y no cambia: todavía no existe un instalador ni una
tarea visible para el operador. La guía de Visual Studio Code conserva el recorrido
manual como fuente vigente hasta que el instalador esté implementado y validado.

## Estado real

Este cambio es exclusivamente de planificación y metodología. No existe aún EXE,
MSI, bootstrapper, manifiesto ejecutable, firma ni prueba en VM. No se eliminó
ningún archivo anterior porque todavía no existe un directorio `current` aprobado.

La primera implementación pertenece al último gate de Sprint 8 y requiere un ADR
de tecnología, compatibilidad, perfiles, actualización y firma.

## Validación documental

El validador recorrió los Markdown mantenidos, resolvió enlaces locales, decodificó
UTF-8 estrictamente y buscó texto dañado y patrones de secretos.

```text
MARKDOWN_FILES=214
BROKEN_LINKS=0
ENCODING_ERRORS=0
MOJIBAKE_FILES=0
SECRET_LEAKS=0
```

No se ejecutaron Maven, Docker, Compose o pruebas de VM porque este cambio no crea
código ni un instalador ejecutable.
