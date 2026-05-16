FROM quay.io/keycloak/keycloak:26.0.7

ENV KC_DB=mysql
ENV KC_DB_URL=jdbc:mysql://yamanote.proxy.rlwy.net:41925/keycloakdb
ENV KC_DB_USERNAME=root
ENV KC_DB_PASSWORD=jjSBExZKZlqveOOfnVwwgXWKVYdWgyHm

ENV KEYCLOAK_ADMIN=admin
ENV KEYCLOAK_ADMIN_PASSWORD=admin123456

ENV KC_CACHE=local

COPY import/ /opt/keycloak/data/import/

RUN /opt/keycloak/bin/kc.sh build

EXPOSE 8080

ENTRYPOINT ["/opt/keycloak/bin/kc.sh", "start", "--optimized", "--http-enabled=true", "--hostname-strict=false", "--http-port=8080", "--http-host=0.0.0.0", "--import-realm"]