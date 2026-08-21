# AGENTS.md — Núcleo ERP

## 1. Propósito e contexto

Este repositório contém a API REST monolítica do ERP da **Núcleo das Reformas**.

O sistema substitui um ERP legado em Microsoft Access. A migração deve preservar dados e regras de negócio relevantes, mas não reproduzir automaticamente a estrutura técnica do legado. O modelo novo deve refletir conceitos de domínio normalizados e consistentes.

Stack principal:

* Java 21;
* Spring Boot;
* Maven;
* Spring MVC;
* Spring Data JPA;
* PostgreSQL;
* Flyway;
* Bean Validation;
* Lombok;
* Apache POI;
* Springdoc OpenAPI.

## 2. Ambiente

Use JDK 21, conforme definido no `pom.xml`, e confirme a versão ativa antes de compilar ou testar:

```powershell
java -version
```

Utilize preferencialmente o Maven Wrapper:

```powershell
.\mvnw.cmd
```

ou, em ambientes Unix:

```bash
./mvnw
```

O banco de desenvolvimento é PostgreSQL. A senha deve ser fornecida externamente por `DB_PASSWORD`. Nunca registre senhas, tokens ou outras credenciais no repositório.

## 3. Arquitetura

O projeto é organizado por domínio ou funcionalidade:

```text
<dominio>/
  controller/
  dto/
  entity/
  mapper/
  repository/
  service/
```

O fluxo esperado é:

```text
HTTP -> Controller -> Service -> Mapper / Repository -> PostgreSQL
```

### Controller

* Recebe requisições HTTP, aplica `@Valid`, delega ao Service e define a resposta HTTP.
* Trabalha exclusivamente com DTOs na fronteira HTTP.
* Não contém regra de negócio, não acessa Repository e não controla transações.

### Service

* Concentra regras de negócio, validações entre entidades, coordenação de repositories e transações.
* Operações de escrita executam em transação de escrita.
* Operações exclusivamente de leitura usam `@Transactional(readOnly = true)`.
* Uma mesma regra não deve ser distribuída entre Controller, Mapper e Repository.

### Repository

* É uma interface Spring Data que estende `JpaRepository` e concentra apenas consultas necessárias à persistência.
* Não contém regra de negócio.

### Mapper

* É manual e estático.
* Converte Request, Entity e Response sem consultar banco ou aplicar regra de negócio.

Os Services são classes concretas anotadas com `@Service` e injetadas diretamente. Não crie interfaces de Service, classes `ServiceImpl`, abstrações genéricas de CRUD ou camadas arquiteturais adicionais sem necessidade concreta de domínio ou arquitetura.

Use injeção por construtor, dependências `final` e preferencialmente `@RequiredArgsConstructor`. Não use `@Autowired` em campos.

`spring.jpa.open-in-view` permanece desabilitado. Relacionamentos necessários devem ser carregados e convertidos para DTO dentro da fronteira transacional; a serialização HTTP não pode depender de lazy loading.

## 4. Convenções e contratos HTTP

Use nomes de domínio em português e sufixos técnicos em inglês, como `ServicoService`, `MaterialRepository` e `ClienteResponse`.

Mantenha estas convenções:

* métodos CRUD: `salvar`, `buscarPorId`, `listar`, `atualizar` e `deletar`;
* rotas em português, minúsculas e no plural, sem prefixo global `/api` enquanto não houver decisão explícita diferente;
* tabelas em português, no singular e em `snake_case`;
* pacotes de domínios compostos em `snake_case`, mantendo classes em `PascalCase`.

Controllers não recebem nem retornam Entity. Use DTOs específicos de entrada e saída, e mantenha Request, Response, validação, Mapper, Controller, OpenAPI e testes coerentes quando o contrato mudar.

Bean Validation deve refletir o schema real e regras confirmadas do domínio. Não invente nulabilidade, tamanho, unicidade ou outras restrições por conveniência do código Java.

