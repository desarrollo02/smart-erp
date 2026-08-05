# Evidencia de generación y revisión del manual PDF de pruebas J11-S4-08

- Fecha: 2026-07-28
- Estado: completado y verificado
- Alcance: guía operativa para ejecutar la validación acumulada del Sprint 4
- Fuente canónica: [manual paso a paso](../runbooks/manual-pruebas-j11-s4-08.md)
- PDF derivado: [manual-pruebas-j11-s4-08.pdf](../output/pdf/manual-pruebas-j11-s4-08.pdf)

## Objetivo

Entregar una guía autocontenida que explique en orden qué pruebas deben
materializarse y ejecutarse dentro de `J11-S4-08`, por qué se realiza cada paso,
qué resultado aceptar, cuándo detenerse y qué evidencia conservar.

El manual no declara verdes los gates. Distingue explícitamente la compilación de
`J11-S4-07` de la validación todavía pendiente y señala las pruebas de Sprint 4 que
aún deben implementarse antes de ejecutar el gate formal.

## Alcance documentado

El PDF contiene:

- preflight de Java, Maven Wrapper, Docker, Compose, secretos por archivo y entorno
  aislado;
- materialización de la matriz pendiente de autoridad global, V4/V5, JTA, web y
  Playwright;
- G0 documental, G1 por módulo, G2 reactor/ArchUnit y composición WAR 0/1/3 plugins;
- PostgreSQL/Testcontainers, migraciones V1-V5, JPA y JTA;
- Dockerfiles, imágenes, Compose, health, bootstrap global y OIDC;
- autorización administrativa positiva/negativa, cabeceras y manipulación;
- empresas, plugins, personalización, seguridad empresarial, autoridad y auditoría;
- Playwright, responsive, accesibilidad y revisión visual;
- persistencia de volúmenes, demo, evidencia, recorrido independiente y PDF de
  cierre.

Cada bloque diferencia acción, motivo, resultado esperado y reacción ante fallos.
No se incluyeron contraseñas, tokens, cookies ni valores de secretos.

## Generación

Se utilizó ReportLab con tipografía local, tamaño A4, portada, índice automático,
encabezados, pies, tablas repetibles, bloques de comando y recuadros semánticos.
La fuente Markdown continúa siendo canónica y el PDF es un artefacto derivado.

## Verificación automática final

El archivo final fue reabierto con `pypdf` y revisado con Poppler:

- páginas: `28`;
- tamaño: `191216` bytes;
- SHA-256: `B6C420A877FEA6B2758E491D92674E9C39EA24CD82E5D56DF3BC55022262E617`;
- formato: A4, PDF 1.4, sin cifrado, formularios ni JavaScript;
- título: `Manual paso a paso de pruebas integrales de Logixone`;
- autor: `Proyecto Logixone Jakarta 11`;
- texto extraíble en las 28 páginas;
- cero páginas vacías;
- ausencia de U+FFFD y guiones Unicode problemáticos;
- ausencia de literales `access_token=` y `client_secret=` en el texto extraído.

## Revisión visual

Se renderizaron las 28 páginas completas a PNG a 120 DPI. Se inspeccionaron cuatro
hojas de contacto y páginas individuales a resolución original, incluidas portada,
índice, comandos largos, tabla de hashes V1-V5, OIDC, Playwright y cierre.

La primera revisión detectó:

1. listas no ordenadas representadas con el número `1`;
2. un comando OIDC partido dentro de un nombre de archivo;
3. contraste insuficiente del identificador `J11-S4-08` en la portada.

Se corrigieron los marcadores, el ajuste tipográfico de código y el color del
identificador. Después se regeneró el PDF, se volvieron a renderizar las 28 páginas
y se repitió la inspección completa.

El resultado final no presenta contenido cortado, solapamientos, páginas vacías,
tablas fuera de margen, comandos partidos dentro de rutas, pies superpuestos ni
caracteres dañados.

## Archivos creados o actualizados

- `docs/runbooks/manual-pruebas-j11-s4-08.md`;
- `docs/output/pdf/manual-pruebas-j11-s4-08.pdf`;
- `docs/evidence/manual-pruebas-j11-s4-08-pdf.md`;
- índices de documentación, runbooks y evidencia;
- referencia operativa en la estrategia de pruebas de Sprint 4.

## Uso permitido

El manual puede utilizarse para implementar y ejecutar `J11-S4-08`. No reemplaza
la evidencia futura del gate, la demo visual real ni el PDF obligatorio de cierre
del Sprint. Mientras esos pasos no estén verdes, Sprint 4 continúa en curso.
