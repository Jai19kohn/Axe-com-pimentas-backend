---
applyTo: "**"
---

# Arquitetura

O backend deve ser um monólito modular.

Organize o código por funcionalidade/domínio.

Evite uma arquitetura baseada apenas em camadas globais como:

controllers/
services/
repositories/
entities/

Prefira agrupar os componentes relacionados à mesma funcionalidade.

Exemplo:

products/
├── ProductController
├── ProductService
├── ProductRepository
└── Product

inventory/
├── InventoryController
├── InventoryService
└── InventoryRepository

Não criar microserviços neste momento.

Não criar abstrações ou interfaces sem necessidade real.

Controllers lidam com HTTP.

Services concentram regras de negócio.

Repositories lidam com persistência.

Mantenha os domínios desacoplados sempre que possível.
