---
applyTo: "**/inventory/**"
---

# Estoque

O estoque não deve ser tratado simplesmente como uma quantidade armazenada no produto.

O sistema poderá controlar:

- produtos acabados;
- matérias-primas;
- insumos;
- embalagens;
- entradas;
- saídas;
- ajustes;
- consumo na produção;
- devoluções;
- perdas.

Uma quantidade zero não significa necessariamente que o produto esteja indisponível.

A disponibilidade pode depender de:

- estoque disponível;
- possibilidade de produção;
- matéria-prima;
- regras do produto.

Movimentações de estoque devem manter consistência dos dados.

Considerar concorrência em operações de entrada, saída e reserva.

Evitar estoque negativo, salvo quando houver uma regra explícita permitindo isso.