Não use `@NotNull` em `boolean` primitivo para detectar ausência no JSON. Quando for necessário distinguir `ausente`, `false` e `true`, use `Boolean`; quando houver somente dois estados e default controlado internamente, avalie `boolean` de acordo com o contrato e o schema.

## 5. Persistência e integridade

PostgreSQL é o contrato persistente. Hibernate usa `ddl-auto=validate` e não deve criar nem alterar o schema.

Toda mudança estrutural permanente deve ser feita por Flyway. Antes de criar ou alterar Entity, DTO ou migration, confirme no schema real:

* nomes e tipos de colunas;
* nulabilidade, tamanho, precisão e escala;
* defaults e estratégia de auditoria;
* índices, chaves estrangeiras e constraints;
* dados existentes e compatibilidade da alteração.

Migrations seguem `V<versao>__<descricao>.sql`. Não reutilize, renomeie, altere ou remova migration aplicada sem verificar `flyway_schema_history` e obter autorização explícita. Nunca execute `flyway clean` em banco compartilhado ou com dados relevantes.

### Unicidade e concorrência

Uma regra de unicidade permanente deve possuir garantia no PostgreSQL. A validação preventiva no Service existe para produzir mensagem amigável, mas não substitui índice ou constraint porque requisições concorrentes podem ultrapassar um `existsBy...`.

Em atualizações, a validação de unicidade deve excluir o próprio registro. Violações concorrentes da constraint devem ser traduzidas para o contrato de erro do domínio quando apropriado.

### Exclusão lógica

Entidades com campo `ativo` usam exclusão lógica: `deletar` define `ativo=false` em vez de remover fisicamente o registro.

Para novos módulos, listagens operacionais devem retornar somente ativos, salvo caso de uso histórico ou administrativo explícito. Buscas por identidade podem incluir inativos quando necessárias para manutenção ou histórico. Qualquer cascata de inativação deve ser uma regra explícita do domínio, não uma suposição genérica.

### Auditoria

Campos de criação devem ter uma estratégia de preenchimento consistente no código. Um `DEFAULT CURRENT_TIMESTAMP` no banco pode servir como proteção estrutural, mas não justifica combinar desnecessariamente `@CreationTimestamp`, `@PrePersist` e preenchimento manual no Service.

## 6. Regras permanentes de domínio

### Material, Fornecedor e MaterialFornecedor

`Material` e `Fornecedor` são entidades independentes. A relação entre eles é modelada por `MaterialFornecedor`, e não por `@ManyToMany` simples, porque possui identidade, ciclo de vida e atributos próprios, como `precoCompra`.

Unidade, largura e demais características intrínsecas pertencem ao cadastro de Material. Preço de compra e informações específicas da oferta pertencem a `MaterialFornecedor`; não coloque preço de fornecedor no cadastro principal de Material.

Regras da relação:

* existe no máximo um vínculo para cada combinação de Material e Fornecedor, independentemente de estar ativo ou inativo;
* a unicidade absoluta do par deve ser garantida no PostgreSQL;
* ao cadastrar novamente um par já existente e inativo, reative o mesmo vínculo e preserve seu `id`, em vez de criar outro registro;
* um vínculo inativo não pode ser alterado pela atualização comum; sua reativação e atualização ocorrem pelo recadastro do mesmo par;
* criação, reativação e manutenção operacional exigem Material e Fornecedor ativos;
* `precoCompra`, quando informado, usa `BigDecimal`, não pode ser negativo e deve permanecer protegido por constraint no PostgreSQL;
* inativar Material ou Fornecedor não apaga nem inativa automaticamente vínculos existentes, que são preservados para histórico;
* informações exclusivas da relação não pertencem aos cadastros principais de Material ou Fornecedor;
* não adicione `unidadeCompra`, histórico de preços ou outros atributos preventivamente; exija regra de negócio comprovada.

### CategoriaServico e Servico

`CategoriaServico` usa exclusão lógica. Categorias novas são sempre criadas ativas, e seu estado pode ser controlado explicitamente pelo fluxo de atualização.

