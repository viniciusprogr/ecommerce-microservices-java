# 7. Testes de integração

Os testes de integração verificam se as peças funcionam **juntas**: Spring, JSON, segurança, SQL e HTTP.

## Repositório com `@DataJpaTest` (H2)

```java
@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class ProductRepositoryTest { ... }
```

Sobe só a camada JPA, aplica as migrations Flyway no H2 e faz rollback ao final de cada teste.

**Lição real deste projeto:** os contextos de teste compartilham o mesmo H2 em memória. O teste precisa limpar
os dados antes, e com `deleteAllInBatch()`, porque o `deleteAll()` só executa o DELETE no flush, e o Hibernate
faz os INSERTs **antes** dos DELETEs no flush.

## API com `@SpringBootTest` + MockMvc

```java
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CustomerControllerIntegrationTest {

    mockMvc.perform(post("/api/customers")
                    .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_USER")))
                    .contentType(MediaType.APPLICATION_JSON).content(BODY))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.document").value("52998224725"));
}
```

- Contexto completo, com segurança, validação e banco H2 reais.
- `jwt()` (spring-security-test) cria um usuário autenticado sem precisar assinar um token.
- Teste também os **erros**: 400 (validação), 401 (sem token), 403 (role errada), 404 e 409.

## Clientes HTTP com `MockRestServiceServer`

`RemoteClientsTest` intercepta o `RestClient` e verifica a URL, o método e o corpo enviados. Depois simula
respostas 200, 404, 409, 500 e falhas de conexão.

## Comunicação entre serviços com MockWebServer

`OrderFlowIntegrationTest` sobe o Order Service inteiro e aponta Customer, Product e Inventory para um
servidor HTTP **real** em uma porta local. Ele prova que:

- as três chamadas acontecem na ordem certa;
- o `Authorization: Bearer ...` e o `X-Correlation-Id` são propagados;
- 409 do Inventory vira 409 para o usuário, e nada é gravado.

## PostgreSQL real com Testcontainers

```java
@SpringBootTest
@Testcontainers
class InventoryPostgresIT {
    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");
}
```

- `@ServiceConnection` injeta a URL, o usuário e a senha do container no Spring.
- Valida as migrations e o `ddl-auto: validate` no banco de produção.
- `InventoryPostgresIT` dispara 20 reservas concorrentes para um estoque de 5, e **exatamente 5** são aceitas.
  Esse comportamento de lock não pode ser confiado ao H2.

Classes `*IT` rodam no `maven-failsafe-plugin` e exigem Docker:

```bash
./mvnw verify            # com Docker: tudo
./mvnw verify -DskipITs  # sem Docker
```

## Exercícios

1. Escreva `CategoryRepositoryTest` com `@DataJpaTest` cobrindo `existsByNameIgnoreCaseAndIdNot`.
2. Adicione ao `OrderFlowIntegrationTest` o cenário "produto inativo" (o MockWebServer devolve `active:false`)
   e verifique o 422.
3. Crie um `CustomerApiPostgresIT` que use MockMvc contra o PostgreSQL do Testcontainers.
