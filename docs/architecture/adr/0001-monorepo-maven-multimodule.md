# ADR-0001: Monorepo com Maven multimódulo

- **Status:** Aceita
- **Data:** 2026-11-04

## Contexto

O projeto tem 5 aplicações e 1 biblioteca. Precisamos de um build reproduzível, versões de dependências
consistentes e uma forma simples de rodar tudo localmente e no CI.

## Decisão

Um único repositório (monorepo) com um `pom.xml` pai do tipo `pom`, que:

- herda de `spring-boot-starter-parent` (gerência de versões e plugins);
- importa o BOM do Spring Cloud;
- declara cada serviço como `<module>`;
- configura Checkstyle e JaCoCo uma única vez para todos.

O **Maven Wrapper** (`mvnw`) é versionado, então qualquer pessoa ou CI usa exatamente o Maven 3.9.9.

## Consequências

- (+) Um comando compila e testa tudo: `./mvnw verify`.
- (+) Fácil de estudar: o código de todos os serviços está à mão.
- (+) Cada serviço continua gerando seu próprio JAR e sua própria imagem Docker, com deploy independente.
- (-) Os serviços compartilham o ciclo de versão (`1.0.0`). Em uma empresa com times independentes, cada
  serviço poderia ter seu próprio repositório e sua própria versão.
- (-) O build completo fica mais lento à medida que o projeto cresce. Mitigação:
  `./mvnw -pl order-service -am verify` compila apenas um serviço e suas dependências.
