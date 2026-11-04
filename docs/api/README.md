# Documentação das APIs

Todas as chamadas externas passam pelo **API Gateway** (`http://localhost:8080`). A documentação interativa
(Swagger UI) de cada serviço fica em sua própria porta:

| Serviço | Swagger UI | OpenAPI JSON |
|---------|------------|--------------|
| Customer | http://localhost:8081/swagger-ui.html | http://localhost:8081/v3/api-docs |
| Product | http://localhost:8082/swagger-ui.html | http://localhost:8082/v3/api-docs |
| Inventory | http://localhost:8083/swagger-ui.html | http://localhost:8083/v3/api-docs |
| Order | http://localhost:8084/swagger-ui.html | http://localhost:8084/v3/api-docs |

No Swagger, clique em **Authorize** e cole o `accessToken` obtido em `/auth/login`.

## Autenticação

```http
POST /auth/login
Content-Type: application/json

{"username": "admin", "password": "<GATEWAY_ADMIN_PASSWORD do .env>"}
```

```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
  "tokenType": "Bearer",
  "expiresIn": 3600,
  "roles": ["ADMIN", "USER"]
}
```

Envie o token em todas as demais chamadas: `Authorization: Bearer <accessToken>`.

## Formato de erro (todos os serviços)

```json
{
  "timestamp": "2026-11-04T15:00:00Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed",
  "path": "/api/customers",
  "correlationId": "4f1c2a0e-...",
  "violations": [
    {"field": "document", "message": "invalid Brazilian individual taxpayer registry number (CPF)"}
  ]
}
```

| Status | Quando |
|--------|--------|
| 400 | Corpo ou parâmetro inválido (Bean Validation, JSON malformado) |
| 401 | Token ausente, inválido ou expirado |
| 403 | Token válido, mas sem a role necessária, ou rota interna |
| 404 | Recurso não encontrado |
| 409 | Conflito: duplicidade, estoque insuficiente, transição de status inválida, edição concorrente |
| 422 | Regra de negócio: cliente/produto inexistente ou inativo no pedido, CPF ou SKU alterado |
| 503 | Um serviço dependente está indisponível |

## Endpoints

### Customer Service (`/api/customers`)

| Método | Caminho | Role | Descrição |
|--------|---------|------|-----------|
| POST | `/api/customers` | USER | Cadastra um cliente |
| GET | `/api/customers?active=&page=&size=&sort=` | USER | Lista paginada |
| GET | `/api/customers/{id}` | USER | Busca por id |
| PUT | `/api/customers/{id}` | USER | Atualiza (o CPF não pode mudar) |
| DELETE | `/api/customers/{id}` | ADMIN | Desativa (exclusão lógica) |
| PATCH | `/api/customers/{id}/activate` | ADMIN | Reativa |

```http
POST /api/customers
{"firstName":"Maria","lastName":"Silva","email":"maria@email.com","document":"529.982.247-25","phone":"11987654321"}
```

```json
HTTP/1.1 201 Created
Location: /api/customers/1

{"id":1,"firstName":"Maria","lastName":"Silva","email":"maria@email.com","document":"52998224725",
 "phone":"11987654321","active":true,"createdAt":"...","updatedAt":"..."}
```

### Product Service (`/api/categories`, `/api/products`)

| Método | Caminho | Role | Descrição |
|--------|---------|------|-----------|
| POST | `/api/categories` | ADMIN | Cria uma categoria |
| GET | `/api/categories` | USER | Lista as categorias |
| GET | `/api/categories/{id}` | USER | Busca uma categoria |
| PUT | `/api/categories/{id}` | ADMIN | Atualiza |
| DELETE | `/api/categories/{id}` | ADMIN | Remove (409 se houver produtos) |
| POST | `/api/products` | ADMIN | Cadastra um produto |
| GET | `/api/products?categoryId=&active=&name=&page=&size=` | USER | Lista com filtros |
| GET | `/api/products/{id}` | USER | Busca por id |
| PUT | `/api/products/{id}` | ADMIN | Atualiza nome, descrição, preço e categoria |
| DELETE | `/api/products/{id}` | ADMIN | Desativa |
| PATCH | `/api/products/{id}/activate` | ADMIN | Reativa |

```http
POST /api/products
{"sku":"MOUSE-001","name":"Mouse sem fio","description":"1600 DPI","price":99.90,"categoryId":1}
```

### Inventory Service (`/api/inventory`)

| Método | Caminho | Role | Descrição |
|--------|---------|------|-----------|
| POST | `/api/inventory` | ADMIN | Cria o estoque de um produto: `{"productId":1,"quantity":100}` |
| GET | `/api/inventory?page=&size=` | USER | Lista as posições de estoque |
| GET | `/api/inventory/{productId}` | USER | `{"productId":1,"quantityOnHand":100,"quantityReserved":2,"available":98}` |
| GET | `/api/inventory/{productId}/availability?quantity=5` | USER | `{"requested":5,"available":98,"sufficient":true}` |
| PUT | `/api/inventory/{productId}` | ADMIN | Ajuste físico: `{"quantityOnHand":150}` |
| POST | `/api/inventory/reservations` | interno | Reserva tudo ou nada (idempotente por `reference`) |
| GET | `/api/inventory/reservations/{reference}` | interno | Consulta uma reserva |
| POST | `/api/inventory/reservations/{reference}/commit` | interno | Baixa definitiva |
| POST | `/api/inventory/reservations/{reference}/release` | interno | Libera a reserva |

As rotas **internas** retornam `403` pelo gateway e só são chamadas pelo Order Service dentro da rede Docker.

### Order Service (`/api/orders`)

| Método | Caminho | Role | Descrição |
|--------|---------|------|-----------|
| POST | `/api/orders` | USER | Cria um pedido |
| GET | `/api/orders?customerId=&status=&page=&size=` | USER | Lista com filtros |
| GET | `/api/orders/{id}` | USER | Busca com os itens |
| PATCH | `/api/orders/{id}/status` | ADMIN | `{"status":"SHIPPED"}`: transições `CONFIRMED→SHIPPED→DELIVERED` ou `CONFIRMED→CANCELLED` |

```http
POST /api/orders
{"customerId":1,"items":[{"productId":1,"quantity":2}]}
```

```json
HTTP/1.1 201 Created
{
  "id": 1,
  "orderNumber": "3f6c1a7e-2b1d-4c8e-9f0a-1b2c3d4e5f60",
  "customerId": 1,
  "status": "CONFIRMED",
  "totalAmount": 199.80,
  "items": [
    {"productId":1,"productName":"Mouse sem fio","unitPrice":99.90,"quantity":2,"subtotal":199.80}
  ],
  "createdAt": "...",
  "updatedAt": "..."
}
```

Repare que o **preço não é enviado pelo cliente**: o Order Service o busca no Product Service. Se o cliente
mandasse o preço, poderia comprar qualquer coisa por R$ 0,01.

## Roteiro completo com curl

O script [`infrastructure/scripts/smoke-test.sh`](../../infrastructure/scripts/smoke-test.sh) executa o fluxo
inteiro (login, catálogo, estoque, cliente, pedido, envio) e é um ótimo exemplo de uso das APIs. O
[README](../../README.md#exemplos-de-requisições) mostra a versão passo a passo.