O nome da categoria é persistido sem espaços externos e é único segundo `LOWER(BTRIM(nome))`, com garantia no PostgreSQL independentemente do estado ativo. Diferenças apenas de caixa ou espaços externos não criam novas categorias; acentos não são removidos nem normalizados.

Todo `Servico` pertence obrigatoriamente a uma `CategoriaServico`. O vínculo é unidirecional do serviço para a categoria; não crie coleção bidirecional preventivamente.

O serviço é único dentro da categoria segundo `categoria_servico_id + LOWER(BTRIM(nome))`, com garantia no PostgreSQL independentemente do estado ativo. O nome é persistido sem espaços externos, sem remoção ou normalização de acentos.

Regras de ciclo de vida:

* um serviço novo é sempre criado ativo e só pode ser associado a categoria existente e ativa;
* ao trocar a categoria, a nova categoria deve existir e estar ativa;
* um serviço só pode ser ativado ou reativado se sua categoria estiver ativa;
* um serviço que permanecerá inativo pode ser atualizado preservando seu vínculo atual com uma categoria inativa;
* preservar o vínculo atual não autoriza criar uma nova associação com categoria inativa;
* inativar uma CategoriaServico, por atualização ou exclusão lógica, inativa logicamente todos os serviços vinculados na mesma transação;
* reativar uma CategoriaServico não reativa seus serviços; cada serviço permanece inativo até reativação explícita.

O cadastro de serviço não deve possuir preço comercial. Valores negociados pertencem ao contexto do orçamento e não podem alterar retroativamente negociações antigas.

### StatusOrcamento e separação operacional

`StatusOrcamento` é um cadastro persistente e administrável no PostgreSQL, não um enum Java. `codigo` é sua identidade funcional estável: obrigatório, único e imutável. Regras estruturais devem usar o código e nunca depender do `id` técnico ou do nome de apresentação.

Os códigos canônicos do fluxo comercial são `RASCUNHO`, `ENVIADO`, `APROVADO`, `RECUSADO` e `CANCELADO`. Status adicionais podem existir, mas não entram automaticamente na máquina de estados; qualquer participação exige regra de domínio explícita. A atribuição de código a registros adicionais ou legados somente pode ser automatizada quando o resultado for inequívoco, sem derivá-lo de ID técnico; ambiguidades exigem mapeamento explícito e devem interromper a migração ou importação.

O nome do status é persistido sem espaços externos e é único segundo `LOWER(BTRIM(nome))`, independentemente de estar ativo ou inativo. Diferenças apenas de caixa ou espaços externos não criam novos status; acentos não são removidos nem normalizados.

A inativação de um status é lógica. Versões históricas podem continuar referenciando status inativos, mas somente status ativo pode ser selecionado na criação da versão ou em uma mudança efetiva de status.

`StatusOrcamento` representa exclusivamente o estado comercial de `OrcamentoVersao`, não da raiz `Orcamento`. Estados da execução pertencem a `StatusOrdemServico`; os dois catálogos e ciclos de vida possuem responsabilidades distintas e não devem reutilizar o mesmo cadastro nem misturar códigos ou regras estruturais.

### UnidadeMaoDeObra

`UnidadeMaoDeObra` é um cadastro persistente e administrável no PostgreSQL, não um enum Java nem texto livre. `MaoDeObraOrcamento` deve referenciar obrigatoriamente esse cadastro por sua identidade persistida. Somente unidade ativa pode ser selecionada na criação ou em uma troca efetiva; reenviar a mesma unidade não constitui nova seleção e preserva o vínculo histórico mesmo que o cadastro tenha sido inativado.

O nome da unidade é persistido sem espaços externos e é único segundo `LOWER(BTRIM(nome))`, com garantia no PostgreSQL independentemente de estar ativa ou inativa. Diferenças apenas de caixa ou espaços externos não criam novas unidades; acentos não são removidos nem normalizados. Unidades novas são criadas ativas, e um POST duplicado não reativa registro inativo; inativação e reativação são alterações explícitas do ciclo de vida.

