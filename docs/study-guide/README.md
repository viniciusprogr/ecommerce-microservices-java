# Guia de estudos

Este guia usa o próprio código do projeto como material didático. Cada capítulo explica um assunto, aponta
**onde ele aparece no código** e termina com exercícios práticos.

| # | Capítulo | Você vai aprender |
|---|----------|-------------------|
| 1 | [Tecnologias do projeto](01-technologies.md) | O papel de cada ferramenta e por que ela foi escolhida |
| 2 | [Conceitos de Java aplicados](02-java-concepts.md) | records, enums com comportamento, Optional, Streams, BigDecimal, sealed switch |
| 3 | [Spring Boot](03-spring-boot.md) | IoC/DI, auto-configuração, camadas, JPA, transações, validação, segurança |
| 4 | [Arquitetura de microsserviços](04-microservices.md) | Bounded contexts, banco por serviço, gateway, saga, idempotência |
| 5 | [Testes unitários](05-unit-tests.md) | JUnit 5, AssertJ, testes parametrizados, padrão AAA |
| 6 | [Mockito](06-mockito.md) | Mocks, stubs, verificações, ArgumentCaptor |
| 7 | [Testes de integração](07-integration-tests.md) | @SpringBootTest, MockMvc, @DataJpaTest, Testcontainers, MockWebServer |
| 8 | [Docker](08-docker.md) | Imagens, multi-stage build, compose, redes, health checks |
| 9 | [Exercícios e desafios](09-exercises-and-challenges.md) | Evoluções do projeto, do nível iniciante ao avançado |

## Roteiro sugerido

1. Suba o projeto com `docker compose up --build` e rode o fluxo do README com curl.
2. Leia o `customer-service`, o serviço mais simples, camada por camada: controller → service → repository → entity.
3. Rode os testes de um serviço e abra o relatório do JaCoCo.
4. Leia o `inventory-service` (regras no domínio, locks) e depois o `order-service` (orquestração).
5. Leia o `api-gateway` (programação reativa e emissão de JWT).
6. Faça os exercícios do capítulo 9, um por branch, com testes e Pull Request.
