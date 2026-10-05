# Axé com Pimentas Backend

API REST de produtos para o e-commerce Axé com Pimentas.

## Requisitos

- Java 25
- Maven
- PostgreSQL

Configure `DB_URL`, `DB_USERNAME` e `DB_PASSWORD` para apontar para o banco PostgreSQL. Por padrão, a aplicação usa `jdbc:postgresql://localhost:5432/axe_com_pimentas` e o usuário `postgres`. `DB_DDL_AUTO` controla a estratégia de atualização do esquema e assume `update` quando não informada.

No PowerShell, por exemplo:

```powershell
$env:DB_URL="jdbc:postgresql://localhost:5432/axe_com_pimentas"
$env:DB_USERNAME="postgres"
$env:DB_PASSWORD="sua-senha"
mvn spring-boot:run
```

## Produtos

- `GET /api/products`: lista produtos ordenados por ID.
- `GET /api/products/{id}`: busca um produto; retorna `404` quando não encontrado.
- `POST /api/products`: cria um produto e retorna `201` com o cabeçalho `Location`.
- `PUT /api/products/{id}`: substitui os dados de um produto existente.
- `DELETE /api/products/{id}`: remove um produto e retorna `204`.
- `GET /api/products/{id}/fulfillment-plan?quantity={quantidade}`: calcula quanto pode ser atendido pelo estoque acabado e quanto precisaria ser produzido.

O corpo de `POST` e `PUT` contém `name`, `description`, `price`, `originalPrice` (opcional), `categoryId`, `stock` e `fulfillmentMode`. As modalidades são `READY_TO_SHIP`, `MADE_TO_ORDER` e `CUSTOM`. Campos textuais são obrigatórios e têm limite de tamanho; os preços devem ser maiores que zero e o estoque não pode ser negativo.

Para `READY_TO_SHIP`, o plano só permite o pedido quando há estoque suficiente. Para `MADE_TO_ORDER`, usa primeiro o estoque acabado e calcula a quantidade restante para produção. Para `CUSTOM`, encaminha toda a quantidade para produção. Essa rota é apenas de planejamento: não reserva nem baixa estoque e não cria pedido ou ordem de produção.

Execute `mvn test` para rodar os testes. Eles usam H2 e não precisam de um PostgreSQL ativo.