`MaoDeObraOrcamento` preserva em `unidade` o snapshot do nome da `UnidadeMaoDeObra`. Renomear posteriormente o cadastro não altera linhas históricas; uma troca efetiva atualiza o snapshot a partir da nova unidade. A descrição é contexto próprio da linha e permanece independente do nome da unidade.

### Orçamentos, versões e valores monetários

`Orcamento` é a raiz da negociação comercial, não o documento versionado nem um cadastro sujeito à exclusão lógica. Mantém conceitualmente identidade, número comercial, Cliente, referência explícita para a versão atual e data de criação. Não possui campo `ativo` e não deve ser excluído física ou logicamente. `OrcamentoVersao` representa um documento comercial e financeiro específico da negociação; status, observação, linhas e agregados pertencem à versão.

O `numero` de `Orcamento` é uma identificação comercial persistente e única, compartilhada por todas as suas versões e independente do `id` técnico. Deve ser gerado pelo PostgreSQL com mecanismo seguro sob concorrência; nunca o derive do `id` nem use `MAX(numero) + 1`. Lacunas são aceitáveis e números já consumidos não devem ser reutilizados manualmente.

Todo novo orçamento cria, na mesma transação, a raiz, a V1 em `RASCUNHO` e a referência `versaoAtual`. O Cliente e o status `RASCUNHO` devem existir e estar ativos; o status é resolvido por `codigo`, nunca por ID ou nome convencionado. A criação inicial é a exceção ao lock pessimista da raiz porque ela ainda não existe.

`Orcamento` mantém referência explícita para `versaoAtual`; não espalhe pelo sistema lógica que infira a versão atual por `MAX(numeroVersao)` ou apenas pela maior numeração. Somente a versão atual pode receber escritas, transicionar ou originar nova versão. Versões anteriores são históricas, imutáveis e devem permanecer reconstruíveis.

O Cliente pertence à raiz e não é versionado. Pode ser substituído somente enquanto houver apenas V1, ela for a versão atual e estiver em `RASCUNHO`; a nova seleção exige Cliente ativo. Depois que V1 deixar `RASCUNHO`, o Cliente fica definitivamente congelado para a negociação. Sua inativação posterior não invalida o orçamento nem remove a referência histórica, e uma negociação com outro Cliente exige novo `Orcamento` e novo número comercial.

A máquina de estados de uma versão é:

* `RASCUNHO` pode transicionar para `ENVIADO` ou `CANCELADO`;
* `ENVIADO` pode transicionar para `APROVADO`, `RECUSADO` ou `CANCELADO`;
* `APROVADO`, `RECUSADO` e `CANCELADO` são terminais para aquela versão.

Reenviar o mesmo status pode ser tratado de forma idempotente. Somente a versão atual pode transicionar, uma mudança efetiva exige status de destino ativo e códigos não canônicos não participam do fluxo sem regra explícita.

Somente a versão atual em `RASCUNHO` é editável. Apenas nela podem ser alteradas a observação e as linhas `ItemOrcamento`, `MaterialOrcamento`, `MaoDeObraOrcamento` e `DespesaOrcamento`. Versões em `ENVIADO`, `APROVADO`, `RECUSADO` ou `CANCELADO` ficam congeladas para conteúdo documental e financeiro; uma versão enviada nunca deve voltar a `RASCUNHO` para receber correções.

Alterações posteriores ao envio exigem criação explícita de nova versão. Ela somente pode partir da versão atual em `ENVIADO` ou `RECUSADO`, começa em `RASCUNHO` e passa a ser `versaoAtual`; a origem permanece intacta. Não crie ramificações de versões históricas. Um `Orcamento` pode possuir no máximo uma versão `APROVADO`, com garantia também no PostgreSQL, e nenhuma nova versão comercial pode ser criada após a aprovação. Aditivos e renegociações posteriores são outro processo de domínio, não mutação ou versionamento silencioso do documento aprovado.

