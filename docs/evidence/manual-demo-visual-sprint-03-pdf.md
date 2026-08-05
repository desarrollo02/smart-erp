# Evidencia de generación y revisión del manual PDF de demo visual

- Fecha: 2026-07-28
- Estado: completado y verificado
- Alcance: manual operativo para presentar la candidata visual de Sprint 3
- Fuente canónica: [manual de demo visual](../runbooks/manual-demo-visual-sprint-03.md)
- PDF derivado: [manual-demo-visual-sprint-03.pdf](../output/pdf/manual-demo-visual-sprint-03.pdf)

## Objetivo

Entregar un guion que permita realizar la demo sin improvisar, indicando en orden
qué preparar, qué acción ejecutar, qué elemento mostrar, qué explicar, qué no
afirmar y cómo recuperarse ante fallos comunes.

Este documento no sustituye la validación independiente ni el PDF obligatorio de
cierre de Sprint. `guia-estructura-repositorio-logixone.pdf` no fue reemplazado.

## Fuentes utilizadas

- estado y resultados de `J11-S3-08`;
- runbooks de Keycloak/OIDC y del shell Jakarta Faces;
- decisiones ADR-0005 y ADR-0007;
- capturas verificadas por Playwright de A, B, 720 px y 375 px;
- URL, identidades ficticias y comportamiento observado en la candidata local.

No se copió al manual ninguna contraseña, token, cookie ni valor sensible. La
contraseña se referencia solamente por su archivo local ignorado.

## Resultado

El PDF contiene 15 páginas A4:

1. portada y alcance;
2. objetivo y mensaje central;
3. preflight de Compose, health y credenciales;
4. cronograma de 12 a 15 minutos;
5. login y selección empresarial;
6. workspace y pantalla compuesta;
7. variante A con captura;
8. variante B con captura;
9. cambio de empresa y responsive con capturas;
10. logout y escenarios de autorización;
11. respuestas sugeridas;
12. límites de comunicación;
13. recuperación y contingencia;
14. versión corta de 5 minutos;
15. tarjeta rápida del presentador.

## Verificación automática

Se reabrió el archivo final con `pypdf` y se comprobó:

- páginas: `15`;
- tamaño: `733123` bytes;
- SHA-256: `FE71015C9B5712E74FCC276CB916DB4BD4F051539087E57F061B46B999BFEC09`;
- título: `Manual para presentar la demo visual de Logixone`;
- autor: `Proyecto Logixone Jakarta 11`;
- texto extraíble y no vacío en las 15 páginas;
- ausencia del secreto local en el texto extraído;
- ausencia de caracteres de reemplazo y guiones Unicode problemáticos.

## Revisión visual

El PDF se renderizó completamente con Poppler a 120 DPI, produciendo 15 imágenes.
Se inspeccionaron portada, títulos, pies, tablas, recuadros y las cuatro capturas.

La primera revisión detectó dos defectos:

1. solapamiento de las listas en la página de límites de comunicación;
2. contraste insuficiente en el recuadro final de la tarjeta rápida.

Ambos se corrigieron, se regeneró el PDF y se volvieron a revisar las páginas
afectadas a resolución original. El resultado final no presenta páginas vacías,
contenido cortado, texto solapado, capturas deformadas ni contraste ilegible.

## Archivos creados o modificados

- `docs/runbooks/manual-demo-visual-sprint-03.md`;
- `docs/output/pdf/manual-demo-visual-sprint-03.pdf`;
- `docs/evidence/manual-demo-visual-sprint-03-pdf.md`;
- índices de `docs/` y `docs/runbooks/`.

## Siguiente uso permitido

Utilizar el PDF como guion de presentación de la candidata local. Antes de cada
reunión se debe ejecutar el preflight documentado y copiar la contraseña fuera de
la pantalla compartida. No usar este artefacto para declarar el Sprint cerrado ni
para promover la imagen a producción.
