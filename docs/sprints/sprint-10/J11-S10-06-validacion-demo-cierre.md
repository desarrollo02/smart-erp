# J11-S10-06 — Validación integral y demo oficial de Sprint 10

- Estado: Implementada y validada automáticamente; validación independiente pendiente
- Fecha de ejecución: 2026-08-20
- Perfil: `with-purchasing-demo`
- Evidencia: [resultado acumulado](../../evidence/J11-S10-06-validacion-demo-cierre.md)
- Demo: [guion reproducible](../../runbooks/demo-cierre-sprint-10.md)
- Dependencias: J11-S10-00 a J11-S10-05
- Siguiente gate: J11-S10-07, decisión explícita del instalador Windows

## Objetivo

Congelar y validar automáticamente el corte técnico de Sprint 10, demostrar en
la aplicación real los floorplans v2 de Inventario y Compras, actualizar el
paquete documental y producir los PDF derivados obligatorios sin adelantar
Ventas ni confundir validación automática con aceptación independiente.

## Alcance ejecutado

- reactor base y composición `with-purchasing-demo` completos;
- límites ArchUnit y ausencia de dependencias cruzadas prohibidas;
- imagen de aplicación y migrador construidas desde el mismo corte;
- migraciones de `core` y siete esquemas de plugins sobre PostgreSQL vacío y
  repetición idempotente;
- health, OIDC, datasource JTA y arnés JTA opt-in sobre WildFly 41;
- recorridos Playwright de maestros, administración, Inventario, Compras,
  responsive, accesibilidad, dependencias y seguridad negativa;
- fotografía de plugins, demo, retrospectiva, manuales y guía de estructura;
- limpieza exclusiva de la infraestructura efímera de esta historia.

## Criterios de aceptación y resultado

| Criterio | Resultado |
|---|---|
| El reactor base y completo termina verde | Cumplido: 28/28 módulos y 565 pruebas Surefire |
| Los límites arquitectónicos siguen vigentes | Cumplido: 34 escenarios ArchUnit |
| WAR y migrador usan la misma composición | Cumplido: perfil `with-purchasing-demo`, ocho plugins físicos |
| Las migraciones parten de cero y son repetibles | Cumplido: 23 migraciones iniciales; segunda ejecución con cero cambios |
| WildFly expone salud, OIDC y transacciones JTA reales | Cumplido: 2 health, 4 OIDC y 6 JTA |
| La demo usa recorridos reales y denegación de servidor | Cumplido: 9/9 casos Playwright |
| Compacto, medio y expandido no pierden operación | Cumplido: 171 PNG revisados en 375/599/600/720/839/840/1280 px |
| El paquete documental y sus PDF se regeneran y revisan | Cumplido; métricas y hashes en la evidencia |
| La validación independiente queda distinguida | Cumplido: continúa pendiente y el Sprint permanece abierto |
| La decisión de instalador no se infiere | Cumplido: queda reservada para J11-S10-07 |

## Hallazgos resueltos durante el gate

La primera repetición focal descubrió que el recorrido de Socios Comerciales
intentaba validar una dependencia activa sin restablecer antes Catálogo,
Inventario y Compras. Se corrigió el orden de preparación y la prueba focal volvió
a verde.

La revisión visual detectó que, en el rango medio, el texto de fuente de la barra
empresarial quedaba excesivamente angosto. El shell reorganizó la barra en dos
filas entre 600 y 839 px y se agregó una aserción geométrica para impedir la
regresión. La nueva candidata repitió Inventario y luego la matriz completa.

Una imagen intermedia construida con un nombre incorrecto de argumento Maven no
incluyó plugins y falló de forma segura al preparar la demo. Fue descartada; no
forma parte del baseline ni de la evidencia verde. La imagen final usa el
argumento canónico `LOGIXONE_MAVEN_PROFILE=with-purchasing-demo`.

## Resultado

El corte suma **586 pruebas automatizadas únicas**: 565 Surefire, 12 de
integración contra la candidata y 9 Playwright, sin fallos, errores ni omisiones.
El arnés JTA fue retirado después del gate y el WAR normal conserva únicamente
`logixone.war`.

La historia queda implementada y validada automáticamente. No cierra Sprint 10,
no promueve imágenes y no sustituye G7 independiente. El siguiente trabajo es
J11-S10-07; `sales` sólo podrá caracterizarse después de registrar esa decisión.