A criação de nova versão e sua clonagem são uma única operação transacional. A cópia inclui observação e as quatro categorias de linha, gera novos IDs e datas de criação e preserva diretamente referências de catálogo, snapshots e valores persistidos da origem. Não consulte os cadastros atuais para reconstruir snapshots nem recalcule valores durante a clonagem. Referências inativas podem ser preservadas nessa cópia histórica; exigências de cadastro ativo continuam aplicáveis a inclusões manuais e a trocas efetivas de referência.

Para negociações existentes, toda escrita deve bloquear primeiro `Orcamento` e respeitar a ordem `Orcamento -> OrcamentoVersao -> linha`. A geração de `numeroVersao` ocorre sob o lock da raiz, a partir da versão atual, e deve possuir unicidade protegida também pelo PostgreSQL; não use `MAX(numeroVersao) + 1`.

`ItemOrcamento`, `MaterialOrcamento`, `MaoDeObraOrcamento` e `DespesaOrcamento` são linhas históricas e documentais pertencentes a `OrcamentoVersao`, não diretamente a `Orcamento` nem cadastros independentes. Operações devem ser delimitadas pelo contexto `Orcamento -> OrcamentoVersao -> linha`; uma versão de outro orçamento ou uma linha de outra versão deve ser tratada como inexistente naquele contexto. Rotas de linha devem receber explicitamente `orcamentoId` e `versaoId`, sem escrita implícita que ignore a versão.

As linhas não possuem campo `ativo`; sua remoção é física, permitida somente na versão atual em `RASCUNHO` e restrita à própria linha, preservando a raiz, a versão, os catálogos e as demais entidades. O mesmo `Servico`, `Material` ou `UnidadeMaoDeObra` pode aparecer múltiplas vezes na mesma versão, pois cada linha pode representar contexto diferente. Despesas com descrições iguais ou semelhantes também podem coexistir; não estabeleça unicidade nesses casos.

Cadastros representam o estado atual do catálogo; linhas de versão representam o que foi negociado em um momento específico. Alterações posteriores em Serviço, Material, Fornecedor ou UnidadeMaoDeObra não modificam conteúdo financeiro, descritivo ou snapshots das versões existentes. A versão aprovada deve preservar exatamente o documento comercial e financeiro aceito pelo Cliente.

`ItemOrcamento` preserva os dados comerciais negociados; `MaterialOrcamento` preserva descrição, unidade, quantidade, `custoUnitario` e `custoTotal`; `MaoDeObraOrcamento` preserva descrição, unidade, quantidade, `custoUnitario` e `custoTotal`. Nunca recalcule versão histórica usando dados ou preços atuais do catálogo.

O `custoUnitario` de `MaterialOrcamento` é o custo previsto adotado naquela versão e não depende automaticamente de `MaterialFornecedor`. Alterações posteriores no Material ou nos preços de `MaterialFornecedor` não modificam linhas históricas existentes. A unidade de `MaterialOrcamento` é snapshot de `Material.unidade` e não é editada independentemente do Material; uma troca efetiva de Material atualiza o snapshot a partir do novo cadastro, sem afetar versões ou linhas anteriores.

Os agregados são independentes por `OrcamentoVersao` e nunca podem misturar linhas de versões diferentes:

* `totalComercial = SUM(ItemOrcamento.valorTotal)`;
* `custoTotalMateriais = SUM(MaterialOrcamento.custoTotal)`;
* `custoTotalMaoDeObra = SUM(MaoDeObraOrcamento.custoTotal)`;
* `custoTotalDespesas = SUM(DespesaOrcamento.valor)`;
* `margemPrevista = totalComercial - custoTotalMateriais - custoTotalMaoDeObra - custoTotalDespesas`;
* `percentualMargem = (margemPrevista * 100) / totalComercial`.

Cada categoria participa somente de seu próprio agregado; receita comercial, custos previstos de materiais, mão de obra e despesas permanecem separados. Os agregados, a margem e o percentual são derivados e não devem ser persistidos, sincronizados manualmente nem mantidos por coluna, trigger ou coluna gerada. Versão sem linhas de uma categoria possui `0.00` no respectivo total. Em listagens de versões, obtenha os totais de forma agregada ou em lote, sem executar `SUM` por versão dentro de loops e sem introduzir N+1.

