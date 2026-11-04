# syntax=docker/dockerfile:1.7
#
# Dockerfile UNICO e parametrizado para todos os servicos (ADR-0008).
# Uso: docker build -f infrastructure/docker/service.Dockerfile --build-arg SERVICE=order-service .
#
# Multi-stage build:
#   1) "build": imagem com JDK + Maven compila apenas o servico pedido e suas dependencias (-pl X -am).
#   2) "runtime": imagem pequena so com JRE e o JAR final. Ferramentas de build nao vao para producao.

ARG SERVICE

FROM maven:3.9.9-eclipse-temurin-21 AS build
ARG SERVICE
WORKDIR /workspace
COPY . .
# O cache do ~/.m2 e reaproveitado entre builds (BuildKit), evitando baixar dependencias toda vez.
# sharing=locked: o compose constroi os 5 servicos em paralelo, e o repositorio local do Maven nao suporta
# escrita concorrente (causava falhas intermitentes como "FileNotFoundException ... .lastUpdated").
# Com o lock, um build por vez usa o cache.
RUN --mount=type=cache,target=/root/.m2,sharing=locked \
    mvn -B -q -pl "${SERVICE}" -am -DskipTests package

FROM eclipse-temurin:21-jre-alpine AS runtime
ARG SERVICE
# Nunca rode a aplicacao como root dentro do container.
RUN addgroup -S app && adduser -S app -G app
WORKDIR /app
COPY --from=build /workspace/${SERVICE}/target/${SERVICE}.jar /app/app.jar
USER app
# MaxRAMPercentage: a JVM respeita o limite de memoria do container.
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "/app/app.jar"]
