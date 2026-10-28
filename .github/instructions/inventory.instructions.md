---
applyTo: "**/inventory/**"
---

# Estoque — Backend

## 1. Princípios gerais

O estoque não deve ser tratado simplesmente como uma quantidade armazenada no produto.

O sistema controla produtos acabados, matérias-primas, insumos, embalagens, entradas, saídas, ajustes, reservas, consumo na produção, devoluções e perdas.

Regras obrigatórias:

- Separar saldo físico, quantidade reservada e quantidade disponível.
- Não permitir estoque negativo, salvo regra explícita.
- Garantir consistência em operações concorrentes.
- Preservar o histórico de movimentações.
- Executar operações relacionadas em transações atômicas.
- Implementar validações no backend, independentemente do frontend.

## 2. Produtos e disponibilidade

Distinguir produtos PRONTA_ENTREGA e SOB_DEMANDA.

### Pronta entrega

- Controlar produtos acabados disponíveis para venda.
- Registrar entradas, saídas, ajustes, devoluções e perdas.
- Considerar reservas comerciais e demais compromissos aplicáveis ao calcular a disponibilidade.

### Sob demanda

- Possuir ficha técnica com os ingredientes, insumos, embalagens e quantidades necessários.
- Calcular a capacidade estimada de produção com base nos materiais disponíveis.
- Não confundir capacidade de produção com produtos acabados disponíveis.
- Considerar reservas, perdas previstas, unidades de medida, prazos e regras comerciais.

## 3. Insumos e custos de aquisição

Cada insumo deve possuir identificação, unidade de medida, saldo, histórico de movimentações e histórico de compras com custo unitário.

### Método de custeio obrigatório: último custo de compra

- Utilizar o custo unitário da compra mais recente registrada e válida para calcular o custo estimado atual do insumo.
- Definir a ordenação da compra mais recente por data efetiva da compra, com critério determinístico para empates.
- Não utilizar o custo médio ponderado como método padrão.
- Uma alteração manual de custo não deve substituir silenciosamente o histórico de compras.
- Ao registrar uma produção, preservar um snapshot do custo utilizado para cada material consumido.
- Alterações posteriores no custo de compra não devem modificar retroativamente o custo registrado de produções anteriores.
- Se não existir custo de compra válido, sinalizar custo não informado e impedir que o sistema apresente um custo total como se estivesse completo.

Validar unidades de medida e conversões antes de calcular custos ou quantidades.

## 4. Ficha técnica e produção

Cada produto fabricável deve possuir uma ficha técnica versionada com os materiais e as quantidades necessárias para produzir uma unidade ou lote.

### Início da produção: reservar

Ao iniciar uma ordem de produção:

1. Validar o estado da ordem e a disponibilidade dos materiais.
2. Calcular as quantidades necessárias com base na ficha técnica utilizada.
3. Reservar todos os materiais necessários atomicamente.
4. Registrar a reserva vinculada à ordem de produção.
5. Atualizar a quantidade disponível, sem realizar ainda a baixa física definitiva dos materiais.
6. Impedir reservas duplicadas para a mesma ordem.

A quantidade disponível deve excluir materiais já reservados, mesmo quando o saldo físico ainda não tiver sido reduzido.

### Conclusão da produção: consumir

Ao concluir uma ordem:

1. Validar se a ordem está em um estado que permite conclusão.
2. Determinar as quantidades efetivamente consumidas.
3. Converter as reservas correspondentes em consumo efetivo, sem contabilizar duas vezes a mesma quantidade.
4. Registrar eventuais diferenças entre consumo previsto e real, incluindo perdas.
5. Dar entrada nos produtos acabados efetivamente produzidos.
6. Registrar os custos utilizados e o histórico da produção.
7. Concluir tudo em uma transação atômica.

O consumo efetivo deve refletir os materiais realmente utilizados, e não simplesmente repetir as quantidades planejadas quando houver divergências registradas.

### Cancelamento e falhas

- Cancelar uma ordem antes do início sem movimentar materiais.
- Ao cancelar uma ordem iniciada e ainda não concluída, liberar as reservas não consumidas.
- Se houver consumo real antes do cancelamento, registrar o consumo e os estornos ou ajustes cabíveis; nunca apagar movimentações.
- Impedir a conclusão ou o cancelamento duplicado de uma ordem já processada.
- Garantir que falhas transacionais não deixem reservas, saldos ou produtos acabados parcialmente atualizados.

## 5. Movimentações e concorrência

