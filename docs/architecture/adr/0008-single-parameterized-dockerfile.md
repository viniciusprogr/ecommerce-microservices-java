# ADR-0008: Um Dockerfile parametrizado para todos os serviços

- **Status:** Aceita
- **Data:** 2026-11-04

## Contexto

Os cinco serviços são construídos da mesma forma: Maven → JAR do Spring Boot → JRE. Cinco Dockerfiles quase
idênticos divergiriam com o tempo.

## Decisão

Um único `infrastructure/docker/service.Dockerfile` com `ARG SERVICE`:

- **Estágio de build** (`maven:3.9.9-eclipse-temurin-21`): `mvn -pl ${SERVICE} -am package` compila só o serviço
  e suas dependências (o `common`). O cache do `~/.m2` é reaproveitado com BuildKit (`--mount=type=cache`).
- **Estágio de runtime** (`eclipse-temurin:21-jre-alpine`): só o JRE e o JAR, executados por um usuário sem
  privilégios.
- O `docker-compose.yml` passa `SERVICE` em `build.args` para cada serviço.

## Consequências

- (+) Uma correção no Dockerfile vale para todos os serviços.
- (+) Imagem final pequena, sem Maven nem código-fonte.
- (-) O contexto de build é o repositório inteiro (mitigado com `.dockerignore`).
