# Backlog de implementação

Planejamento por etapas definido no início do projeto e seu status atual.

| Etapa | Itens | Branch | Status |
|-------|-------|--------|--------|
| 1. Planejamento | Requisitos, arquitetura, diagramas, ADRs, backlog | `feature/project-structure` | Concluída |
| 2. Estrutura inicial | Git, Maven multimódulo, Maven Wrapper, Checkstyle, JaCoCo, módulo `common` | `feature/project-structure` | Concluída |
| 3. Serviços | Customer (CRUD, CPF, exclusão lógica) | `feature/customer-service` | Concluída |
| | Product (categorias, produtos, filtros com Specification) | `feature/product-service` | Concluída |
| | Inventory (estoque, reserva idempotente, commit, release, lock pessimista) | `feature/inventory-service` | Concluída |
| | Order (orquestração, compensação, máquina de estados, eventos) | `feature/order-service` | Concluída |
| 4. Integração | Clientes REST, propagação de JWT e Correlation ID, API Gateway com login JWT | `feature/order-service`, `feature/api-gateway` | Concluída |
| 5. Testes | Unitários, repositório (H2), API (MockMvc), comunicação (MockWebServer), PostgreSQL (Testcontainers) | em cada feature | Concluída |
| 6. Infraestrutura | Dockerfile parametrizado, compose, health checks, init do banco, smoke test | `feature/docker` | Concluída |
| 7. Documentação | README, ADRs, banco, API, guia de estudos | `feature/documentation` | Concluída |
| 8. CI/CD | GitHub Actions: Checkstyle, build, testes, cobertura, smoke test com Docker | `feature/ci-cd` | Concluída |
| 9. Publicação | Merge em `main`, tag `v1.0.0`, push | `main` | Concluída |

## Próximas evoluções (v1.x)

Detalhadas como exercícios em [`docs/study-guide/09-exercises-and-challenges.md`](../study-guide/09-exercises-and-challenges.md):

- Resilience4j (circuit breaker e retry) nos clientes do Order Service.
- RabbitMQ para eventos de pedido (`OrderEventPublisher`).
- Vincular o usuário do login ao cliente (um USER só vê os próprios pedidos).
- Keycloak com RS256/JWKS no lugar do HS256.
- Busca de produtos em lote para reduzir chamadas na criação de pedidos.
- Tracing distribuído com Micrometer Tracing + Zipkin.
