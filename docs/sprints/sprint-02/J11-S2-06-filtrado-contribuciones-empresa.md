# J11-S2-06 — Filtrado de contribuciones por empresa

- Estado: Completada
- Fecha de inicio: 2026-07-27
- Fecha de cierre: 2026-07-27
- Dependencia: `J11-S2-05` completada y verde

## Objetivo

Componer capacidades, permisos y menús únicamente desde plugins efectivos para una empresa, incluyendo solo su personalización asignada y situándola después de los plugins funcionales, con resultados diferentes y aislados sobre el mismo catálogo físico.

## Alcance

- servicio neutral de aplicación para consultar contribuciones por empresa;
- integración con `PluginRegistry` y el estado efectivo, sin modificar descriptores;
- orden estable según catálogo topológico y orden de contribuciones declarado;
- filtrado de capacidades, permisos y `MenuContribution`;
- uso del `reference-plugin` como contribuyente físico real;
- fixture de personalización para verificar selección exclusiva y orden final, sin aplicar todavía overlays de pantalla;
- fixtures de prueba adicionales para dependencias requeridas y opcionales;
- pruebas positivas y negativas con dos empresas;
- prueba conjunta de filtrado visual y guarda operativa.

## Reglas

- una contribución no se copia a tablas del kernel; se resuelve desde el descriptor vigente;
- estado persistido decide qué plugins pueden contribuir, no altera el descriptor global;
- empresa inactiva devuelve una composición vacía;
- plugin ausente o desactivado no contribuye;
- una empresa operativa incluye exactamente su plugin `CUSTOMIZATION` asignado;
- cualquier personalización no asignada a la empresa queda excluida aunque esté físicamente presente;
- la composición conserva el orden topológico entre plugins funcionales y coloca después la personalización empresarial;
- activar para empresa A no modifica el resultado de empresa B;
- no se filtran permisos por usuario todavía; solo se determina qué permisos existen por plugins efectivos;
- rutas de menú permanecen datos neutrales, sin dependencias JSF/PrimeFaces;
- una operación debe seguir pasando por la guarda aunque su menú no esté visible.

## Fuera de alcance

- renderizado de menú o UI y aplicación del overlay de pantalla, que corresponde a `J11-S2-07`;
- concesión de permisos a roles/usuarios;
- endpoint REST funcional del plugin;
- tareas programadas o listeners empresariales;
- persistencia de copias de contribuciones.

## Criterios de aceptación

- **CA-01:** el servicio recibe empresa y devuelve una vista inmutable y determinista.
- **CA-02:** empresa con `reference-plugin` activo recibe exactamente sus contribuciones declaradas.
- **CA-03:** otra empresa con el mismo plugin desactivado recibe cero contribuciones de él.
- **CA-04:** empresa inactiva recibe una composición vacía.
- **CA-05:** plugin físicamente ausente nunca aparece aunque exista una decisión persistida anterior.
- **CA-06:** dependencias requeridas respetan orden y efectividad en la misma empresa.
- **CA-07:** dependencias opcionales ausentes no producen contribuciones fantasma ni errores.
- **CA-08:** IDs duplicados se siguen rechazando al validar el catálogo, no durante el filtrado.
- **CA-09:** las colecciones no permiten mutar registro, descriptor ni estado persistido.
- **CA-10:** ocultar menú y denegar operación se prueban como controles complementarios.
- **CA-11:** variantes con y sin `reference-plugin` permanecen compilables y arrancables.
- **CA-12:** pruebas de módulo, integración, ArchUnit, WAR y `mvn verify` están verdes.
- **CA-13:** dos empresas seleccionan personalizaciones distintas sobre el mismo catálogo sin filtración cruzada.
- **CA-14:** una personalización no asignada no aporta ninguna contribución a la empresa consultada.
- **CA-15:** el orden efectivo sitúa todos los plugins funcionales antes de la personalización asignada.
- **CA-16:** ausencia o incompatibilidad de la personalización produce el resultado seguro definido por el ADR, nunca una composición estándar silenciosa.

## Gates

1. pruebas unitarias del compositor;
2. matriz de dos empresas y múltiples estados;
3. guarda más filtrado;
4. ArchUnit;
5. WAR presente/ausente;
6. `mvnw.cmd -B verify`.

## Siguiente historia permitida

`J11-S2-07` cuando contribuciones, operaciones y selección de la capa final estén aisladas por empresa.

## Resultado y cierre

Los 16 criterios quedaron satisfechos. `CompanyContributionService` proyecta una vista inmutable y determinista desde la composición efectiva existente: conserva el orden topológico de plugins funcionales, coloca al final exactamente la personalización asignada y aplana capacidades, permisos y menús sin copiar contribuciones a tablas del kernel.

Empresa inexistente, inactiva o con personalización no operativa produce una vista vacía y segura; una decisión persistida para un JAR ausente no crea contribuciones fantasma. El catálogo rechaza globalmente IDs de capacidad, permiso o menú compartidos por plugins distintos antes de cualquier filtrado.

JUnit, ArchUnit, PostgreSQL/JTA dentro de WildFly, las dos variantes del WAR y las dos imágenes Docker quedaron verdes. La prueba runtime demostró resultados distintos para dos empresas sobre el mismo catálogo físico y confirmó que ocultar un menú no sustituye la guarda operativa.

Evidencia: [J11-S2-06 — Filtrado de contribuciones por empresa](../../evidence/J11-S2-06-filtrado-contribuciones-empresa.md).