Toda movimentação deve registrar o item afetado, tipo, quantidade, unidade, data, motivo, origem e responsável, quando aplicável.

- Utilizar controle de concorrência apropriado à persistência adotada.
- Evitar reservas simultâneas que ultrapassem o saldo disponível.
- Impedir que requisições repetidas dupliquem movimentações.
- Registrar estornos como novas movimentações vinculadas à operação original.
- Preservar histórico de compras, produção, custos e estoque.
- Não excluir fisicamente registros que comprometam a auditoria.

## 6. Calculadora de custos e precificação

O backend deve fornecer cálculos oficiais de custo, preço sugerido, lucro estimado, margem e markup.

### Custo de fabricação

Calcular o custo dos materiais pela quantidade efetivamente necessária multiplicada pelo último custo de compra válido, respeitando conversões de unidades.

Considerar também, quando cadastrados:

- Embalagens e materiais auxiliares;
- Mão de obra;
- Perdas;
- Custos indiretos;
- Taxas e despesas variáveis.

Não considerar o custo completo quando existirem componentes obrigatórios sem custo informado. Indicar os componentes pendentes.

### Preço por margem desejada

A margem desejada deve representar a proporção do preço de venda que permanece como lucro estimado após os custos considerados.

Quando os custos fixos por unidade forem conhecidos e as despesas variáveis forem percentuais do preço de venda:

Preço sugerido = custo unitário / (1 - percentual de despesas variáveis - margem desejada).

Validar que a soma do percentual de despesas variáveis com a margem desejada seja inferior a 100%.

Se houver despesas variáveis em valor fixo por unidade, incluí-las no numerador conforme a fórmula definida para o modelo de custos.

Fórmulas de referência:

Lucro estimado por unidade = preço de venda - custos unitários - despesas variáveis.

Margem estimada (%) = lucro estimado por unidade / preço de venda × 100.

Markup = preço de venda / custo de referência, quando aplicável e com denominador maior que zero.

- Não confundir margem com markup.
- Utilizar BigDecimal para valores monetários.
- Definir política consistente de arredondamento.
- Tratar preço de venda igual a zero sem divisão por zero.
- Separar simulação de atualização efetiva do preço cadastrado.
- Preservar snapshots dos custos usados em produções e vendas históricas.

## 7. Critérios de aceitação — Backend

### Cadastro e estoque

- [ ] É possível cadastrar produtos acabados, produtos sob demanda e insumos.
- [ ] O sistema diferencia saldo físico, reservas e disponibilidade.
- [ ] Entradas, saídas, ajustes, perdas e devoluções geram histórico.
- [ ] O sistema rejeita operações que causariam estoque negativo indevido.

### Produção

- [ ] Ao iniciar uma ordem, os materiais são reservados sem baixa física definitiva.
- [ ] A quantidade disponível diminui conforme as reservas são criadas.
- [ ] Outra ordem não consegue reservar os mesmos materiais já comprometidos.
- [ ] Ao concluir a produção, as reservas são convertidas em consumo efetivo sem duplicidade.
- [ ] Os produtos acabados são adicionados ao estoque somente na conclusão.
- [ ] Ao cancelar uma ordem iniciada, os materiais não consumidos voltam a ficar disponíveis.
- [ ] Requisições duplicadas e falhas não duplicam nem deixam movimentações parciais.
- [ ] Diferenças entre consumo planejado e real são registradas.

### Custos e precificação

- [ ] O custo estimado utiliza o último custo de compra válido de cada insumo.
- [ ] Alterações no custo atual não modificam snapshots históricos.
- [ ] A ausência de custo obrigatório é sinalizada.
- [ ] O preço sugerido considera a margem desejada e as despesas variáveis.
- [ ] Margens inválidas, divisões por zero e valores monetários inválidos são tratados.
- [ ] Os cálculos são executados no backend e cobertos por testes automatizados.

### Qualidade

- [ ] Existem testes para concorrência, reservas, conclusão, cancelamento, custos e precificação.
- [ ] As operações críticas são transacionais.
- [ ] A implementação respeita a arquitetura e as dependências existentes.
- [ ] Os testes relevantes são executados antes de considerar a tarefa concluída.

## 8. Implementação

Antes de modificar o código, verificar a arquitetura existente, as instruções gerais e a documentação oficial da versão efetivamente utilizada do Spring Boot.

Implementar em etapas pequenas, reutilizando os mecanismos existentes. Não inventar endpoints, entidades ou regras comerciais sem verificar o projeto.
