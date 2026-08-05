# Demo candidata de definiciones del catálogo - J11-S8-C02

- Estado: decimonoveno corte validado; J11-S8-C02 y Sprint 8 permanecen abiertos
- Fecha: 2026-08-04
- Perfil físico: `with-inventory-demo`
- Usuario ficticio: `demo.empresas.ab`
- Permisos: `commercial_catalog.definitions.manage` y `commercial_catalog.items.manage`
- Evidencia objetivo: `../evidence/screenshots/J11-S8-C02-variant-assignment/e2e/`

## Objetivo

Demostrar que unidades, categorías, marcas, etiquetas y familias de variantes
tienen administradores visibles y autorizados, que una unidad nueva aparece en
los selectores consumidores y que las cuatro definiciones simples pueden
inactivarse/reactivarse sin SQL, reinicio ni acceso a tablas privadas. El mismo
recorrido demuestra que una definición simple puede revisar nombre/estructura,
consultar su historial sin cambiar código o identidad y ser reemplazada por una
sucesora sin reescribir referencias anteriores, y que un perfil tributario conserva identidad e historia al
crear una revisión explícita, que sus versiones pueden consultarse en orden y que
el perfil puede inactivarse/reactivarse. Una familia permite además crear una
revisión completa de nombre y atributos, consultar todas sus estructuras
históricas sin reinterpretar asignaciones existentes y asignar la revisión activa
a un artículo con valores tipados.

La candidata permite consulta, alta e inactivación/reactivación de definiciones
simples y retorno seguro desde selectores renderizados por plugins y desde los 11
usos nativos administrables. También demuestra que una familia conserva identidad
y estructura histórica al revisarse, inactivarse y reactivarse. El decimonoveno
corte demuestra además la asignación versionada a un artículo; no presenta como
disponible la generación de múltiples SKU ni altas para los siete selectores
nativos cerrados o de despliegue.

## Preparación segura

1. Use únicamente las empresas y usuarios ficticios de la demo.
2. Compruebe que `commercial_catalog` está activo y que el rol posee
   `commercial_catalog.definitions.manage`.
3. Levante el perfil `with-inventory-demo` sin eliminar volúmenes. La evidencia de
   este corte se obtuvo con la composición aislada `logixone-vfh`, puertos
   28080/9180 y volúmenes nuevos; el entorno habitual no se migró.
4. Verifique:
   - `http://localhost:28080/logixone/health/live`;
   - `http://localhost:28080/logixone/health/ready`.
5. Ambos deben responder HTTP 200 y `UP`.

## Guion para presentar

1. Inicie sesión, seleccione la empresa ficticia A y muestre el menú fusionado.
2. Abra **Definiciones del catálogo**. Explique que la opción proviene del plugin
   y sólo aparece con activación y permiso vigentes.
3. Muestre el directorio único de unidades, categorías, marcas y etiquetas.
4. Filtre por texto, tipo y estado para explicar que no son valores hardcodeados
   dentro de cada formulario.
5. Pulse **Nueva definición**, elija **Unidad** e ingrese un código y nombre
   ficticios únicos. Seleccione la escala de decimales y registre.
6. Confirme el detalle creado y vuelva al directorio.
7. Regrese a **Nueva definición**, elija **Etiqueta**, regístrela y filtre el
   directorio por **Etiqueta** para demostrar que el maestro no está hardcodeado.
8. Abra **Familias de variantes** y explique que una familia es la plantilla que
   define las características comunes de futuras variantes, no una variante ni un
   artículo vendible. Use el ejemplo *Calzado*: la familia define `COLOR` y
   `TALLA`; una variante futura podría ser *Negro / 40*.
9. Pulse **Nueva familia** y agregue en orden los atributos ficticios `COLOR`
   (Texto) y `TALLA` (Número).
10. Muestre **Atributos preparados**, registre la familia y verifique ambos
    atributos en el detalle. Vuelva al directorio y filtre por su código.
11. Abra **Nueva revisión**. Confirme que el borrador contiene `COLOR` y `TALLA`,
    cambie el nombre, retire ambos atributos y agregue `NUMERO` (Número,
    obligatorio). Pulse **Crear revisión** y compruebe que identidad y código no
    cambiaron.
12. Abra **Historial**. Verifique primero la versión vigente con `NUMERO` y luego
    la versión original con `COLOR/TALLA`, ambas completas y en solo lectura.
