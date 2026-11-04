# ADR-0006: Arquitetura em camadas, sem Lombok e sem MapStruct

- **Status:** Aceita
- **Data:** 2026-11-04

## Decisão

Cada serviço usa as camadas `controller → service → repository`, com os pacotes `entity`, `dto`, `mapper`,
`exception` e `config`.

- **Controllers finos**: recebem DTOs validados (`@Valid`) e delegam. Não há regra de negócio neles.
- **Services retornam DTOs** e definem os limites de transação (`@Transactional`). Como
  `spring.jpa.open-in-view=false`, todo acesso a associações *lazy* acontece dentro do service.
- **Entidades com comportamento** (ex.: `InventoryItem.reserve(qty)`, `Order.changeStatus()`): as invariantes
  ficam junto dos dados (modelo de domínio rico, DDD), em vez de espalhadas pelos services.
- **DTOs como `record`**: imutáveis, concisos e nativos do Java 16+.
- **Mappers escritos à mão**: explícitos e fáceis de depurar. Para um projeto deste tamanho, o MapStruct não
  compensa o processador de anotações extra.
- **Sem Lombok**: o objetivo é estudar Java, e `record` já resolve a maior parte do boilerplate.
- **Sem a dupla "Service/ServiceImpl"** quando só existe uma implementação: o Mockito consegue mockar classes
  concretas. Interfaces só aparecem quando há variação real (ex.: `OrderEventPublisher`).
- **Builder Pattern** só onde há muitos atributos (`Product.builder()`).

## Consequências

- (+) Código previsível: todos os serviços têm a mesma estrutura.
- (+) Fácil de testar: services com Mockito e controllers com MockMvc.
- (-) Mais linhas de código do que com Lombok + MapStruct.
