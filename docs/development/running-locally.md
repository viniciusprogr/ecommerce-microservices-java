# Executando e depurando localmente

## Opção 1: tudo no Docker (recomendado)

```bash
cp .env.example .env      # edite as senhas e o JWT_SECRET
docker compose up --build
```

Acompanhe os logs de um serviço:

```bash
docker compose logs -f order-service
```

## Opção 2: banco no Docker e serviços na IDE

Útil para depurar com breakpoints.

1. Suba só o PostgreSQL:

   ```bash
   docker compose up -d postgres
   ```

2. Defina as variáveis de ambiente na configuração de execução da IDE (os mesmos valores do `.env`):

   | Variável | Exemplo |
   |----------|---------|
   | `DB_USERNAME` | `ecommerce` |
   | `DB_PASSWORD` | valor de `POSTGRES_PASSWORD` |
   | `JWT_SECRET` | o mesmo em todos os serviços e no gateway |
   | `GATEWAY_ADMIN_PASSWORD`, `GATEWAY_USER_PASSWORD` | só no gateway |

   O `DB_URL` padrão já aponta para `localhost:5432/<servico>_db`.

3. Execute cada `*Application.java`, ou pelo terminal:

   ```bash
   ./mvnw -pl customer-service -am spring-boot:run
   ```

   Portas: gateway 8080, customer 8081, product 8082, inventory 8083 e order 8084.

## Comandos úteis do Maven

| Comando | O que faz |
|---------|-----------|
| `./mvnw verify` | Tudo: Checkstyle, testes unitários, `*IT` com Testcontainers e cobertura |
| `./mvnw verify -DskipITs` | Igual, mas sem os `*IT` (sem precisar de Docker) |
| `./mvnw -pl order-service -am test` | Testa só um serviço (e o `common`) |
| `./mvnw -pl order-service test -Dtest=OrderServiceTest` | Executa uma classe de teste |
| `./mvnw checkstyle:check` | Só a verificação de estilo |

Relatório de cobertura: `<servico>/target/site/jacoco/index.html`.

## Problemas comuns

| Sintoma | Causa provável |
|---------|----------------|
| `Could not resolve placeholder 'JWT_SECRET'` | Variável de ambiente não definida |
| `must have at least 32 characters` | `JWT_SECRET` curto demais |
| `Validate failed: Migration checksum mismatch` | Uma migration já aplicada foi editada. Crie uma nova (`V2__...`) ou recrie o volume com `docker compose down -v` |
| `Could not find a valid Docker environment` | Os testes `*IT` precisam de Docker. Use `-DskipITs` |
| 401 em tudo | Token expirado (60 min) ou `JWT_SECRET` diferente entre o gateway e os serviços |
