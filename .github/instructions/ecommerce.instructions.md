---
applyTo: "**/orders/**,**/cart/**,**/products/**"
---

# E-commerce

O sistema vende produtos de:

- pronta entrega;
- produção sob demanda;
- personalização.

Um produto pode ser vendido mesmo sem existir previamente como produto acabado em estoque, quando sua modalidade permitir produção sob demanda.

Pedidos devem preservar os dados relevantes no momento da compra.

O preço registrado no pedido não deve depender do preço atual do produto.

Personalizações pertencem ao item do pedido.

Exemplo:

Produto:
Vela personalizada

Item do pedido:

- aroma
- cor
- tamanho
- texto

Não criar um novo produto para cada combinação de personalização.

Não alterar o cadastro original do produto para armazenar as escolhas de um cliente específico.
