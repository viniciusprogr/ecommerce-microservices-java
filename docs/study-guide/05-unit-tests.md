# 5. Testes unitários

Um teste unitário verifica **uma unidade de comportamento** isolada: sem banco, sem rede e sem Spring.
Ele roda em milissegundos.

## Estrutura AAA (Arrange, Act, Assert)

```java
@Test
void shouldCommitReservedQuantity() {
    InventoryItem item = new InventoryItem(1L, 10);   // Arrange
    item.reserve(4);

    item.commit(4);                                   // Act

    assertThat(item.getQuantityOnHand()).isEqualTo(6);   // Assert
    assertThat(item.getQuantityReserved()).isZero();
}
```

## Nomes que descrevem o comportamento

`shouldNotReserveMoreThanAvailable`, `shouldReleaseReservation_whenSavingFails`. Quando o teste falha, o
nome já explica o que quebrou.

## JUnit 5 em uso

| Recurso | Para quê | Exemplo |
|---------|----------|---------|
| `@Test` | Caso de teste | todos |
| `@Nested` | Agrupar cenários | `CustomerServiceTest.Create`, `.Update` |
| `@BeforeEach` | Preparação comum | criação do service |
| `@ParameterizedTest` + `@ValueSource`/`@CsvSource` | Mesmo teste, várias entradas | `OrderTest.shouldFollowStateMachine`, CPFs inválidos |
| `@ExtendWith(MockitoExtension.class)` | Habilitar o Mockito | services |

## AssertJ

```java
assertThat(response.items()).extracting(OrderItemResponse::productName).containsExactly("Mouse", "Teclado");
assertThatThrownBy(() -> service.findById(99L))
        .isInstanceOf(CustomerNotFoundException.class)
        .hasMessage("Customer 99 not found");
assertThat(total).isEqualByComparingTo("349.80");   // BigDecimal
```

## O que testar

- Caminho feliz.
- **Cada regra de negócio** e cada exceção (duplicidade, estoque insuficiente, transição inválida).
- Bordas: quantidade 0 e negativa, lista vazia, valores repetidos.
- Efeitos colaterais que **não** devem acontecer (`verify(repository, never()).save(any())`).

## Testes de validação

`CustomerRequestValidationTest` usa o `Validator` do Bean Validation diretamente, sem Spring, para provar que
CPF inválido, e-mail inválido e campos em branco são rejeitados.

## Cobertura

O JaCoCo mede quais linhas foram executadas. O build exige 70% por módulo. Abra
`<servico>/target/site/jacoco/index.html` depois de `./mvnw verify -DskipITs`. Cobertura alta não garante bons
testes: um teste sem asserções "cobre" código sem verificar nada.

## Exercícios

1. Escreva testes para `CustomerMapper` cobrindo a normalização de e-mail e CPF.
2. Adicione em `InventoryItemTest` um teste parametrizado para `adjustOnHand` com vários valores.
3. Quebre uma regra de propósito (troque `>=` por `>` em `InventoryItem.canReserve`) e veja quais testes
   falham. Essa técnica se chama *mutation testing* (ferramenta: PIT).
