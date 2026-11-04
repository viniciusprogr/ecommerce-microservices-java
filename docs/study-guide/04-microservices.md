# 4. Arquitetura de microsserviços

## O que é

Um sistema dividido em serviços pequenos, cada um **dono de uma capacidade de negócio e dos seus dados**,
que se comunicam pela rede e são implantados de forma independente.

| Vantagens | Custos |
|-----------|--------|
| Deploy e escala independentes | Rede: latência, falhas parciais, timeouts |
| Times autônomos por contexto | Consistência eventual, sem transação ACID global |
| Falha isolada (com resiliência) | Observabilidade distribuída (logs, tracing) |
| Tecnologia por serviço | Mais infraestrutura (CI, containers, gateway) |

> Para sistemas pequenos, um **monólito modular** costuma ser a melhor escolha. Aqui usamos microsserviços
> porque o objetivo é aprender esses desafios na prática.

## Bounded Contexts (DDD)

Cada serviço tem o seu modelo. O "produto" do catálogo (preço vigente, categoria) não é o mesmo que o
"item de pedido" (preço **pago**, nome no momento da compra) nem o "item de estoque" (quantidades).

## Banco por serviço

Nenhum serviço lê a tabela de outro. O Order Service sabe o `customerId`, mas para saber se o cliente está
ativo **pergunta ao Customer Service** (`CustomerClient`).

## API Gateway

Ponto único de entrada: roteamento, autenticação, Correlation ID e bloqueio de rotas internas. Os clientes
conhecem um único endereço.

## Comunicação síncrona com `RestClient`

```java
restClient.get().uri("/api/customers/{id}", id)
        .retrieve()
        .onStatus(s -> s.value() == 404, (req, res) -> { throw new InvalidOrderException(...); })
        .body(RemoteCustomer.class);
```

- **Tolerant Reader**: `RemoteCustomer` declara só os campos usados, e campos novos no JSON são ignorados.
- **Timeouts** (`ServicesProperties`): sem eles, um serviço lento derruba quem o chama.
- **Tradução de erros**: 404 → 422, 409 → 409, timeout/5xx → 503.

## Saga orquestrada e compensação

```
reservar estoque ──► gravar pedido ──► publicar evento
        ▲                  │ falhou
        └──── liberar ◄────┘   (ação compensatória)
```

Sem transação distribuída, cada passo tem um "desfazer". Veja `OrderService.create` e
`compensateReservation`.

## Idempotência

Na rede, uma chamada pode ser executada sem que a resposta chegue, e o cliente tenta de novo. A reserva usa o
`orderNumber` como chave: repetir a chamada devolve a mesma reserva (`ReservationService.reserve`).
`commit` e `release` também são idempotentes.

## Concorrência no estoque

Dois pedidos simultâneos para a última unidade: o `SELECT ... FOR UPDATE` faz o segundo esperar o primeiro
terminar e então ver o estoque atualizado. Os locks são obtidos **sempre na mesma ordem** (`order by
productId`) para evitar deadlock.

## Preparado para eventos

`OrderEventPublisher` é uma porta: hoje grava log, amanhã publica no RabbitMQ. Leia
[communication.md](../architecture/communication.md).

## Exercícios

1. Desligue o Inventory (`docker compose stop inventory-service`) e tente criar um pedido. Qual status volta?
   Em quanto tempo? Onde está esse timeout?
2. Envie duas vezes a mesma reserva (mesma `reference`) direto no Inventory (porta 8083) e confira que o
   estoque só foi reservado uma vez.
3. Desenhe o fluxo que mudaria se o envio do pedido (`SHIPPED`) fosse assíncrono via RabbitMQ.
