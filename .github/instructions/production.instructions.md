---
applyTo: "**/production/**"
---

# Produção

Alguns produtos são fabricados sob demanda.

O sistema poderá futuramente controlar:

- ordens de produção;
- quantidade a produzir;
- status da produção;
- matérias-primas necessárias;
- consumo de matérias-primas;
- produto acabado.

Uma ordem de produção deve ser tratada separadamente de um pedido de cliente.

Um pedido pode gerar uma necessidade de produção, mas os conceitos não devem ser confundidos.

Não implementar funcionalidades de produção sem solicitação explícita.
