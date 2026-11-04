# 9. Exercícios e desafios para evoluir o projeto

Faça cada item em uma branch `feature/*`, com testes, e abra um Pull Request para a `develop`. O CI vai validar.

## Nível 1: fundamentos

1. **Data de nascimento do cliente**: migration `V2`, campo, validação `@Past` e testes.
2. **Busca de cliente por e-mail**: `GET /api/customers/by-email?email=`.
3. **Ordenação de produtos por preço**: valide os campos aceitos em `sort` para evitar erro 500 com campo
   inexistente.
4. **Mensagens de validação em português**: crie `ValidationMessages.properties`.

## Nível 2: regras de negócio

5. **Estoque mínimo**: `InventoryItem` ganha `minimumQuantity`, e `GET /api/inventory/low-stock` lista os itens
   abaixo do mínimo.
6. **Motivo do cancelamento**: `PATCH /api/orders/{id}/status` aceita `reason` quando o status é `CANCELLED`.
7. **Status `RETURNED`**: a partir de `DELIVERED`, devolvendo o estoque. Quais chamadas ao Inventory seriam
   necessárias?
8. **Histórico de status**: tabela `order_status_history` registrando cada transição e quando ela ocorreu.

## Nível 3: microsserviços

9. **Busca de produtos em lote**: `GET /api/products?ids=1,2,3` e uso no `OrderService` (uma chamada em vez de N).
10. **Resilience4j**: circuit breaker e retry (só em operações idempotentes!) nos clientes do Order Service.
    Teste com o MockWebServer devolvendo 503.
11. **RabbitMQ**: implemente `RabbitOrderEventPublisher`, adicione o RabbitMQ ao compose e crie um
    `notification-service` que consome `ORDER_CREATED` e registra um "e-mail enviado".
12. **Outbox pattern**: grave o evento em uma tabela `outbox` na mesma transação do pedido e publique de forma
    assíncrona. Por que isso é mais confiável que publicar direto?
13. **Job de reconciliação**: encontre reservas `RESERVED` sem pedido correspondente há mais de 15 minutos e
    libere-as (cobre a falha da compensação).

## Nível 4: segurança

14. **Vincular o usuário ao cliente**: o token carrega `customerId`, e um `USER` só cria e consulta os próprios
    pedidos (403 nos demais). Escreva os testes primeiro.
15. **Token de serviço**: o Order Service obtém um token próprio (`ROLE_SERVICE`) para chamar o Inventory, e as
    reservas passam a exigir `hasRole("SERVICE")`.
16. **Keycloak**: substitua o login do gateway por Keycloak (RS256 + JWKS). Os serviços passam a usar
    `spring.security.oauth2.resourceserver.jwt.issuer-uri`, e o `JWT_SECRET` deixa de existir.
17. **Rate limiting no gateway** com `RequestRateLimiter` + Redis.

## Nível 5: operação

18. **Tracing distribuído**: Micrometer Tracing + Zipkin e compare com o Correlation ID manual.
19. **Métricas de negócio**: um contador `orders.created` com Micrometer e visualização no Prometheus + Grafana.
20. **Publicar imagens no GHCR** pelo GitHub Actions quando uma tag `v*` for criada.
21. **Swagger agregado no gateway**: uma única página com a documentação de todos os serviços.

## Como saber se ficou bom

- `./mvnw verify` verde (e o CI verde).
- Cobertura acima de 70% por módulo.
- Testes para o sucesso **e** para os erros.
- ADR nova em `docs/architecture/adr/` quando a decisão for arquitetural.
- README e `docs/api` atualizados.
