FROM docker.io/library/eclipse-temurin:21.0.11_10-jdk-noble@sha256:15ec53e5373fbede42fb2c1ec5b7e3ccbe1b0c9ad232ed67a16ed1b8b47070b6 AS builder

WORKDIR /workspace

COPY --chmod=0755 mvnw ./mvnw
COPY .gitattributes ./.gitattributes
COPY .mvn/ ./.mvn/
COPY infra/docker/Dockerfile infra/docker/Dockerfile.migrator infra/docker/unzip-with-jar infra/docker/logixone-entrypoint.sh ./infra/docker/
COPY infra/keycloak/keycloak-entrypoint.sh ./infra/keycloak/
COPY --chmod=0755 infra/docker/unzip-with-jar /usr/local/bin/unzip
COPY pom.xml ./pom.xml
COPY platform-bom/ ./platform-bom/
COPY plugin-api/ ./plugin-api/
COPY tools/plugin-scaffold/ ./tools/plugin-scaffold/
COPY kernel-api/ ./kernel-api/
COPY kernel-domain/ ./kernel-domain/
COPY kernel-application/ ./kernel-application/
COPY kernel-infrastructure-jakarta/ ./kernel-infrastructure-jakarta/
COPY web-shell/ ./web-shell/
COPY migrator/ ./migrator/
COPY plugins/ ./plugins/
COPY distribution/ ./distribution/
COPY tests/ ./tests/

ARG LOGIXONE_BUILD_MODE=verified
ARG LOGIXONE_MAVEN_PROFILE=none

RUN --mount=type=cache,id=logixone-maven-tools-3.9.16,target=/workspace/.tools,sharing=locked \
    find . -path './.tools' -prune -o -type f -exec chmod 0644 {} + \
    && chmod 0755 ./mvnw \
    && if [ -d .tools/maven-wrapper-home/wrapper/dists ]; then \
        find .tools/maven-wrapper-home/wrapper/dists -type f -path '*/bin/mvn' -exec chmod 0755 {} +; \
    fi \
    && case "$LOGIXONE_BUILD_MODE:$LOGIXONE_MAVEN_PROFILE" in \
        verified:none) MAVEN_USER_HOME=/workspace/.tools/maven-wrapper-home ./mvnw -B verify ;; \
        verified:with-reference-plugin) MAVEN_USER_HOME=/workspace/.tools/maven-wrapper-home ./mvnw -B -Pwith-reference-plugin verify ;; \
        verified:with-screen-customization-plugins) MAVEN_USER_HOME=/workspace/.tools/maven-wrapper-home ./mvnw -B -Pwith-screen-customization-plugins verify ;; \
        verified:with-business-partners-demo) MAVEN_USER_HOME=/workspace/.tools/maven-wrapper-home ./mvnw -B -Pwith-business-partners-demo verify ;; \
        verified:with-commercial-catalog-demo) MAVEN_USER_HOME=/workspace/.tools/maven-wrapper-home ./mvnw -B -Pwith-commercial-catalog-demo verify ;; \
        verified:with-inventory-demo) MAVEN_USER_HOME=/workspace/.tools/maven-wrapper-home ./mvnw -B -Pwith-inventory-demo verify ;; \
        verified:with-purchasing-demo) MAVEN_USER_HOME=/workspace/.tools/maven-wrapper-home ./mvnw -B -Pwith-purchasing-demo verify ;; \
        visual-candidate:none) MAVEN_USER_HOME=/workspace/.tools/maven-wrapper-home ./mvnw -B -DskipTests package ;; \
        visual-candidate:with-reference-plugin) MAVEN_USER_HOME=/workspace/.tools/maven-wrapper-home ./mvnw -B -DskipTests -Pwith-reference-plugin package ;; \
        visual-candidate:with-screen-customization-plugins) MAVEN_USER_HOME=/workspace/.tools/maven-wrapper-home ./mvnw -B -DskipTests -Pwith-screen-customization-plugins package ;; \
        visual-candidate:with-business-partners-demo) MAVEN_USER_HOME=/workspace/.tools/maven-wrapper-home ./mvnw -B -DskipTests -Pwith-business-partners-demo package ;; \
        visual-candidate:with-commercial-catalog-demo) MAVEN_USER_HOME=/workspace/.tools/maven-wrapper-home ./mvnw -B -DskipTests -Pwith-commercial-catalog-demo package ;; \
        visual-candidate:with-inventory-demo) MAVEN_USER_HOME=/workspace/.tools/maven-wrapper-home ./mvnw -B -DskipTests -Pwith-inventory-demo package ;; \
        visual-candidate:with-purchasing-demo) MAVEN_USER_HOME=/workspace/.tools/maven-wrapper-home ./mvnw -B -DskipTests -Pwith-purchasing-demo package ;; \
        *) echo "Unsupported LOGIXONE_BUILD_MODE or LOGIXONE_MAVEN_PROFILE" >&2; exit 64 ;; \
    esac \
    && test -s distribution/logixone-war/target/logixone.war \
    && postgresql_jdbc_version="$(sed -n 's:.*<postgresql.jdbc.version>\([^<]*\)</postgresql.jdbc.version>.*:\1:p' pom.xml)" \
    && test -n "$postgresql_jdbc_version" \
    && mkdir -p runtime-dependencies \
    && cp ".tools/maven-repository/org/postgresql/postgresql/${postgresql_jdbc_version}/postgresql-${postgresql_jdbc_version}.jar" \
        runtime-dependencies/postgresql.jar

FROM quay.io/wildfly/wildfly:41.0.0.Final-jdk21@sha256:7131f9e6b0d9d2e22caa57ddc729966f552cd7771432a9d4cefc72fbaec8d7d0 AS runtime

LABEL org.opencontainers.image.title="Logixone Jakarta 11" \
      org.opencontainers.image.description="ERP modular basado en plugins sobre Jakarta EE 11" \
      org.opencontainers.image.version="0.1.0-SNAPSHOT"

COPY --from=builder --chown=jboss:root \
    /workspace/runtime-dependencies/postgresql.jar \
    /opt/jboss/wildfly/modules/system/layers/base/org/postgresql/main/postgresql.jar
COPY --chown=jboss:root \
    infra/wildfly/modules/org/postgresql/main/module.xml \
    /opt/jboss/wildfly/modules/system/layers/base/org/postgresql/main/module.xml
COPY --chown=jboss:root \
    infra/wildfly/configure-runtime.cli \
    /opt/jboss/wildfly/bin/configure-logixone-runtime.cli

RUN /opt/jboss/wildfly/bin/jboss-cli.sh \
        --file=/opt/jboss/wildfly/bin/configure-logixone-runtime.cli \
    && rm /opt/jboss/wildfly/bin/configure-logixone-runtime.cli \
    && rm -rf /opt/jboss/wildfly/standalone/configuration/standalone_xml_history

COPY --from=builder --chown=jboss:root \
    /workspace/distribution/logixone-war/target/logixone.war \
    /opt/jboss/wildfly/standalone/deployments/logixone.war
COPY --chmod=0755 --chown=jboss:root \
    infra/docker/logixone-entrypoint.sh \
    /opt/jboss/container/logixone-entrypoint.sh

EXPOSE 8080

USER jboss

ENTRYPOINT ["/opt/jboss/container/logixone-entrypoint.sh"]
CMD ["/opt/jboss/wildfly/bin/standalone.sh", "--stability=preview", "-b", "0.0.0.0"]