`margemPrevista` representa somente a diferença entre o valor comercial e os custos internos previstos controlados, não lucro ou resultado financeiro completo. Pode ser positiva, zero ou negativa. Quando `totalComercial` for `0.00`, `percentualMargem` será `0.00`, sem divisão, enquanto a margem continuará refletindo os custos e poderá permanecer negativa. Margem e percentual usam `BigDecimal`, duas casas decimais e `RoundingMode.HALF_UP`, reutilizando os quatro totais já resolvidos sem novas consultas ou duplicação das fórmulas internas das linhas.

Use `BigDecimal` para valores monetários, nunca `float` ou `double`. Precisão e escala devem corresponder ao PostgreSQL. Fórmulas importantes ficam centralizadas no Service ou em componente de domínio específico, com definição clara do que é informado, calculado, persistido e recalculável.

Em `ItemOrcamento`, `MaterialOrcamento` e `MaoDeObraOrcamento`, a quantidade admite até quatro casas decimais e os valores monetários usam duas. Em `MaterialOrcamento` e `MaoDeObraOrcamento`, `custoTotal` é calculado como `quantidade * custoUnitario`. Cálculos de totais e arredondamentos pertencem à aplicação, centralizados no Service, com `RoundingMode.HALF_UP` para o valor final. O PostgreSQL protege apenas invariantes simples das colunas e não deve replicar fórmulas ou arredondamentos em constraints, colunas geradas ou outros cálculos persistentes.

`DespesaOrcamento.valor` usa `BigDecimal` com duas casas decimais, aceita zero e não pode ser negativo. Como é um valor informado diretamente, entradas com escala superior à contratada devem ser rejeitadas, sem arredondamento silencioso.

### OrdemServico e execução operacional

O domínio comercial permanece estruturado em `Orcamento -> OrcamentoVersao`; o domínio operacional inicia em `OrdemServico`, raiz independente que referencia a versão comercial aprovada da qual se originou. Sua criação e evolução não substituem, recalculam nem alteram `Orcamento`, Cliente, `OrcamentoVersao`, `StatusOrcamento`, observação comercial, linhas, agregados, `margemPrevista` ou `percentualMargem`. A versão aprovada permanece a fonte histórica exata do que foi aceito comercialmente.

A criação de `OrdemServico` é explícita e contextual ao par `Orcamento -> OrcamentoVersao`. A versão deve pertencer ao orçamento informado, ser a versão atual e possuir `StatusOrcamento.codigo = APROVADO`; uma versão de outro orçamento é inexistente naquele contexto. A regra usa exclusivamente o código funcional, nunca ID ou nome. Versões em `RASCUNHO`, `ENVIADO`, `RECUSADO`, `CANCELADO` ou status adicional sem regra explícita não podem originar uma ordem. Não ofereça criação direta de ordem sem origem comercial explícita nesse modelo.

Cada `OrcamentoVersao` pode originar no máximo uma `OrdemServico`. Essa unicidade e a unicidade do número operacional devem ser garantidas no PostgreSQL, além da validação preventiva no Service. Uma ordem existente não deve ser substituída, reativada nem duplicada para a mesma versão.

`StatusOrdemServico` é cadastro persistente e administrável separado de `StatusOrcamento`, não enum Java. `codigo` é sua identidade funcional obrigatória, única e imutável; regras estruturais usam o código e nunca dependem de ID ou nome. O nome é apresentação, persistido sem espaços externos e único segundo `LOWER(BTRIM(nome))`. A inativação do status é lógica e preserva referências históricas; status inativo não pode ser selecionado em mudança efetiva.

Os códigos estruturais da máquina operacional são `COMPRAR_MATERIAL`, `EM_EXECUCAO`, `INSTALAR` e `CONCLUIDO`. Status adicionais podem existir, mas não participam automaticamente de regras estruturais. Toda ordem nova inicia em `COMPRAR_MATERIAL`, que deve existir e estar ativo; ausência ou inatividade é falha estrutural explícita, sem fallback por ID, nome ou outro status.

