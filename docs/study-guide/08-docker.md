# 8. Docker

## Conceitos

| Termo | Significado |
|-------|-------------|
| Imagem | Pacote imutável com o sistema de arquivos e o comando de inicialização |
| Container | Uma instância em execução de uma imagem |
| Dockerfile | Receita para construir uma imagem |
| Volume | Armazenamento que sobrevive à remoção do container (dados do PostgreSQL) |
| Rede | Containers na mesma rede se encontram pelo nome (`http://order-service:8084`) |
| Compose | Define vários containers, redes e volumes em um único YAML |

## Multi-stage build (`infrastructure/docker/service.Dockerfile`)

```dockerfile
FROM maven:3.9.9-eclipse-temurin-21 AS build      # estágio 1: compila
RUN mvn -pl "${SERVICE}" -am -DskipTests package

FROM eclipse-temurin:21-jre-alpine AS runtime     # estágio 2: só o necessário para rodar
COPY --from=build /workspace/${SERVICE}/target/${SERVICE}.jar /app/app.jar
USER app                                          # nunca rode como root
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "/app/app.jar"]
```

A imagem final não contém Maven, JDK nem código-fonte: fica menor e mais segura.

## Docker Compose (`docker-compose.yml`)

- **Âncoras YAML** (`x-service-env: &service-env` e `<<: *service-env`) evitam repetir configuração.
- **Variáveis obrigatórias**: `${JWT_SECRET:?mensagem}` faz o compose parar com uma mensagem clara se faltar
  o `.env`.
- **Health checks**: cada serviço responde em `/actuator/health/readiness`.
- **`depends_on: condition: service_healthy`**: o Order só sobe depois de Customer, Product e Inventory estarem
  saudáveis, e o Gateway depois de todos.
- **Init do banco**: os scripts em `/docker-entrypoint-initdb.d` rodam na primeira subida e criam os 4 bancos.

## Comandos do dia a dia

```bash
docker compose up --build            # constrói e sobe tudo
docker compose up -d                 # em segundo plano
docker compose ps                    # status e saúde
docker compose logs -f api-gateway   # logs
docker compose restart order-service
docker compose down                  # para (mantém os dados)
docker compose down -v               # para e APAGA o volume do banco
```

## Exercícios

1. Rode `docker images` e compare o tamanho da imagem de um serviço com o de `maven:3.9.9-eclipse-temurin-21`.
2. Adicione limites de memória (`deploy.resources.limits.memory: 512m`) e observe o efeito do
   `MaxRAMPercentage`.
3. Adicione um container do **pgAdmin** ao compose, na rede `backend`, e conecte-se ao `postgres`.
4. Faça o build com o mesmo Dockerfile para outro serviço:
   `docker build -f infrastructure/docker/service.Dockerfile --build-arg SERVICE=product-service -t teste .`
