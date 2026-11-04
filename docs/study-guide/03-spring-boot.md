# 3. Spring Boot

## Inversão de controle e injeção de dependência

Você não instancia `CustomerService` com `new`: o Spring cria os objetos (*beans*) e entrega as dependências
pelo construtor.

```java
@Service
public class CustomerService {
    private final CustomerRepository repository;
    private final CustomerMapper mapper;

    public CustomerService(CustomerRepository repository, CustomerMapper mapper) { ... }
}
```

Vantagens: dependências explícitas, campos `final` e testes simples (basta passar mocks no construtor).

## Auto-configuração

Ao adicionar `spring-boot-starter-data-jpa`, o Boot detecta o driver e configura `DataSource`,
`EntityManagerFactory` e transações. O projeto cria a própria auto-configuração no módulo `common`
(`CommonWebAutoConfiguration`, `JwtSecurityAutoConfiguration`), registrada em
`META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`. Com
`@ConditionalOnMissingBean`, um serviço pode substituir qualquer bean, se precisar.

## Camadas

```
HTTP → Controller (DTO + @Valid) → Service (@Transactional, regras) → Repository (JPA) → Banco
                     ↑                     ↓
               GlobalExceptionHandler ← exceções de domínio
```

## Spring Data JPA

- **Query derivation**: `existsByEmailAndIdNot(email, id)` vira SQL automaticamente.
- **`@EntityGraph`**: carrega associações na mesma consulta (evita o problema N+1).
- **`Specification`**: filtros dinâmicos combináveis (`ProductSpecifications`).
- **`@Lock(PESSIMISTIC_WRITE)`**: `SELECT ... FOR UPDATE` (`InventoryItemRepository`).
- **`@Version`**: lock otimista. Atualizações concorrentes resultam em `409`.

## Transações

- `@Transactional` no service: tudo ou nada.
- `readOnly = true` em consultas: otimiza o Hibernate (sem *dirty checking*).
- **Dirty checking**: dentro da transação, alterar uma entidade gerenciada basta para o Hibernate gerar o
  `UPDATE` no commit (veja `CustomerService.deactivate`, que não chama `save`).
- `open-in-view: false`: a sessão do Hibernate não fica aberta até a view. Associações *lazy* precisam ser
  acessadas dentro do service, o que é uma boa prática.
- No `OrderService`, a criação **não** é `@Transactional`, porque uma transação aberta durante chamadas HTTP
  prende conexões. As gravações usam transações curtas (`TransactionTemplate`).

## Validação

```java
public record CustomerRequest(@NotBlank @Email String email, @NotBlank @CPF String document, ...) { }

@PostMapping
public ResponseEntity<CustomerResponse> create(@Valid @RequestBody CustomerRequest request) { ... }
```

Se falhar, o `GlobalExceptionHandler` devolve `400` com a lista `violations`.

## Tratamento centralizado de erros

`@RestControllerAdvice` + `@ExceptionHandler` mapeiam exceções para status HTTP. Os services lançam exceções
com significado (`DuplicateCustomerException extends ConflictException`) e nunca lidam com HTTP.

## Configuração externa

`application.yml` usa `${VARIAVEL:valor-padrao}`. Segredos não têm valor padrão (`${JWT_SECRET}`), então a
aplicação **não sobe** sem eles (fail fast). `@ConfigurationProperties` + `@Validated` validam a configuração
na inicialização (`JwtProperties`, `ServicesProperties`).

## Spring Security com JWT

1. O gateway autentica usuário e senha (BCrypt) e assina um JWT (`TokenService`).
2. Cada requisição traz `Authorization: Bearer <token>`.
3. O `oauth2ResourceServer().jwt()` valida a assinatura, a expiração e o emissor.
4. O claim `roles` vira `ROLE_ADMIN`/`ROLE_USER` (`JwtGrantedAuthoritiesConverter`).
5. As regras ficam em `SecurityConfig`: `.requestMatchers(HttpMethod.DELETE, "/api/customers/**").hasRole("ADMIN")`.

## Observabilidade

- **Actuator**: `/actuator/health/readiness` é usado pelos health checks do Docker.
- **Logs estruturados**: `LOGGING_STRUCTURED_FORMAT_CONSOLE=ecs` gera JSON (recurso nativo do Boot 3.4).
- **Correlation ID**: gerado no gateway, colocado no MDC e propagado pelos interceptors. Ele aparece no log
  de todos os serviços.

## Exercícios

1. Adicione `GET /api/customers/by-email?email=` usando *query derivation*.
2. Crie a migration `V2__add_birth_date.sql`, o campo na entidade e no DTO, e uma validação `@Past`.
3. Faça `@WebMvcTest(CustomerController.class)` com o service mockado e compare a velocidade com o teste
   `@SpringBootTest`.
4. Mude `ddl-auto` para `update`, renomeie uma coluna na entidade e observe o que acontece. Depois volte para
   `validate` e explique por que ele é mais seguro.
