# 6. Mockito

O Mockito cria **dublês** de dependências para testar uma classe isoladamente. No `OrderServiceTest`, os três
clientes HTTP, o repositório e o publicador de eventos são mocks: simulamos qualquer resposta (ou falha) dos
outros serviços sem rede.

## Criando mocks

```java
@ExtendWith(MockitoExtension.class)
class OrderServiceTest {
    @Mock private CustomerClient customerClient;
    @Mock private OrderRepository repository;

    @BeforeEach
    void setUp() {
        service = new OrderService(repository, customerClient, ...);   // injeção manual pelo construtor
    }
}
```

## Stubbing: definindo respostas

```java
when(customerClient.getCustomer(1L)).thenReturn(new RemoteCustomer(1L, "Maria", "Silva", "m@e.com", true));
when(inventoryClient.reserve(any())).thenThrow(new InsufficientStockException("Insufficient stock"));
when(repository.save(any(Order.class))).thenAnswer(inv -> {
    Order order = inv.getArgument(0);        // devolve o próprio objeto recebido,
    ReflectionTestUtils.setField(order, "id", 99L);   // simulando o id gerado pelo banco
    return order;
});
```

## Verificação: o que foi (ou não) chamado

```java
verify(inventoryClient).release(anyString());              // a compensação aconteceu
verify(repository, never()).save(any());                   // nada foi gravado
verifyNoInteractions(inventoryClient, repository);         // nem chegou a reservar
```

## ArgumentCaptor: inspecionando o que foi enviado

```java
ArgumentCaptor<ReservationRequest> captor = ArgumentCaptor.forClass(ReservationRequest.class);
verify(inventoryClient).reserve(captor.capture());
assertThat(captor.getValue().reference()).isEqualTo(response.orderNumber());
```

## Strict stubs e `lenient()`

Com `MockitoExtension`, um stub que nunca é usado faz o teste **falhar** (`UnnecessaryStubbingException`).
Isso mantém os testes limpos. Use `lenient()` só para stubs genéricos de propósito (veja o
`TransactionTemplate` no `setUp` do `OrderServiceTest`).

## Quando NÃO usar mock

- Objetos de valor e entidades: use os reais (`new InventoryItem(1L, 10)`).
- Mappers simples: os testes usam `new CustomerMapper()`.
- Para testar a integração real (SQL, HTTP, JSON), use os testes de integração do capítulo 7.

> Se você precisa de muitos mocks para testar uma classe, talvez ela tenha responsabilidades demais.

## Exercícios

1. No `OrderServiceTest`, verifique com `ArgumentCaptor` que o `OrderEvent` publicado na mudança de status tem
   o tipo `ORDER_STATUS_CHANGED`.
2. Teste que `CustomerService.update` **não** chama `saveAndFlush` quando o e-mail já pertence a outro cliente.
3. Use `@InjectMocks` em vez da construção manual. Quais são as vantagens e as desvantagens?
