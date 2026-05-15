FROM quay.io/keycloak/keycloak:24.0.5

ENV KC_HEALTH_ENABLED=true
ENV KC_METRICS_ENABLED=true

EXPOSE 8080

ENTRYPOINT ["/opt/keycloak/bin/kc.sh", "start"]