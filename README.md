# Axé com Pimentas Backend

API REST de produtos para o e-commerce Axé com Pimentas.

## Requisitos

- Java 25
- Maven
- PostgreSQL

Configure `DB_URL`, `DB_USERNAME` e `DB_PASSWORD` para apontar para o banco PostgreSQL. Por padrão, a aplicação usa `jdbc:postgresql://localhost:5432/axe_com_pimentas` e o usuário `postgres`. `DB_DDL_AUTO` controla a estratégia de atualização do esquema e assume `update` quando não informada.

Configure também `ADMIN_USERNAME` e `ADMIN_PASSWORD` para criar o único usuário administrador. A aplicação não inicia se essas credenciais estiverem ausentes. O backend usa autenticação HTTP Basic e exige o papel de administrador para criar, editar ou remover produtos e gravar movimentações de estoque; mantenha a API atrás de HTTPS fora do ambiente local.

Para iniciar o PostgreSQL local com Docker Compose e executar a aplicação no PowerShell:

```powershell
docker compose up -d
$env:DB_PASSWORD="postgres"
$env:ADMIN_USERNAME="admin"
$env:ADMIN_PASSWORD="defina-uma-senha-forte"
mvn spring-boot:run
```

Para rodar sem PostgreSQL, use o perfil `h2` (banco em memória; os dados são perdidos ao encerrar):

```powershell
$env:ADMIN_USERNAME="admin"
$env:ADMIN_PASSWORD="defina-uma-senha-forte"
$env:SPRING_PROFILES_ACTIVE="h2"
mvn spring-boot:run
```

As credenciais padrão do Compose são apenas para desenvolvimento local. Em outros ambientes, configure `DB_PASSWORD` com um segredo próprio.

## Backups em pasta de rede

O Compose mantém os dados ativos no volume Docker `postgres_data`. Esse volume sobrevive a reinícios normais dos contêineres, mas fica no disco do computador que executa o Docker. Para manter uma cópia separada na rede, o script `scripts/backup-postgres.ps1` cria um dump PostgreSQL em formato customizado, valida sua estrutura e grava o arquivo em uma pasta compartilhada SMB/UNC. Após criar o backup com sucesso, mantém os 14 mais recentes; falhas de conexão, geração ou cópia interrompem o script e não iniciam a limpeza.

Crie uma pasta compartilhada em outro computador ou NAS e conceda permissão de gravação à conta do Windows que executará o script. Como o dump contém os dados do banco, restrinja o acesso à pasta às pessoas autorizadas. Use o caminho UNC (por exemplo, `\\OUTRO-PC\Backups\Axe`), não uma unidade mapeada. Com o PostgreSQL do Compose em execução:

```powershell
.\scripts\backup-postgres.ps1 -DestinationPath "\\OUTRO-PC\Backups\Axe"
```

Também é possível configurar `AXE_BACKUP_PATH` no ambiente e executar o script sem parâmetros. O destino deve existir e estar acessível; o script não armazena credenciais de rede. Faça um teste manual antes de agendar.

Para criar uma tarefa diária no Windows Task Scheduler:

1. Crie uma tarefa com gatilho diário e configure-a para executar com a mesma conta que tem acesso à pasta compartilhada.
2. Em **Ação**, use `powershell.exe` como programa e informe os argumentos abaixo, substituindo os caminhos pelo local real:

   ```text
   -NoProfile -File "C:\caminho\Axe-com-pimentas-backend\scripts\backup-postgres.ps1" -DestinationPath "\\OUTRO-PC\Backups\Axe"
   ```

3. Configure a tarefa para executar quando o computador e o Docker Desktop estiverem disponíveis. Se uma execução agendada for perdida porque o computador estava desligado, habilite a opção de executá-la assim que possível.
4. Consulte o histórico da tarefa e a pasta compartilhada para confirmar as execuções. Se o compartilhamento estiver indisponível ou o PostgreSQL não estiver rodando, o script termina com erro em vez de indicar sucesso.

Para testar um dump sem tocar no banco usado pela aplicação, restaure-o em um banco temporário:

```powershell
$backup = Get-ChildItem "\\OUTRO-PC\Backups\Axe" -Filter "axe-com-pimentas-*.dump" |
    Sort-Object LastWriteTime -Descending |
    Select-Object -First 1
docker cp $backup.FullName axe-com-pimentas-postgres:/tmp/axe-restore-test.dump
docker compose exec -T postgres createdb -U postgres axe_restore_test
docker compose exec -T postgres pg_restore -U postgres -d axe_restore_test /tmp/axe-restore-test.dump
docker compose exec -T postgres dropdb -U postgres axe_restore_test
docker exec axe-com-pimentas-postgres rm -f /tmp/axe-restore-test.dump
```

