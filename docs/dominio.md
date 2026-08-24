# Domínio e regras implementadas

> Este documento registra apenas comportamentos confirmados no código e, quando persistentes, nas migrations. Regras presentes somente como orientação no `AGENTS.md` não são apresentadas como funcionalidade entregue.

## Visão do domínio

O modelo separa três responsabilidades:

- **catálogos atuais:** Cliente, Material, Fornecedor, CategoriaServico, Servico, UnidadeMaoDeObra e os dois catálogos de status;
- **negociação histórica:** `Orcamento -> OrcamentoVersao -> linhas`;
- **execução operacional:** `OrdemServico`, originada de uma versão aprovada.

Alterações nos catálogos não reescrevem snapshots e valores já gravados nas linhas de versões.

## Orçamento e versão

### Criação

`OrcamentoService.salvar` executa na mesma transação:

1. exige Cliente existente e ativo;
2. resolve o status ativo de código `RASCUNHO`;
3. cria a raiz Orcamento, cujo `numero` vem da sequence PostgreSQL `orcamento_numero_seq`;
4. cria `OrcamentoVersao` número 1 com a observação recebida;
5. grava a V1 como `versaoAtual` da raiz.

O número comercial é único, independente do ID e pode ter lacunas. Não existe exclusão de Orcamento.

### Cliente da negociação

O Cliente pertence à raiz. Uma troca efetiva é aceita somente quando:

- há uma única versão;
- ela é a V1;
- ela é a versão atual;
- seu código de status é `RASCUNHO`;
- o novo Cliente existe e está ativo.

Reenviar o mesmo `clienteId` é idempotente. Fora do estado inicial, a troca é rejeitada.

### Versão atual e contexto

`Orcamento` mantém `versaoAtual` explicitamente. Escritas usam `OrcamentoVersaoGuard`, que:

1. bloqueia a raiz com lock pessimista;
2. busca e bloqueia a versão pertencente ao orçamento;
3. para conteúdo, exige que seja a versão atual em `RASCUNHO`.

Uma versão ou linha de outro orçamento/versão é tratada como não encontrada naquele contexto. Leituras históricas são permitidas.

## Estados comerciais

`StatusOrcamento` é entidade persistente. O código é normalizado para maiúsculas na criação, único e imutável pela Entity e por trigger PostgreSQL. O nome é persistido sem espaços externos e tem unicidade por `LOWER(BTRIM(nome))`.

Códigos estruturais usados pela aplicação:

```text
RASCUNHO -> ENVIADO -> APROVADO
    |          |  \-> RECUSADO
    |          \----> CANCELADO
    \---------------> CANCELADO
```

- `APROVADO`, `RECUSADO` e `CANCELADO` não possuem saída na política.
- Reenviar o mesmo ID de status é idempotente.
- Mudança efetiva exige status de destino existente e ativo.
- Código adicional cadastrado não participa automaticamente da máquina.
- Somente a versão atual pode transicionar.
- O PostgreSQL permite no máximo uma versão `APROVADO` por orçamento por índice único parcial.

## Edição e versionamento

Somente a versão atual em `RASCUNHO` pode alterar observação ou linhas. Versões enviadas ou terminais ficam congeladas para conteúdo.

Uma nova versão pode ser criada somente a partir da versão atual em `ENVIADO` ou `RECUSADO`, desde que o orçamento ainda não tenha versão aprovada. A operação:

- calcula `numeroVersao` como número da versão atual + 1 sob lock da raiz;
- cria a nova versão em `RASCUNHO`;
- copia a observação;
- clona itens, materiais, mão de obra e despesas;
- gera novas entidades/IDs;
- preserva diretamente referências, descrições, unidades, quantidades, valores e totais da origem;
- atualiza `versaoAtual` apenas depois da clonagem;
- ocorre em uma única transação.

A clonagem não consulta o estado atual dos catálogos e não recalcula as linhas. Unicidade de `(orcamento_id, numero_versao)` também existe no banco.

## Linhas e snapshots

Todas as rotas de linha recebem `orcamentoId` e `versaoId`. As linhas não possuem `ativo`; exclusão é física e permitida apenas na versão atual em `RASCUNHO`.

### ItemOrcamento

- exige Serviço ativo na inclusão ou troca efetiva;
- cria `descricao` com o nome atual do Serviço;
- permite alterar a descrição da linha sem renomear o catálogo;
- trocar o Serviço atualiza a descrição para o nome do novo Serviço, salvo descrição explicitamente enviada;
- `desconto` omitido na criação vira `0.00`;
- rejeita quantidade não positiva, valores negativos e desconto superior ao subtotal;
- `valorTotal = quantidade * valorUnitario - desconto`, com duas casas e `HALF_UP`.

### MaterialOrcamento

- exige Material ativo na inclusão ou troca efetiva;
- copia o nome para `descricao` e `Material.unidade` para o snapshot `unidade`;
- a unidade não é editada diretamente;
- trocar Material atualiza o snapshot de unidade e, salvo descrição explícita, a descrição;
- `custoTotal = quantidade * custoUnitario`, com duas casas e `HALF_UP`;
- não consulta `MaterialFornecedor` para definir o custo.

### MaoDeObraOrcamento

