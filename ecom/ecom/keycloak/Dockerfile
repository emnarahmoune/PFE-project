FROM quay.io/keycloak/keycloak:26.0.7

ENV KC_DB=mysql
ENV KC_DB_URL_HOST=yamanote.proxy.rlwy.net
ENV KC_DB_URL_PORT=41925
ENV KC_DB_URL_DATABASE=keycloakdb
ENV KC_DB_USERNAME=root
ENV KC_DB_PASSWORD=jjSBExZKZlqveOOfnVwwgXWKVYdWgyHm

ENV KC_HTTP_ENABLED=true
ENV KC_HOSTNAME_STRICT=false
ENV KC_PROXY_HEADERS=xforwarded
ENV KC_HOSTNAME=pfe-project-1-979y.onrender.com

COPY import/ /opt/keycloak/data/import/

RUN /opt/keycloak/bin/kc.sh build

EXPOSE 8080

ENTRYPOINT ["/opt/keycloak/bin/kc.sh", "start", "--optimized", "--http-enabled=true", "--hostname-strict=false", "--http-port=8080"]