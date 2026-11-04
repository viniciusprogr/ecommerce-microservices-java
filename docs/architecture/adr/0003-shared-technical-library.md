# ADR-0003: Biblioteca técnica compartilhada (`common`)

- **Status:** Aceita
- **Data:** 2026-11-04

## Contexto

Os quatro serviços Spring MVC precisam de código técnico idêntico: formato de erro (`ApiError`), tratamento
global de exceções, Correlation ID, validação de JWT e interceptors HTTP. Copiar esse código quatro vezes viola
o DRY e gera divergências.

Por outro lado, bibliotecas compartilhadas são uma fonte conhecida de acoplamento em microsserviços
("monólito distribuído"), principalmente quando carregam o **modelo de domínio**.

## Decisão

Criar o módulo `common` com **apenas código técnico e transversal**:

- `exception`: `ApiError`, exceções base (`ResourceNotFoundException`, `ConflictException`,
  `BusinessRuleException`, `RemoteServiceException`) e `GlobalExceptionHandler`;
- `web`: `CorrelationIdFilter`, `PageResponse`;
- `security`: validação de JWT, conversão de roles e respostas 401/403 em JSON;
- `client`: interceptors de propagação do token e do Correlation ID.

Fica proibido colocar no `common`: entidades JPA, DTOs de negócio ou regras de domínio.

O módulo é ativado por **auto-configuração do Spring Boot**
(`META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`): basta declará-lo como
dependência. O API Gateway (WebFlux, reativo) não usa o `common`, porque a pilha é diferente.

## Consequências

- (+) Consistência total no formato de erros e na segurança.
- (+) Aprendizado: mostra como funcionam os *starters* do Spring Boot.
- (-) Uma mudança incompatível no `common` afeta todos os serviços. Mitigação: o `common` é pequeno, coberto
  por testes e versionado junto com os serviços (ADR-0001).