13. Seleccione **Estado**, pulse **Inactivar familia** y confirme el mensaje.
    Vuelva a **Resumen** y compruebe que `NUMERO`, identidad y estado inactivo
    siguen visibles.
14. Vuelva al directorio, filtre por **Inactivas**, abra la familia y pulse
    **Reactivar familia** desde **Estado**.
15. Aclare que la plantilla se puede asignar a un artículo, pero no crea por sí
    sola múltiples SKU, existencias o precios.
16. Abra **Artículos y servicios**, inicie un alta, complete código, nombre,
    descripción, tipo y alcance y pulse **Agregar o administrar** junto a
    **Unidad base**.
17. En Definiciones registre una unidad ficticia distinta y use la banda
    **Administración contextual** para volver a Artículos y servicios. Compruebe
    **Opciones actualizadas**, el borrador recuperado y la unidad nueva disponible.
18. Complete el alta como **Producto**, seleccione unidad y perfil tributario y
    pulse **Registrar**.
19. Abra **Variantes**, elija la familia activa, pulse **Mostrar atributos** e
    ingrese `NUMERO=42.00`. Pulse **Asignar variante** y confirme en **Resumen**
    que la revisión exacta conserva el valor normalizado `NUMERO=42`.
20. Abra una lista de precios y muestre la misma unidad en el selector de entrada.
21. Abra la unidad creada, seleccione **Nueva revisión**, cambie el nombre y la
    escala decimal y pulse **Crear revisión**. Confirme que el código no cambió.
22. Abra **Historial** y compruebe que aparecen la versión actual y la original,
    ordenadas desde la más reciente y en modo de solo lectura.
23. Seleccione **Reemplazar**, ingrese código/nombre nuevos y mantenga la escala.
    Confirme la sucesora; vuelva al directorio, filtre el origen como **Inactivo**
    y muestre **Reemplazada por**. Abra el artículo creado y compruebe que conserva
    el código de unidad anterior. Explique que no hubo borrado ni reasignación
    silenciosa de referencias históricas.
24. Abra el perfil tributario creado, seleccione **Nueva revisión**, cambie el
    tratamiento, la descripción y la vigencia, y pulse **Crear revisión**. Confirme
    que el código, nombre e identidad no cambiaron.
25. Seleccione **Historial** y compruebe que la versión actual aparece primero y la
    anterior como histórica, ambas con tratamiento, descripción y vigencia.
26. Seleccione **Estado**, pulse **Inactivar**,
    búsquelo mediante el filtro **Inactivos** y pulse **Reactivar**. Explique que
    cada cambio creó una nueva revisión y conservó las referencias históricas.
27. Abra **Autoridad global**, conserve usuario y rol en **Asignar rol global** y
    pulse **Agregar o administrar** junto a **Usuario**. Registre un usuario
    ficticio en **Seguridad empresarial**, mantenga el contexto durante el POST y
    use **Volver a asignación de rol global**. Compruebe **Opciones actualizadas**
    y que el borrador permitido fue recuperado.
28. Cambie a 720 y 375 px; filtros, lista, alta, historial, retorno y acciones deben reordenarse sin
   overflow horizontal normal.
29. Como control negativo, retire temporalmente
    `commercial_catalog.definitions.manage`, renueve la sesión y confirme que el
    menú desaparece y la ruta directa es denegada. Restaure el permiso al terminar.

## Ejecución automática

```powershell
$projectRoot=(Resolve-Path '.').Path

.\mvnw.cmd -Pvisual-e2e -pl tests/e2e-tests `
  "-Dit.test=CommercialCatalogVisualIT" `
  "-Dlogixone.commercial-catalog.e2e=true" `
  "-Dlogixone.app-url=http://localhost:28080/logixone/faces/app/index.xhtml" `
  "-Dlogixone.admin-url=http://localhost:28080/logixone/faces/admin/index.xhtml" `
  "-Dlogixone.demo-user-password-file=$projectRoot/.tools/secrets/demo-user-password.txt" `
  "-Dlogixone.evidence-dir=$projectRoot/docs/evidence/screenshots/J11-S8-C02-variant-assignment/e2e" `
  "-Dlogixone.playwright.executable=$projectRoot/.tools/playwright/chromium-1228/chrome-win64/chrome.exe" `
  verify
```