A máquina operacional é linear:

* `COMPRAR_MATERIAL` pode transicionar somente para `EM_EXECUCAO`;
* `EM_EXECUCAO` pode transicionar somente para `INSTALAR`;
* `INSTALAR` pode transicionar somente para `CONCLUIDO`;
* `CONCLUIDO` é terminal.

Não pule etapas, não retorne a estados anteriores e não inclua status customizado na máquina sem decisão explícita de domínio. Reenviar o mesmo status pode ser tratado de forma idempotente; mudanças efetivas exigem destino ativo e devem passar por operação específica e política central, não pelo update comum.

A observação da ordem pode ser alterada em `COMPRAR_MATERIAL`, `EM_EXECUCAO` e `INSTALAR`; `CONCLUIDO` a congela. Número, origem e status não são mutáveis pelo update comum. `OrdemServico` não possui `ativo` nem operação de exclusão e deve ser preservada como histórico operacional. O modelo atual não define cancelamento operacional; eventual interrupção exige regra explícita e não deve ser simulada por exclusão física ou lógica.

`OrdemServico` referencia a versão aprovada, sem duplicar preventivamente Cliente, itens, materiais, mão de obra, despesas, totais ou margem. Snapshots operacionais somente devem ser introduzidos quando houver requisito real de domínio.

O número da ordem é uma identidade operacional persistente e única, independente de seu ID técnico, do número comercial do orçamento e de `numeroVersao`. Deve ser gerado pelo PostgreSQL com mecanismo seguro sob concorrência; nunca use `MAX(numero) + 1`. Lacunas são aceitáveis e números consumidos não devem ser reutilizados manualmente.

Na criação contextual, bloqueie na ordem `Orcamento -> OrcamentoVersao` para manter consistente a aprovação que origina a execução. Depois da criação, `OrdemServico` é a raiz das próprias alterações e transições; bloqueie a ordem diretamente e não reacople o fluxo operacional ao comercial. As garantias de uma ordem por versão, número único, código único e imutável de status e nome normalizado devem existir também no PostgreSQL; validações preventivas no Service não substituem constraints.

## 7. Legado e importação de dados

O Microsoft Access é fonte de dados históricos, descoberta de regras e conferência da migração, não referência arquitetural para o PostgreSQL.

Antes de migrar dados legados:

1. identifique os conceitos de negócio presentes;
2. separe dados duplicados, derivados e relacionamentos implícitos;
3. normalize os conceitos no modelo atual;
4. defina a transformação e a rastreabilidade de cada registro.

IDs do Access não devem determinar automaticamente as chaves primárias novas. Quando houver necessidade de rastreabilidade, mantenha correspondência explícita entre origem, ID legado, entidade nova e ID novo, sem contaminar permanentemente o domínio sem justificativa.

Não faça merge destrutivo automático por similaridade textual, acentuação, abreviação ou erro de digitação. Ferramentas podem normalizar para comparação, apontar candidatos e produzir relatórios, mas dados ambíguos não devem ser fundidos ou descartados silenciosamente.

Migração estrutural e importação legada são fluxos distintos. Flyway evolui o schema; importações complexas devem executar separadamente com leitura, normalização, validação, mapeamento, persistência e relatório.

Importação é um caso de uso transacional próprio, não uma variação do CRUD nem referência arquitetural para novos módulos. Acesso direto a Repository dentro de um Service de importação não autoriza ignorar invariantes; regras compartilhadas devem ser extraídas para componentes reutilizáveis em vez de duplicadas.

Importadores, inclusive os baseados em Apache POI, devem confirmar layouts de colunas com o arquivo real do negócio e tratar explicitamente cabeçalho, células e linhas vazias, tipos, datas, duplicidades, limites, registros inválidos, transação, rollback e relatório de erros. Não dependa de dados previamente existentes no banco do desenvolvedor.

