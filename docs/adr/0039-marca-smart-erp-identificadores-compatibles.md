# ADR-0039 — Marca Smart ERP e identificadores técnicos compatibles

- Estado: Aceptado
- Fecha: 2026-08-05
- Decisión de producto: adoptar **Smart ERP** como nombre visible mediante un
  cambio de marca seguro
- No modifica: contratos públicos, persistencia, composición, URLs ni formatos
  operativos

## Contexto

El producto nació con el nombre visible Logixone y ese término también quedó
incorporado en miles de identificadores técnicos: paquetes Java, coordenadas
Maven, nombre y contexto del WAR, realm y cliente OIDC, datasource JNDI, unidades
de persistencia, variables, imágenes, volúmenes, scripts, propiedades, rutas y
evidencias históricas.

Cambiar todos esos identificadores junto con la marca produciría una migración
incompatible de despliegue, autenticación, datos y automatización. El responsable
de producto eligió explícitamente el 2026-08-05 el cambio seguro.

## Decisión

### 1. Marca canónica

- El nombre visible del producto es **Smart ERP**.
- El slug del repositorio y las nuevas referencias no técnicas usan `smart-erp`.
- Títulos, cabeceras, pies, mensajes, nombres descriptivos Maven, manuales y
  metadatos visibles nuevos deben usar `Smart ERP`.
- No se crearán variantes como `SmartERP`, `Smart-Erp` o `SMART ERP` salvo que un
  formato externo exija otra capitalización.

### 2. Identificadores preservados

Este rebranding no cambia:

- `py.com.logixone` ni los nombres de clases públicas;
- `logixone-parent`, módulos, `artifactId`, `logixone.war` ni `/logixone`;
- realm `logixone`, cliente `logixone-web` ni variables `LOGIXONE_*`;
- datasource `java:/jdbc/LogixoneCoreDS` ni unidades JPA;
- base, usuarios, esquemas, imágenes, redes, volúmenes, propiedades o scripts que
  ya contienen `logixone`;
- nombres y checksums de migraciones aplicadas;
- nombres de ejecutables y contenido derivado del instalador interno
  `0.8.0-internal.1`;
- PDF derivados de baselines anteriores, que permanecen como artefactos
  históricos hasta su regeneración obligatoria en el cierre del Sprint;
- nombres de archivos documentales existentes cuyo cambio rompería enlaces;
- evidencias históricas que registran el nombre vigente en el momento del hecho.

La presencia de `logixone` en uno de esos lugares no constituye por sí misma una
deuda que pueda corregirse mediante reemplazo textual.

### 3. Instalador y artefactos anteriores

El instalador interno actual pertenece a un baseline obsoleto y conserva su nombre
y textos originales. No se regenera ni reemplaza `installer/windows/current` como
parte de esta decisión. Si producto solicita un nuevo instalador al cierre, su
fuente y superficies visibles adoptarán Smart ERP, pero deberá conservar o migrar
explícitamente rutas y estado existentes conforme a ADR-0026 y ADR-0029.

Los PDF ya publicados tampoco se reescriben para aparentar pertenencia al nuevo
baseline. El PDF obligatorio de estructura y cualquier guía derivada afectada se
regenerarán y verificarán contra Smart ERP en el gate documental de cierre.

### 4. Renombrado técnico futuro

Cualquier cambio de un identificador preservado exige otra historia y, cuando
afecte arquitectura, datos o compatibilidad, otro ADR. Debe incluir inventario de
consumidores, compatibilidad temporal, migración, rollback, enlaces o redirects,
actualización de secretos/configuración y pruebas de actualización sin pérdida.

## Consecuencias

- La interfaz y documentación vigente presentan una marca coherente sin romper
  instalaciones, bookmarks, clientes OIDC, imágenes ni volúmenes.
- Código y comandos pueden mostrar `Smart ERP` junto a identificadores técnicos
  que aún contienen `logixone`; las guías deben explicar esa diferencia.
- Las pruebas de marca revisan las superficies visibles y, a la vez, protegen los
  contratos técnicos heredados que continúan siendo necesarios.
- La evidencia histórica no se reescribe para aparentar que la nueva marca existía
  antes de esta decisión.
