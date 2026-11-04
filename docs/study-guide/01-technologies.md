# 1. Tecnologias do projeto

| Tecnologia | Para que serve | Onde ver |
|------------|----------------|----------|
| **Java 21 (LTS)** | Linguagem. LTS = suporte de longo prazo, padrão em empresas. | Todo o código |
| **Maven + Wrapper** | Build, dependências e execução de testes. O `mvnw` baixa a versão certa do Maven. | `pom.xml`, `mvnw` |
| **Spring Boot 3.4** | Framework que configura a aplicação por convenção (servidor embutido, JSON, JPA...). | `*Application.java` |
| **Spring Web (MVC)** | Controllers REST sobre Servlet (uma thread por requisição). | `controller/` |
| **Spring WebFlux** | Pilha reativa e não bloqueante, usada pelo Gateway. | `api-gateway` |
| **Spring Cloud Gateway** | Proxy/roteador de borda com filtros. | `api-gateway/src/main/resources/application.yml` |
| **Spring Data JPA + Hibernate** | Mapeamento objeto-relacional e repositórios gerados automaticamente. | `entity/`, `repository/` |
| **Bean Validation** | Validação declarativa (`@NotBlank`, `@Email`, `@CPF`). | `dto/` |
| **Spring Security** | Autenticação (JWT) e autorização (roles). | `config/SecurityConfig.java` |
| **Flyway** | Versionamento do banco: scripts SQL numerados e aplicados em ordem. | `db/migration/` |
| **PostgreSQL** | Banco relacional de produção. | `docker-compose.yml` |
| **H2** | Banco em memória para testes rápidos. | `application-test.yml` |
| **springdoc-openapi** | Gera o OpenAPI/Swagger a partir dos controllers. | `config/OpenApiConfig.java` |
| **Actuator** | Health checks, métricas e info. | `/actuator/health` |
| **JUnit 5, AssertJ, Mockito** | Testes unitários. | `src/test` |
| **Testcontainers** | Sobe um PostgreSQL real em Docker durante os testes. | `*PostgresIT.java` |
| **MockWebServer** | Servidor HTTP falso para testar a comunicação entre serviços. | `OrderFlowIntegrationTest` |
| **Checkstyle / JaCoCo** | Qualidade de código e cobertura mínima. | `config/checkstyle`, `pom.xml` |
| **Docker / Compose** | Empacotamento e execução de toda a infraestrutura. | `infrastructure/docker`, `docker-compose.yml` |
| **GitHub Actions** | CI: build, testes e smoke test a cada push e Pull Request. | `.github/workflows/ci.yml` |

## Por que Spring MVC nos serviços e WebFlux no gateway?

- Os serviços fazem acesso a banco com JPA, que é **bloqueante**. O MVC é mais simples de ler e de depurar.
- O Spring Cloud Gateway é construído sobre WebFlux/Netty: ele passa a maior parte do tempo esperando a rede
  e atende milhares de conexões com poucas threads.

## Exercícios

1. Abra o `pom.xml` raiz e encontre onde as versões do Spring Cloud e do springdoc são definidas. Por que elas
   ficam no pai e não em cada serviço?
2. Rode `./mvnw -pl customer-service dependency:tree` e encontre quem traz o Hibernate para o projeto.
3. Acesse `http://localhost:8081/actuator/health` com o projeto rodando. Pare o PostgreSQL
   (`docker compose stop postgres`) e acesse de novo. O que mudou?
