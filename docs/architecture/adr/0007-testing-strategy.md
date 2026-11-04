# ADR-0007: Estratégia de testes (pirâmide)

- **Status:** Aceita
- **Data:** 2026-11-04

## Decisão

```
            /\        Smoke test E2E (docker compose + curl)            -> CI
           /  \       *IT: PostgreSQL real via Testcontainers           -> CI (exige Docker)
          /    \      *IntegrationTest: @SpringBootTest + MockMvc + H2  -> todo build
         /      \     Repository: @DataJpaTest + H2                     -> todo build
        /        \    Unitários: domínio puro e services com Mockito    -> todo build
```

| Tipo | Ferramentas | Exemplo |
|------|-------------|---------|
| Domínio | JUnit 5, AssertJ | `InventoryItemTest`, `OrderTest` (máquina de estados) |
| Service | Mockito | `OrderServiceTest` (compensação quando a gravação falha) |
| Validação | Bean Validation | `CustomerRequestValidationTest` (CPF inválido) |
| Cliente HTTP | `MockRestServiceServer` | `RemoteClientsTest` (404 → 422, 409 → 409, timeout → 503) |
| Repository | `@DataJpaTest` + H2 | `ProductRepositoryTest` (Specifications) |
| API | `@SpringBootTest` + MockMvc + `jwt()` | `CustomerControllerIntegrationTest` |
| Comunicação entre serviços | MockWebServer (HTTP real) | `OrderFlowIntegrationTest` (propaga token e Correlation ID) |
| Gateway | WebTestClient + MockWebServer | `GatewayIntegrationTest` |
| Banco real | Testcontainers | `InventoryPostgresIT` (20 reservas concorrentes, nunca vende a mais) |
| Ponta a ponta | Docker Compose + curl | `infrastructure/scripts/smoke-test.sh` |

Regras:

- Classes `*Test` rodam no `surefire` (fase `test`); classes `*IT` rodam no `failsafe` (fase `integration-test`).
- O JaCoCo exige **70% de cobertura de linhas** por módulo, senão o build falha.
- O H2 nunca substitui o PostgreSQL: todo serviço tem pelo menos um `*IT` com o banco real.
- Nenhum teste é desabilitado para fazer o build passar.

## Consequências

- (+) Feedback rápido localmente (sem Docker) e validação completa no CI.
- (-) Os `*IT` não rodam em máquinas sem Docker. Use `./mvnw verify -DskipITs` e confie no CI.
