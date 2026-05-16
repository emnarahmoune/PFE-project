FROM quay.io/keycloak/keycloak:24.0.5

ENV KC_DB=mysql
ENV KC_DB_URL_HOST=yamanote.proxy.rlwy.net
ENV KC_DB_URL_PORT=41925
ENV KC_DB_URL_DATABASE=keycloakdb
ENV KC_DB_USERNAME=root
ENV KC_DB_PASSWORD=jjSBExZKZlqveOOfnVwwgXWKVYdWgyHm

ENV KC_HTTP_ENABLED=true
ENV KC_HOSTNAME_STRICT=false
ENV KC_PROXY=edge
ENV KC_HEALTH_ENABLED=true
ENV KC_METRICS_ENABLED=true
ENV KC_SPI_CONNECTIONS_JPA_LEGACY_MIGRATION_STRATEGY=manual

RUN /opt/keycloak/bin/kc.sh build

EXPOSE 8080

ENTRYPOINT ["/opt/keycloak/bin/kc.sh", "start", "--optimized", "--http-port=8080"]