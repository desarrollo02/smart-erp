# Evidencia J11-S10-01 — Contrato neutral versionado de floorplans

- Fecha: 2026-08-14
- Estado: Implementada y validada automáticamente
- Validación independiente: pendiente dentro del calendario autorizado
- Materialización: `.tools/tmp/validation/J11-S10-01/`
- Alcance: `plugin-api` y documentación técnica; sin renderer ni plugins migrados

## Resultado técnico

`PluginApiVersion.CURRENT` avanzó de `0.4.3` a `0.4.4`, conservando los rangos
vivos `[0.4.0,0.5.0)`. Se agregaron tipos Java puros para:

- propósito de pantalla;
- rol semántico de regiones;
- texto, fecha, cantidad, dinero, estado, referencia buscable, líneas y resumen;
- intención y énfasis de acciones;
- confirmación simple o con motivo;
- experiencia v2 y estado dinámico por elemento.

`ScreenDefinition` tiene un quinto componente opcional, pero conserva el
constructor de cuatro argumentos. Contratos 1.x no admiten experiencia v2;
contratos 2.x o posteriores la requieren. La validación v2 comprueba regiones,
referencias semánticas y correspondencia exacta de acciones.

`ScreenInteraction.Result` conserva el constructor de siete argumentos y agrega
un mapa inmutable `elementStates`. La ausencia de una entrada preserva el estado
estático v1. Los estados contradictorios se rechazan al construir el valor.

## Compatibilidad encontrada y corregida

La primera compilación focal falló porque una lista reasignada se capturaba en una
lambda. Se materializó una copia local efectiva y se repitió el gate: 27/27 verde.

El primer gate amplio encontró una prueba del kernel que construye
deliberadamente una pantalla v1 con elementos duplicados para comprobar el
diagnóstico del registro. La nueva validación había adelantado ese rechazo al
constructor público. Se restringió la validación estructural nueva a experiencias
v2; el punto de diagnóstico v1 quedó intacto. Después de la corrección:

- `PluginCatalogResolverTest`: 13/13 verde;
- `plugin-api`: 27/27 verde;
- ningún consumidor v1 requirió cambios.

El primer intento del gate arquitectónico también alcanzó el límite externo de
120 segundos sin prueba fallida. Se repitió con un límite suficiente y sólo esa
ejecución completa se considera evidencia verde.

## Gates ejecutados

```powershell
.\mvnw.cmd -f .tools/tmp/validation/J11-S10-01/pom.xml -pl plugin-api -am test
.\mvnw.cmd -f .tools/tmp/validation/J11-S10-01/pom.xml -pl kernel-domain -am "-Dtest=PluginCatalogResolverTest" "-Dsurefire.failIfNoSpecifiedTests=false" test
.\mvnw.cmd -f .tools/tmp/validation/J11-S10-01/pom.xml -pl tests/architecture-tests -am test
.\mvnw.cmd -f .tools/tmp/validation/J11-S10-01/pom.xml verify
```

Resultados finales:

| Gate | Resultado |
|---|---|
| `plugin-api` focal | 27 pruebas, 0 fallos/errores/omitidas |
| regresión del registro | 13 pruebas, 0 fallos/errores/omitidas |
| arquitectura/composición | 24/24 módulos; 34 pruebas de arquitectura verdes |
| reactor `verify` | 28/28 módulos; `BUILD SUCCESS` en 2 min 45 s |
| reportes JUnit del corte | 145 reportes; 539 pruebas; 0 fallos, errores u omitidas |

ArchUnit confirmó que `plugin-api` sólo depende de Java y de sus propios
contratos. El WAR default se empaquetó y todos los plugins v1 compilaron. No
correspondían PostgreSQL/Testcontainers, JTA/OIDC, Docker/Compose ni Playwright:
esta historia no cambia dominio, persistencia, composición efectiva, handler,
shell ni runtime.

## Documentación

Se actualizaron el manual técnico y la guía de implementación con la anatomía
v1/v2, invariantes y versión 0.4.4. El manual de usuario fue revisado y no cambia:
el producto visible continúa usando el renderer v1 hasta J11-S10-02 y los pilotos
de J11-S10-03/J11-S10-04.

## Siguiente trabajo

J11-S10-02 debe registrar renderers cerrados del shell para los propósitos v2,
rechazar versiones/propósitos no soportados y demostrar que los maestros v1 no
regresan. Todavía no corresponde migrar Inventario o Compras.
