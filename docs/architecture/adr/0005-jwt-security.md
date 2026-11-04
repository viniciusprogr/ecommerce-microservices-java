# ADR-0005: JWT HS256 emitido pelo gateway e validado em todos os serviços

- **Status:** Aceita
- **Data:** 2026-11-04

## Contexto

Precisamos de autenticação stateless e de autorização por roles, de forma didática e sem infraestrutura extra.

## Decisão

- O **API Gateway** expõe `POST /auth/login`. Ele valida usuário e senha contra usuários definidos por
  variáveis de ambiente (as senhas viram hash BCrypt na inicialização) e emite um JWT assinado com
  **HMAC-SHA256**. Claims: `iss`, `sub`, `iat`, `exp` e `roles` (ex.: `["ADMIN","USER"]`).
- O gateway valida o token em todas as rotas `/api/**` e **bloqueia as rotas internas**
  (`/api/inventory/reservations/**`).
- **Cada serviço também valida o token** (Spring Security OAuth2 Resource Server) e aplica as regras de role do
  seu domínio. Isso é **defesa em profundidade**: quem chegar a um serviço sem passar pelo gateway continua sem
  acesso.
- O segredo (`JWT_SECRET`, mínimo de 32 caracteres) vem de variável de ambiente. Nenhuma credencial vai para o Git.
- Erros 401/403 retornam JSON genérico, sem revelar o motivo exato da falha. O login responde a mesma mensagem
  para usuário inexistente e para senha errada.

Matriz de autorização:

| Recurso | Leitura (GET) | Escrita |
|---------|---------------|---------|
| Clientes | USER, ADMIN | criar/atualizar: USER, ADMIN; desativar/reativar: ADMIN |
| Produtos e categorias | USER, ADMIN | ADMIN |
| Estoque | USER, ADMIN | ADMIN |
| Reservas de estoque | interno (bloqueado no gateway) | interno |
| Pedidos | USER, ADMIN | criar: USER, ADMIN; alterar status: ADMIN |

## Alternativas consideradas

- **Keycloak/Auth0 com RS256 e JWKS**: é o padrão de mercado, mas adiciona um container pesado e conceitos
  (realms, clients) que desviam do foco. Ficou como desafio de evolução.
- **Sessão com cookie**: exige estado no servidor, o que não combina com microsserviços.

## Consequências

- (+) Simples, sem estado e testável.
- (-) Chave simétrica: quem valida também conseguiria emitir tokens. Isso é aceitável aqui porque todos os
  serviços são confiáveis. Com RS256, só o emissor tem a chave privada.
- (-) Não há *refresh token* nem revogação. Os tokens expiram em 60 minutos (configurável).
- (-) Usuários definidos em configuração não escalam. É um substituto didático para um Identity Provider.
- (-) Um `USER` pode consultar pedidos de outros clientes, porque não existe vínculo entre o usuário do login e o
  cadastro de cliente. Essa limitação é conhecida e virou exercício no guia de estudos.
