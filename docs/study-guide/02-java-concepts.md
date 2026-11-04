# 2. Conceitos de Java aplicados

## Records (Java 16+)

Classes imutáveis e concisas para carregar dados. O compilador gera o construtor, os acessores, `equals`,
`hashCode` e `toString`.

```java
public record CustomerResponse(Long id, String firstName, String email, boolean active) { }
response.email();   // acessor, sem o prefixo "get"
```

Onde: todos os DTOs (`dto/`), `ApiError`, `OrderEvent`, `GatewaySecurityProperties` (configuração imutável).

> Entidades JPA **não** são records: o Hibernate precisa de um construtor sem argumentos e de campos mutáveis.

## Enums com comportamento e `switch` como expressão

```java
public Set<OrderStatus> allowedTransitions() {
    return switch (this) {
        case CONFIRMED -> EnumSet.of(SHIPPED, CANCELLED);
        case SHIPPED -> EnumSet.of(DELIVERED);
        case DELIVERED, CANCELLED -> EnumSet.noneOf(OrderStatus.class);
    };
}
```

O `switch` com `->` devolve um valor, e o compilador exige que **todos os casos** sejam tratados. Se você criar
um novo status e esquecer de tratá-lo, o código não compila. Veja `OrderStatus.java`.

## Encapsulamento e invariantes

`InventoryItem` não tem `setQuantityReserved`. A única forma de mudar o estoque é por métodos com significado
(`reserve`, `release`, `commit`, `adjustOnHand`), e cada um valida as regras. É impossível deixar o objeto
inválido "por acidente".

## `BigDecimal` para dinheiro

```java
new BigDecimal("0.1").add(new BigDecimal("0.2"));   // 0.3
0.1 + 0.2;                                          // 0.30000000000000004
```

- Crie a partir de `String`, nunca de `double`.
- Compare com `compareTo` (ou `isEqualByComparingTo` nos testes): `new BigDecimal("2.0").equals(new BigDecimal("2.00"))` é `false`.
- Defina a escala: `unitPrice.setScale(2, RoundingMode.HALF_EVEN)` em `OrderItem`.

## `Optional`

```java
return repository.findById(id).orElseThrow(() -> new CustomerNotFoundException(id));
```

Torna explícito que o resultado pode não existir e evita `NullPointerException`.

## Streams e Collectors

```java
BigDecimal total = items.stream().map(OrderItem::getSubtotal).reduce(BigDecimal.ZERO, BigDecimal::add);

Map<Long, InventoryItem> byProduct = items.stream()
        .collect(Collectors.toMap(InventoryItem::getProductId, Function.identity()));
```

Onde: `Order` (total), `ReservationService` (agrupamento e locks), mappers (`toList()`).

## Pattern matching com `instanceof`

```java
if (authentication instanceof JwtAuthenticationToken jwtToken) {
    jwtToken.getToken().getTokenValue();   // já convertido, sem cast manual
}
```

Veja `BearerTokenPropagationInterceptor` e os métodos `equals` das entidades.

## Text blocks

```java
String body = """
        {"customerId":1,"items":[{"productId":10,"quantity":3}]}
        """;
```

Muito usados nos testes para escrever JSON legível.

## Concorrência

`InventoryPostgresIT` usa `ExecutorService` + `invokeAll` para disparar 20 reservas simultâneas e provar que o
lock pessimista impede vender a mesma unidade duas vezes.

## `equals`/`hashCode` em entidades

As entidades comparam pelo `id`, e o `hashCode` é constante por classe. Isso mantém o comportamento correto
em `Set`s mesmo antes de o `id` ser gerado pelo banco. É uma recomendação clássica para entidades JPA.

## Exercícios

1. Crie um record `Money(BigDecimal amount, String currency)` com validação no *compact constructor*
   (`amount` não negativo) e use-o em `OrderItem`.
2. Adicione o status `RETURNED` (permitido a partir de `DELIVERED`). Veja o compilador apontar o `switch` e
   atualize `OrderTest`.
3. Reescreva `ensureNoDuplicatedProducts` em `OrderService` usando `Collectors.groupingBy` + `counting()`.
