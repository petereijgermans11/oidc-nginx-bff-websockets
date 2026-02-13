# OIDC Example

Open het poject in een devcontainer

## Start project Docker Compose

```sh
docker compose up --build --no-deps --force-recreate
```

## Greeter App

Open de browser naar http://localhost:8090

## Greeter Service

Ga naar [greeter.rest](greeter-service/resources/greeter.rest), login en gebruik het access_token om een greet request te versturen

## Gateway/BFF

De gateway draait op http://localhost:8008

## Keycloak

Open de browser naar http://localhost:8000 en login met admin/admin