El resultado final usó la imagen
`logixone/app:j11-s8-c02-variant-assignment`, digest
`sha256:f457b3d2bf150df1bbfc2283f1fb15be937d63f237781d2932f9174e70a447da`.
La prueba final quedó verde en 94,89 segundos y produjo 77 PNG (8.967.738 bytes).
La suite crea y revisa una familia, lee las estructuras original y vigente en el
historial, recorre su ciclo y mantiene el escenario acumulado de unidad, perfil,
retorno y seguridad negativa; además restaura
permisos y deja activos `commercial_catalog` e `inventory`.

## Resultado esperado y recuperación

- la definición aparece en el directorio y en los selectores de la misma empresa;
- la página no tiene overflow horizontal normal en 375, 720 y 1280 px;
- una sesión sin permiso no ve el menú ni puede abrir la ruta directa;
- liveness/readiness permanecen `UP` después del recorrido.

Si una acción falla, no repita altas ni modifique PostgreSQL directamente. Anote
pantalla, hora y mensaje, consulte health y conserve logs/capturas. No ejecute
`DELETE`, `TRUNCATE`, `DROP` ni `docker compose down --volumes`.

## Evidencia visual representativa

![Directorio expandido de definiciones](../evidence/screenshots/J11-S8-C02-tags/e2e/catalog-definitions-directory-expanded-1280.png)

![Alta compacta de etiqueta](../evidence/screenshots/J11-S8-C02-tags/e2e/catalog-tag-create-compact-375.png)

![Etiquetas filtradas en compacto](../evidence/screenshots/J11-S8-C02-tags/e2e/catalog-tags-filtered-compact-375.png)

![Directorio expandido de familias](../evidence/screenshots/J11-S8-C02-variants/e2e/variant-families-directory-expanded-1280.png)

![Directorio compacto de familias](../evidence/screenshots/J11-S8-C02-variants/e2e/variant-families-directory-compact-375.png)

![Alta compacta con dos atributos](../evidence/screenshots/J11-S8-C02-variants/e2e/variant-family-create-compact-375.png)

![Familia filtrada en compacto](../evidence/screenshots/J11-S8-C02-variants/e2e/variant-families-filtered-compact-375.png)

![Unidad inactiva en expandido](../evidence/screenshots/J11-S8-C02-lifecycle/e2e/catalog-unit-inactive-expanded-1280.png)

![Unidad inactiva en medio](../evidence/screenshots/J11-S8-C02-lifecycle/e2e/catalog-unit-inactive-medium-720.png)

![Unidad inactiva en compacto](../evidence/screenshots/J11-S8-C02-lifecycle/e2e/catalog-unit-inactive-compact-375.png)

![Perfil tributario inactivo en expandido](../evidence/screenshots/J11-S8-C02-tax-profile-lifecycle/e2e/tax-profile-inactive-expanded-1280.png)

![Perfil tributario inactivo en medio](../evidence/screenshots/J11-S8-C02-tax-profile-lifecycle/e2e/tax-profile-inactive-medium-720.png)

![Perfil tributario inactivo en compacto](../evidence/screenshots/J11-S8-C02-tax-profile-lifecycle/e2e/tax-profile-inactive-compact-375.png)

![Revisión tributaria en expandido](../evidence/screenshots/J11-S8-C02-tax-profile-revision/e2e/tax-profile-revision-expanded-1280.png)

![Revisión tributaria en medio](../evidence/screenshots/J11-S8-C02-tax-profile-revision/e2e/tax-profile-revision-medium-720.png)

![Revisión tributaria en compacto](../evidence/screenshots/J11-S8-C02-tax-profile-revision/e2e/tax-profile-revision-compact-375.png)

![Historial tributario en expandido](../evidence/screenshots/J11-S8-C02-tax-profile-history/e2e/tax-profile-history-expanded-1280.png)

![Historial tributario en medio](../evidence/screenshots/J11-S8-C02-tax-profile-history/e2e/tax-profile-history-medium-720.png)

![Historial tributario en compacto](../evidence/screenshots/J11-S8-C02-tax-profile-history/e2e/tax-profile-history-compact-375.png)

![Administrador contextual expandido](../evidence/screenshots/J11-S8-C02-selector-return/e2e/selector-return-manager-expanded-1280.png)

![Administrador contextual medio](../evidence/screenshots/J11-S8-C02-selector-return/e2e/selector-return-manager-medium-720.png)

![Administrador contextual compacto](../evidence/screenshots/J11-S8-C02-selector-return/e2e/selector-return-manager-compact-375.png)

![Borrador y opciones restaurados en compacto](../evidence/screenshots/J11-S8-C02-selector-return/e2e/selector-return-restored-compact-375.png)