- exige UnidadeMaoDeObra ativa na inclusão ou troca efetiva;
- `descricao` é contexto obrigatório da linha;
- copia o nome da unidade selecionada para o snapshot textual `unidade`;
- reenviar a mesma unidade preserva o snapshot, mesmo se o cadastro tiver sido inativado depois;
- uma troca efetiva atualiza o snapshot a partir da nova unidade ativa;
- `custoTotal = quantidade * custoUnitario`, com duas casas e `HALF_UP`.

### DespesaOrcamento

- possui descrição contextual e valor informado diretamente;
- aceita zero e rejeita valor negativo;
- rejeita escala superior a duas casas, sem arredondamento silencioso.

Não há unicidade por referência ou descrição nas quatro categorias de linha.

## Totais da versão

Os totais não são colunas persistidas. `OrcamentoVersaoTotaisService` consulta agregados por versão e calcula:

```text
totalComercial       = SUM(ItemOrcamento.valorTotal)
custoTotalMateriais  = SUM(MaterialOrcamento.custoTotal)
custoTotalMaoDeObra  = SUM(MaoDeObraOrcamento.custoTotal)
custoTotalDespesas   = SUM(DespesaOrcamento.valor)
margemPrevista       = totalComercial - materiais - mão de obra - despesas
percentualMargem     = margemPrevista * 100 / totalComercial
```

- categoria sem linhas totaliza `0.00`;
- margem e percentual usam duas casas e `HALF_UP`;
- se `totalComercial` é zero, `percentualMargem` é `0.00` sem divisão;
- a margem continua refletindo os custos e pode ser negativa;
- a listagem de versões carrega cada categoria de totais em lote, não executando um `SUM` por versão dentro de loop.

## Ordem de serviço

### Criação

A criação é contextual a `/orcamentos/{orcamentoId}/versoes/{versaoId}/ordem-servico` e:

1. bloqueia Orcamento e depois OrcamentoVersao;
2. exige que a versão pertença ao orçamento e seja `versaoAtual`;
3. exige `StatusOrcamento.codigo = APROVADO`;
4. rejeita versão que já tenha uma ordem;
5. resolve o status ativo `COMPRAR_MATERIAL`;
6. cria a ordem referenciando a versão.

O número vem de `ordem_servico_numero_seq`. O PostgreSQL garante número único e uma ordem por versão. A ordem não possui `ativo`, DELETE ou criação sem origem comercial.

### Estados operacionais

`StatusOrdemServico` é um catálogo separado, com código único, normalizado para maiúsculas na criação e imutável por Entity e trigger.

```text
COMPRAR_MATERIAL -> EM_EXECUCAO -> INSTALAR -> CONCLUIDO
```

- não é permitido pular, voltar ou selecionar código adicional;
- reenviar o mesmo ID de status é idempotente;
- mudança efetiva exige destino existente e ativo;
- `CONCLUIDO` é terminal.

Após criada, a OrdemServico é bloqueada diretamente nas atualizações. Sua observação pode mudar em `COMPRAR_MATERIAL`, `EM_EXECUCAO` e `INSTALAR`; fica congelada em `CONCLUIDO`. Número, origem e status não fazem parte do update comum.

## Exclusão lógica e cadastros

DELETE define `ativo=false` para Cliente, Fornecedor, Material, MaterialFornecedor, CategoriaServico, Servico, UnidadeMaoDeObra, StatusOrcamento e StatusOrdemServico.

Comportamento das listagens:

- Cliente, Fornecedor e Material usam `findAll()` e incluem inativos;
- CategoriaServico, Servico, MaterialFornecedor, UnidadeMaoDeObra e os dois status retornam somente ativos;
- buscas por ID incluem registros inativos.

Regras adicionais implementadas:

- CategoriaServico, UnidadeMaoDeObra e nomes dos status são únicos por `LOWER(BTRIM(nome))` independentemente de `ativo`;
- Servico é único por categoria e nome normalizado;
- inativar CategoriaServico inativa seus Serviços ativos na mesma transação;
- reativar a categoria não reativa os Serviços;
- novo Serviço exige categoria ativa e nasce ativo;
- Serviço só é reativado se sua categoria estiver ativa;
- MaterialFornecedor é único por Material + Fornecedor, inclusive inativo;
- recadastrar o par inativo reativa o mesmo ID e atualiza `precoCompra`;
- atualizar vínculo inativo é rejeitado;
- criação, reativação e atualização do vínculo exigem Material e Fornecedor ativos;
- inativar Material ou Fornecedor não inativa automaticamente os vínculos.

## Divergências e decisões pendentes

- A semântica diferente das listagens de Cliente/Fornecedor/Material precisa ser confirmada como contrato intencional.
- Os limites de alguns DTOs não coincidem com o schema; não está confirmado se os limites menores são regras de negócio.
- A implementação de importação contorna os Services CRUD e não aplica de forma explícita todas as validações e regras compartilhadas.
- Os quatro Services de linha possuem testes unitários específicos para cálculos, limites, snapshots e seleção ou preservação de referências ativas/inativas. O fluxo integrado de versionamento também exercita clonagem e preservação histórica dessas linhas em PostgreSQL descartável.
