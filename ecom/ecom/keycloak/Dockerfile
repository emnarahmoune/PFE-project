FROM quay.io/keycloak/keycloak:26.5.5

ENV KC_DB=mysql
ENV KC_DB_URL=jdbc:mysql://yamanote.proxy.rlwy.net:41925/keycloakdb
ENV KC_DB_USERNAME=root
ENV KC_DB_PASSWORD=jjSBExZKZlqveOOfnVwwgXWKVYdWgyHm

ENV KC_BOOTSTRAP_ADMIN_USERNAME=admin
ENV KC_BOOTSTRAP_ADMIN_PASSWORD=admin123456

ENV KC_CACHE=local

EXPOSE 8080

CMD ["/opt/keycloak/bin/kc.sh", "start-dev", "--http-port=8080", "--http-host=0.0.0.0"]