Execute esse teste periodicamente e só remova o banco temporário depois de confirmar que a restauração terminou. Os backups na rede protegem contra falha do disco do servidor, mas não contra indisponibilidade ou perda do próprio dispositivo de rede; mantenha cópias adicionais se precisar de proteção contra esse cenário.

## Produtos

- `GET /api/products`: lista produtos ordenados por ID.
- `GET /api/products/{id}`: busca um produto; retorna `404` quando não encontrado.
- `GET /api/products/next-sku`: reserva e retorna o próximo SKU numérico de seis dígitos; exige autenticação de administrador.
- `POST /api/products`: cria um produto e retorna `201` com o cabeçalho `Location`.
- `PUT /api/products/{id}`: substitui os dados de um produto existente.
- `DELETE /api/products/{id}`: remove um produto e retorna `204`.
- `GET /api/products/{id}/fulfillment-plan?quantity={quantidade}`: calcula quanto pode ser atendido pelo estoque acabado e quanto precisaria ser produzido.
- `GET /api/admin/session`: valida as credenciais do administrador e retorna `204` quando autenticado.
- `GET /api/inventory/products/{productId}/finished-stock`: consulta o estoque acabado.
- `PUT /api/inventory/products/{productId}/finished-stock`: compatibilidade legada para definir o saldo com `{ "quantity": 10 }`; o ajuste é auditado sem motivo detalhado.
- `GET /api/inventory/products/{productId}/finished-stock/movements`: lista o histórico do produto.
- `GET /api/admin/inventory/movements`: lista o histórico de todos os produtos, dos mais recentes aos mais antigos; exige autenticação administrativa.
- Ao criar um produto com `initialStock` maior que zero, o saldo e uma movimentação de entrada com motivo `Estoque inicial` são registrados na mesma transação. Quantidade zero ou ausente não gera movimentação.
- `POST /api/inventory/products/{productId}/finished-stock/movements`: registra entrada (`RECEIPT`), saída (`ISSUE`), ajuste (`ADJUSTMENT`) ou avaria (`DAMAGE`).
- `POST /api/inventory/products/{productId}/finished-stock/movements/{movementId}/reversal`: estorna um lançamento sem removê-lo do histórico.
- `GET /api/suppliers`: lista fornecedores ativos; exige autenticação de administrador.
- `POST /api/suppliers`: cadastra um fornecedor; exige autenticação de administrador.

O corpo de `POST /movements` contém `type`, `quantity`, `reason`, `responsible`, `idempotencyKey` (UUID) e `reference` (opcional). A quantidade de `ADJUSTMENT` é o novo saldo desejado; nos demais tipos, é a quantidade a movimentar. Repetir a mesma requisição com a mesma chave retorna o lançamento original; reutilizar a chave com dados diferentes retorna `409`. O estorno recebe `reason`, `responsible` e uma nova `idempotencyKey`; cada lançamento pode ser estornado uma vez, e o estorno será recusado se tornar o saldo negativo. O saldo e o histórico são atualizados na mesma transação. Produtos com histórico de movimentações não podem ser excluídos; desative-os para removê-los do catálogo.

O corpo de `POST` e `PUT` de produtos contém `name`, `description`, `price`, `originalPrice` (opcional), `categoryId`, `fulfillmentMode` e os campos operacionais `sku`, `barcode` (opcional), `supplierId` (opcional), `purchaseCost`, `active`, `variationColor`, `variationSize` e `variationModel`. SKU é numérico, único e tem seis dígitos; a interface reserva e exibe o próximo código antes do cadastro. Fornecedores são cadastrados separadamente. As modalidades são `READY_TO_SHIP`, `MADE_TO_ORDER` e `CUSTOM`. Produtos inativos não aparecem no catálogo nem podem ser planejados para atendimento. Estoque não faz parte do cadastro nem da resposta do catálogo; consulte e ajuste o saldo pela API de inventário. Campos textuais são obrigatórios e têm limite de tamanho; os preços devem ser maiores que zero e o saldo não pode ser negativo.

Para `READY_TO_SHIP`, o plano só permite o pedido quando há estoque suficiente. Para `MADE_TO_ORDER`, usa primeiro o estoque acabado e calcula a quantidade restante para produção. Para `CUSTOM`, encaminha toda a quantidade para produção. Essa rota é apenas de planejamento: não reserva nem baixa estoque e não cria pedido ou ordem de produção.

Execute `mvn test` para rodar os testes. Eles usam H2 e não precisam de um PostgreSQL ativo.
