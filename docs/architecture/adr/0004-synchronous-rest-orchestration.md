# ADR-0004: Orquestração REST síncrona com compensação

- **Status:** Aceita
- **Data:** 2026-11-04

## Contexto

Criar um pedido envolve quatro serviços. As opções são:

1. **Orquestração síncrona (REST)**: o Order Service chama os outros e decide.
2. **Coreografia assíncrona (eventos)**: cada serviço reage a eventos publicados em um broker (RabbitMQ/Kafka).

## Decisão

Começar com **orquestração REST síncrona** (requisito do projeto), já preparada para evoluir:

- O usuário recebe na hora a resposta definitiva: `201` (confirmado) ou `409` (sem estoque).
- A reserva de estoque é **idempotente** (chave = `orderNumber`) e existe **compensação** (release) se a
  gravação do pedido falhar.
- Os eventos de domínio já são publicados por uma porta (`OrderEventPublisher`). Trocar a implementação de log
  por RabbitMQ/Kafka não altera as regras de negócio.
- Os clientes HTTP (`RestClient`) têm timeouts e traduzem erros para exceções de domínio.

## Consequências

- (+) Simples de entender, depurar e testar.
- (+) Consistência imediata para o usuário.
- (-) **Acoplamento temporal**: se o Inventory estiver fora do ar, não é possível criar pedidos (503).
- (-) A latência soma as chamadas (uma por produto). Melhoria possível: um endpoint de busca em lote
  (`GET /api/products?ids=1,2,3`).
- Próximos passos documentados: Resilience4j (circuit breaker e retry) e mensageria para as mudanças de status.
