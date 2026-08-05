# Keycloak externo para desarrollo y demo

## Baseline fijado

- Producto: Keycloak `26.7.0`.
- Imagen: `quay.io/keycloak/keycloak:26.7.0`.
- Plataforma ejecutable: `linux/amd64`.
- Digest ejecutable: `sha256:26939e1318d6f008fc2ee6e10cec1cf8f1ba8a21846c1bc81b91ed0506bc2a7a`.
- Índice OCI publicado: `sha256:0f198be292568439d700cdbfb893e69a6009bb43a94a06a945b1d3d506c76b13`.
- Origen: registro oficial `quay.io/keycloak/keycloak`.
- Licencia del producto: Apache License 2.0. La imagen no se incorpora al WAR ni a la imagen de Smart ERP; se opera como servicio externo.

El digest ejecutable se obtuvo el 2026-07-28 con
`docker buildx imagetools inspect quay.io/keycloak/keycloak:26.7.0`. La etiqueta
permite leer la versión y el digest impide que esa referencia cambie de contenido.

## Modelo local

Compose inicia Keycloak con `start-dev --import-realm`, escucha en
`http://keycloak.localhost:8180` y conserva su almacén `dev-file` en el volumen
nombrado `keycloak-data`. El alias `keycloak.localhost` existe en la red interna
`identity` y los navegadores resuelven el sufijo `.localhost` hacia loopback; por
eso el mismo issuer es válido para WildFly y para el navegador.

Este almacén es exclusivamente para desarrollo y demo. Una implantación compartida
o productiva debe usar una base PostgreSQL administrada, TLS, hostname público,
backups, rotación de secretos, alta disponibilidad y límites de recursos acordes
con la operación.

## Importación declarativa

`import/logixone-realm.json` declara un realm, un cliente confidencial, el mapper
de audience y tres identidades ficticias destinadas exclusivamente a la matriz de
demo. No versiona ninguna contraseña: el entrypoint obtiene la contraseña común de
esas identidades, la contraseña administrativa y el secreto del cliente desde
archivos montados en `/run/secrets/`, y los expone solo como variables del proceso
para sustituir los placeholders durante la primera importación.

El cliente usa Authorization Code Flow como cliente confidencial. La integración
administrada de WildFly 41 no envía PKCE desde el modelo de
`elytron-oidc-client`, por lo que el realm no lo exige. Se mantienen secreto de
cliente externo, redirects exactos, audience, RS256, validación de issuer y
expiración, y rotación del identificador de sesión. Producción requiere además HTTPS.

Keycloak omite la importación de un realm que ya existe. Por lo tanto:

- recrear el contenedor sin retirar `keycloak-data` conserva el estado;
- retirar el volumen vuelve a importar el baseline declarativo;
- cambiar el JSON o el archivo del secreto no modifica automáticamente un realm ya
  persistido;
- los cambios posteriores y la rotación deben ejecutarse mediante un procedimiento
  administrativo versionado y verificable.

No utilizar `docker compose down -v` salvo que se busque de forma explícita borrar
el estado local de PostgreSQL y Keycloak.