## 8. Erros e segurança

O tratamento HTTP é centralizado em `GlobalExceptionHandler` e mantém contrato baseado em `ProblemDetail`.

Use `ResourceNotFoundException` para recursos inexistentes e `BusinessException` para violações de regras de negócio. Falhas de Bean Validation retornam HTTP 400 em `ProblemDetail`, com erros por campo na propriedade `erros`. Mensagens devem ser compreensíveis sem expor stack trace, SQL, caminhos locais, credenciais, configuração sensível ou detalhes internos desnecessários.

Não presuma identidade autenticada enquanto segurança não estiver implementada. Não introduza Spring Security parcialmente em tarefa não relacionada. Uma implementação futura deve definir de forma conjunta autenticação, autorização, papéis, endpoints públicos, proteção da documentação, CORS, CSRF e estratégia de sessão ou token.

## 9. Testes e validação

Toda regra de negócio relevante deve possuir cobertura proporcional ao risco:

* Service: regras, validações, conflitos, cálculos e transições de estado;
* Mapper: conversões relevantes entre Request, Entity e Response;
* Controller: status HTTP, Bean Validation, payloads e erros esperados por testes MVC;
* Repository: consultas customizadas e comportamentos dependentes do PostgreSQL;
* migrations: schema, constraints, relacionamentos e compatibilidade JPA;
* importação: arquivos pequenos e determinísticos com cenários válidos, inválidos, duplicados e rollback quando aplicável.

Siga, quando aplicável, a convenção `<Dominio>ServiceTest`, `<Dominio>MapperTest`, `<Dominio>ControllerTest` e `<Dominio>RepositoryIntegrationTest`. Use integração quando o comportamento depender do PostgreSQL, de transação real ou da interação entre domínios; não crie teste de Repository apenas para repetir comportamento padrão do JPA.

Testes de integração devem usar ambiente isolado e não depender do banco pessoal do desenvolvedor. Execute testes relevantes e a suíte completa quando o ambiente permitir. Se uma validação não puder ser executada, informe o motivo e nunca declare sucesso sem verificação.

## 10. Disciplina de alterações

Antes de editar:

1. leia este arquivo;
2. execute `git status` e preserve alterações existentes;
3. identifique os módulos afetados e leia implementações semelhantes;
4. confira migrations, schema real, relacionamentos, contratos e testes;
5. entenda o significado do domínio antes de implementar.

Não altere arquivos fora do escopo nem faça refatorações amplas apenas porque encontrou dívida técnica. Não execute automaticamente operações destrutivas como `git reset`, `git checkout .`, `git clean`, `DROP`, `TRUNCATE` ou `flyway clean`.

Trate mudanças persistentes como uma decisão conjunta entre schema, migration, Entity, Repository, Service, DTO, Mapper, Controller, testes e dados existentes. Trate mudanças de contrato como uma decisão conjunta entre Request, Response, validação, Mapper, Controller, OpenAPI e testes.

Depois de alterar:

1. revise o `git diff` e confirme o escopo;
2. compile com Java 21;
3. execute os testes proporcionais ao risco;
4. valide a compatibilidade entre Entity e schema quando aplicável;
5. relate objetivamente mudanças, verificações reais, limitações, riscos e dívidas fora do escopo.

## 11. Revisão arquitetural e manutenção deste arquivo

Ao concluir uma tarefa relevante, identifique decisões novas que sejam simultaneamente permanentes, úteis para futuras implementações e não facilmente inferíveis pelo código. Compare-as com este documento e sugira atualização apenas quando esses critérios forem atendidos.

Não registre aqui progresso de sessão, arquivos modificados, migrations recém-aplicadas, resultados ou quantidades de testes, bugs e warnings temporários, configuração momentânea do ambiente, TODOs locais ou detalhes triviais de implementação.

Mantenha este arquivo curto, atual e coerente. Reescreva ou remova regras obsoletas; ele deve descrever como o projeto deve ser desenvolvido agora, não acumular o histórico de sua evolução.