![Destino nativo expandido](../evidence/screenshots/J11-S8-C02-native-selector-return/e2e/native-selector-target-expanded-1280.png)

![Destino nativo medio](../evidence/screenshots/J11-S8-C02-native-selector-return/e2e/native-selector-target-medium-720.png)

![Destino nativo compacto](../evidence/screenshots/J11-S8-C02-native-selector-return/e2e/native-selector-target-compact-375.png)

![Formulario nativo restaurado expandido](../evidence/screenshots/J11-S8-C02-native-selector-return/e2e/native-selector-restored-expanded-1280.png)

![Formulario nativo restaurado medio](../evidence/screenshots/J11-S8-C02-native-selector-return/e2e/native-selector-restored-medium-720.png)

![Formulario nativo restaurado compacto](../evidence/screenshots/J11-S8-C02-native-selector-return/e2e/native-selector-restored-compact-375.png)

![Familia inactiva con atributos en expandido](../evidence/screenshots/J11-S8-C02-variant-family-lifecycle/e2e/variant-family-inactive-expanded-1280.png)

![Familia inactiva con atributos en medio](../evidence/screenshots/J11-S8-C02-variant-family-lifecycle/e2e/variant-family-inactive-medium-720.png)

![Familia inactiva con atributos en compacto](../evidence/screenshots/J11-S8-C02-variant-family-lifecycle/e2e/variant-family-inactive-compact-375.png)

![Revisión de unidad en expandido](../evidence/screenshots/J11-S8-C02-simple-definition-history/e2e/catalog-unit-revision-expanded-1280.png)

![Revisión de unidad en medio](../evidence/screenshots/J11-S8-C02-simple-definition-history/e2e/catalog-unit-revision-medium-720.png)

![Revisión de unidad en compacto](../evidence/screenshots/J11-S8-C02-simple-definition-history/e2e/catalog-unit-revision-compact-375.png)

![Historial de unidad en expandido](../evidence/screenshots/J11-S8-C02-simple-definition-history/e2e/catalog-unit-history-expanded-1280.png)

![Historial de unidad en medio](../evidence/screenshots/J11-S8-C02-simple-definition-history/e2e/catalog-unit-history-medium-720.png)

![Historial de unidad en compacto](../evidence/screenshots/J11-S8-C02-simple-definition-history/e2e/catalog-unit-history-compact-375.png)

![Reemplazo de unidad en expandido](../evidence/screenshots/J11-S8-C02-simple-definition-replacement/e2e/catalog-unit-replacement-expanded-1280.png)

![Origen reemplazado en medio](../evidence/screenshots/J11-S8-C02-simple-definition-replacement/e2e/catalog-unit-replaced-link-medium-720.png)

![Referencia histórica conservada en compacto](../evidence/screenshots/J11-S8-C02-simple-definition-replacement/e2e/catalog-item-preserved-unit-compact-375.png)

![Revisión de familia en expandido](../evidence/screenshots/J11-S8-C02-variant-family-history/e2e/variant-family-revision-expanded-1280.png)

![Revisión de familia en medio](../evidence/screenshots/J11-S8-C02-variant-family-history/e2e/variant-family-revision-medium-720.png)

![Revisión de familia en compacto](../evidence/screenshots/J11-S8-C02-variant-family-history/e2e/variant-family-revision-compact-375.png)

![Historial estructural de familia en expandido](../evidence/screenshots/J11-S8-C02-variant-family-history/e2e/variant-family-history-expanded-1280.png)

![Historial estructural de familia en medio](../evidence/screenshots/J11-S8-C02-variant-family-history/e2e/variant-family-history-medium-720.png)

![Historial estructural de familia en compacto](../evidence/screenshots/J11-S8-C02-variant-family-history/e2e/variant-family-history-compact-375.png)

![Asignación de familia en expandido](../evidence/screenshots/J11-S8-C02-variant-assignment/e2e/catalog-item-variant-expanded-1280.png)

![Asignación de familia en medio](../evidence/screenshots/J11-S8-C02-variant-assignment/e2e/catalog-item-variant-medium-720.png)

![Asignación de familia en compacto](../evidence/screenshots/J11-S8-C02-variant-assignment/e2e/catalog-item-variant-compact-375.png)

Esta demo candidata no cierra J11-S8-C02 ni Sprint 8. Siguen pendientes
las fuentes normativas, la recongelación documental, el PDF y la decisión
posterior sobre el instalador.
