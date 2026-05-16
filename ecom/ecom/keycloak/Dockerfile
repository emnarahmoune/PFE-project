FROM quay.io/keycloak/keycloak:26.0.7

ENV KC_DB=mysql
ENV KC_DB_URL=jdbc:mysql://yamanote.proxy.rlwy.net:41925/keycloakdb
ENV KC_DB_USERNAME=root
ENV KC_DB_PASSWORD=jjSBExZKZlqveOOfnVwwgXWKVYdWgyHm

ENV KEYCLOAK_ADMIN=admin
ENV KEYCLOAK_ADMIN_PASSWORD=admin123456

ENV KC_CACHE=local

COPY import/ /opt/keycloak/data/import/

EXPOSE 8080

ENTRYPOINT ["/opt/keycloak/bin/kc.sh"]
CMD ["start-dev", "--http-host=0.0.0.0", "--http-port=8080", "--import-realm"]