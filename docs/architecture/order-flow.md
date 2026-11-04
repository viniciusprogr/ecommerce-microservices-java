# Fluxo de criação e ciclo de vida de pedidos

## Criação de pedido (caminho feliz)

```mermaid
sequenceDiagram
    autonumber
    actor U as Usuário
    participant G as API Gateway
    participant O as Order Service
    participant C as Customer Service
    participant P as Product Service
    participant I as Inventory Service
    participant DB as order_db

    U->>G: POST /api/orders {customerId, items[]}
    G->>O: encaminha (JWT validado)
    O->>C: GET /api/customers/{customerId}
    C-->>O: 200 cliente ativo
    loop para cada item
        O->>P: GET /api/products/{productId}
        P-->>O: 200 produto ativo + preço
    end
    O->>O: gera orderNumber (UUID), calcula subtotais e total
    O->>I: POST /api/inventory/reservations {reference: orderNumber, items}
    I->>I: bloqueia linhas (SELECT ... FOR UPDATE), valida disponível e reserva
    I-->>O: 201 reserva RESERVED
    O->>DB: INSERT order (CONFIRMED) + items
    O->>O: publica OrderEvent(CREATED)
    O-->>G: 201 Created + Location
    G-->>U: 201 pedido
```

## Cenários de erro

| Situação | Onde é detectada | Resposta | Efeito colateral |
|----------|------------------|----------|------------------|
| Corpo inválido (sem itens, quantidade <= 0) | Bean Validation | 400 | nenhum |
| Produto repetido na lista | `OrderService` | 422 | nenhum |
| Cliente inexistente ou inativo | `CustomerClient` / `OrderService` | 422 | nenhum |
| Produto inexistente ou inativo | `ProductClient` / `OrderService` | 422 | nenhum |
| Estoque insuficiente | Inventory (409) | 409 | nenhum (a reserva é tudo ou nada) |
| Serviço remoto fora do ar / timeout | clientes HTTP | 503 | nenhum |
| Falha ao gravar o pedido **após** reservar | `OrderService` | 500 | **compensação**: libera a reserva |

### Por que não usar uma única transação?

Cada serviço tem seu próprio banco, então não existe transação ACID que abranja Order e Inventory.
Usamos o padrão **Saga (orquestrada)** em sua forma mais simples: o Order Service coordena os passos e, se um
passo posterior falhar, executa a **ação compensatória** do passo anterior (liberar a reserva).

Duas decisões tornam isso seguro:

1. **Idempotência da reserva**: a reserva é identificada pelo `orderNumber`. Se o Order Service repetir a
   chamada (retry), o Inventory devolve a reserva existente em vez de reservar de novo.
2. **Chamadas remotas fora de `@Transactional`**: o `OrderService` não mantém uma transação de banco aberta
   enquanto espera respostas HTTP, evitando segurar conexões do pool por tempo indeterminado.

## Máquina de estados do pedido

```mermaid
stateDiagram-v2
    [*] --> CONFIRMED: criado com estoque reservado
    CONFIRMED --> SHIPPED: baixa definitiva do estoque (commit)
    CONFIRMED --> CANCELLED: libera reserva (release)
    SHIPPED --> DELIVERED
    DELIVERED --> [*]
    CANCELLED --> [*]
```

As transições permitidas ficam no próprio enum `OrderStatus` (`canTransitionTo`). Assim a regra mora no
domínio e é testada de forma unitária, sem depender de banco ou HTTP. Uma transição inválida
(ex.: `DELIVERED → CANCELLED`) retorna `409 Conflict`.

Efeitos colaterais de cada transição:

| Transição | Chamada ao Inventory | Efeito no estoque |
|-----------|----------------------|-------------------|
| `CONFIRMED → SHIPPED` | `POST /reservations/{ref}/commit` | `onHand -= q`, `reserved -= q` |
| `CONFIRMED → CANCELLED` | `POST /reservations/{ref}/release` | `reserved -= q` |
| `SHIPPED → DELIVERED` | nenhuma | nenhum |

## Estoque: físico, reservado e disponível

```
disponível = quantidadeEmEstoque (onHand) - quantidadeReservada (reserved)
```

- **Reservar** não tira o produto da prateleira: só impede que outro pedido o use.
- **Commit (baixa)** acontece no envio: o item sai fisicamente.
- **Release** devolve a quantidade reservada para o disponível.
