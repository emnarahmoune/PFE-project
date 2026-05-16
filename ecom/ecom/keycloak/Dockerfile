FROM quay.io/keycloak/keycloak:26.5.5

ENV KC_DB=mysql
ENV KC_DB_URL=jdbc:mysql://yamanote.proxy.rlwy.net:41925/keycloakdb
ENV KC_DB_USERNAME=root
ENV KC_DB_PASSWORD=jjSBExZKZlqveOOfnVwwgXWKVYdWgyHm

ENV KC_DB_POOL_INITIAL_SIZE=1
ENV KC_DB_POOL_MIN_SIZE=1
ENV KC_DB_POOL_MAX_SIZE=5

ENV KC_BOOTSTRAP_ADMIN_USERNAME=admin
ENV KC_BOOTSTRAP_ADMIN_PASSWORD=admin123456

ENV KC_CACHE=local
ENV KC_PROXY_HEADERS=xforwarded
ENV KC_HTTP_ENABLED=true
ENV KC_HOSTNAME_STRICT=false

RUN /opt/keycloak/bin/kc.sh build

EXPOSE 8080

ENTRYPOINT []
CMD ["/opt/keycloak/bin/kc.sh", "start", "--optimized", "--http-enabled=true", "--http-host=0.0.0.0", "--http-port=8080", "--hostname-strict=false", "--proxy-headers=xforwarded"]