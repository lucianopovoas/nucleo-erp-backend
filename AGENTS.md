# AGENTS.md — Núcleo ERP

## 1. Visão geral

Este repositório contém uma API REST monolítica para o ERP da **Núcleo das Reformas**.

O sistema está sendo desenvolvido como substituição de um ERP legado em Microsoft Access. O objetivo da migração não é reproduzir a estrutura técnica do sistema antigo, mas preservar os dados e regras de negócio relevantes em um modelo de domínio mais consistente.

Stack principal:

* Java 21;
* Spring Boot 4.1.0;
* Maven;
* Spring MVC;
* Spring Data JPA;
* PostgreSQL;
* Flyway;
* Bean Validation;
* Lombok;
* Apache POI;
* Springdoc OpenAPI.

Pacote raiz:

```text
br.com.nucleodasreformas.nucleoerp
```

Classe principal:

```text
NucleoErpApplication
```

---

## 2. Ambiente obrigatório

* Use JDK 21.
* O `pom.xml` define `<java.version>21</java.version>`.
* Antes de compilar ou testar, confirme a versão ativa com:

```powershell
java -version
```

* Caso necessário, ajuste `JAVA_HOME`.
* Utilize preferencialmente o Maven Wrapper do repositório:

```powershell
.\mvnw.cmd
```

no Windows, ou:

```bash
./mvnw
```

em ambientes Unix.

O banco padrão de desenvolvimento é PostgreSQL:

```text
jdbc:postgresql://localhost:5432/nucleo_erp
```

Usuário padrão:

```text
postgres
```

A senha deve ser fornecida externamente por variável de ambiente:

```text
DB_PASSWORD
```

Nunca registre senhas, tokens, credenciais ou outros segredos no repositório.

Exemplo:

```powershell
$env:DB_PASSWORD = '<senha-local>'
.\mvnw.cmd test
.\mvnw.cmd spring-boot:run
```

Swagger UI:

```text
/swagger
```

OpenAPI:

```text
/api-docs
```

---

## 3. Princípios arquiteturais

O projeto é organizado por domínio/funcionalidade.

Estrutura padrão:

```text
<dominio>/
  controller/
  dto/
  entity/
  mapper/
  repository/
  service/
```

Fluxo esperado para uma operação HTTP:

```text
HTTP
  ↓
Controller
  ↓
Service
  ↓
Mapper / Repository
  ↓
PostgreSQL
```

### Controller

Responsabilidades:

* receber requisições HTTP;
* aplicar `@Valid`;
* trabalhar exclusivamente com DTOs na fronteira HTTP;
* delegar regras de negócio ao Service;
* retornar o status HTTP adequado.

Não deve:

* conter regra de negócio;
* acessar Repository diretamente;
* expor Entity diretamente;
* controlar manualmente transações.

Controllers devem permanecer finos.

### Service

O Service é a principal fronteira de:

* regra de negócio;
* validação entre entidades;
* coordenação entre repositories;
* transação.

Utilize:

```java
@Transactional(readOnly = true)
```

para operações exclusivamente de leitura.

Operações que alteram estado devem executar dentro de uma transação de escrita.

Não distribua uma mesma regra de negócio entre Controller, Mapper e Repository.

### Repository

Repositories devem:

* estender `JpaRepository`;
* concentrar acesso e consultas de persistência;
* possuir somente consultas necessárias ao domínio.

Não implemente regra de negócio no Repository.

### Mapper

Mappers são:

* manuais;
* estáticos;
* responsáveis apenas pela conversão entre DTOs e entidades.

Exemplos:

```text
Request -> Entity
Entity -> Response
```

Não coloque:

* consulta ao banco;
* regra de negócio;
* cálculo financeiro;
* validação dependente de outras entidades;

dentro de Mapper.

### Injeção de dependências

Use injeção por construtor.

Dependências devem ser `final`.

O padrão preferencial do projeto é:

```java
@RequiredArgsConstructor
```

Evite:

```java
@Autowired
```

em campos.

### Lazy loading

`spring.jpa.open-in-view` permanece desabilitado.

Portanto:

* carregue relacionamentos necessários dentro da fronteira transacional;
* converta Entities em DTOs enquanto os dados necessários ainda estiverem disponíveis;
* não dependa de lazy loading durante serialização HTTP.

---

## 4. Módulos e evolução do domínio

Módulos atualmente existentes devem ser identificados pelo código-fonte antes de qualquer alteração.

Domínios conhecidos do ERP incluem, entre outros:

```text
cliente
fornecedor
material
categoria_servico
servico
status_orcamento
orcamento
item_orcamento
material_orcamento
funcionario
mao_obra
despesa
importacao
```

A existência de tabela ou migration SQL não significa que o respectivo módulo Java esteja implementado.

Antes de modificar um domínio:

1. inspecione sua Entity;
2. seus DTOs;
3. Service;
4. Repository;
5. Mapper;
6. Controller;
7. migrations relacionadas;
8. testes existentes.

Não presuma implementação com base apenas no schema.

---

## 5. Convenções de código

Use nomes de domínio em português.

Exemplos:

```text
Cliente
Fornecedor
Material
Servico
Orcamento
```

Use sufixos técnicos em inglês:

```text
Controller
Service
Repository
Request
Response
Mapper
```

Exemplos:

```text
ClienteController
ClienteService
ClienteRepository
ClienteRequest
ClienteResponse
ClienteMapper
```

Métodos CRUD seguem preferencialmente:

```text
salvar
buscarPorId
listar
atualizar
deletar
```

Rotas HTTP:

* minúsculas;
* em português;
* no plural;
* sem prefixo global `/api`, salvo decisão arquitetural posterior explícita.

Exemplos:

```text
/clientes
/fornecedores
/materiais
/categorias-servico
/orcamentos
```

Tabelas PostgreSQL utilizam:

* português;
* singular;
* `snake_case`.

Exemplos:

```text
cliente
fornecedor
material
categoria_servico
item_orcamento
```

---

## 6. DTOs e contratos HTTP

Controllers não devem receber nem retornar Entity diretamente.

Use DTOs específicos de entrada e saída:

```text
<Dominio>Request
<Dominio>Response
```

Requests devem conter Bean Validation compatível com o schema e com as regras do negócio.

Responses devem representar integralmente o contrato público da API.

Quando fizerem parte do contrato, não omita campos como:

```text
id
criadoEm
ativo
```

Mudanças de contrato devem considerar em conjunto:

* Request;
* Response;
* validação;
* Mapper;
* Controller;
* OpenAPI;
* testes.

---

## 7. Banco de dados como contrato persistente

Hibernate utiliza:

```text
ddl-auto: validate
```

Portanto o Hibernate não deve criar ou alterar automaticamente o schema.

Toda alteração estrutural permanente deve ser realizada por Flyway.

A Entity JPA deve permanecer compatível com o schema PostgreSQL real.

Ao criar ou alterar campos, valide:

* nome da coluna;
* tipo SQL;
* nullable;
* tamanho;
* precisão;
* escala;
* default;
* índice;
* chave estrangeira;
* constraint de unicidade.

Não invente requisitos de validação apenas por conveniência do código Java.

Exemplo:

```sql
nome VARCHAR(200) NOT NULL
```

deve ser refletido apropriadamente na Entity e no Request.

---

## 8. Booleans e nulabilidade

Não use `@NotNull` em `boolean` primitivo para detectar ausência de propriedade JSON.

Quando os estados:

```text
ausente
false
true
```

precisarem ser distinguíveis, utilize:

```java
Boolean
```

Quando o domínio possuir apenas dois estados e houver default controlado internamente, avalie o uso de `boolean`.

A decisão deve estar alinhada ao contrato HTTP e ao schema.

---

## 9. Exclusão lógica

Quando uma entidade possuir o campo:

```text
ativo
```

a exclusão padrão do ERP é lógica:

```text
ativo = false
```

O método:

```text
deletar
```

não deve executar `DELETE` físico nesses casos.

Não implemente exclusão lógica em uma entidade que não possua suporte adequado no schema sem criar antes a migration correspondente.

Consultas devem decidir explicitamente se registros inativos fazem parte do resultado.

Não assuma automaticamente que:

```java
findAll()
```

é adequado para endpoints públicos.

Para novos módulos, prefira que listagens operacionais retornem somente registros ativos, salvo quando o caso de uso exigir histórico ou administração de registros inativos.

A decisão deve ser explícita.

---

## 10. Auditoria

Campos de criação devem possuir uma única estratégia de preenchimento.

Evite combinar mecanismos redundantes como:

```text
@CreationTimestamp
@PrePersist
DEFAULT CURRENT_TIMESTAMP
preenchimento manual no Service
```

sem necessidade.

Antes de implementar auditoria em novo módulo, observe o schema e escolha uma estratégia única e consistente.

O banco pode possuir `DEFAULT CURRENT_TIMESTAMP` como proteção estrutural, mas o código Java não deve duplicar desnecessariamente múltiplas estratégias de geração do mesmo valor.

---

## 11. Unicidade e concorrência

Validação de unicidade no Service melhora a mensagem de erro, mas não substitui garantia no banco.

Quando uma regra for realmente única, considere:

```text
constraint UNIQUE no PostgreSQL
+
validação amigável no Service
```

Nunca dependa exclusivamente de:

```java
existsBy...
```

como garantia de consistência, pois requisições concorrentes podem ultrapassar essa validação.

Em atualização, qualquer consulta de unicidade deve ignorar o próprio registro.

Exemplo:

```java
existsByNomeAndIdNot(nome, id)
```

quando aplicável.

---

## 12. Materiais e fornecedores

`Material` e `Fornecedor` são entidades independentes.

Um fornecedor pode fornecer múltiplos materiais.

Um material pode ser oferecido por múltiplos fornecedores.

Não coloque no cadastro principal de `Material` informações que pertencem exclusivamente à relação com um fornecedor.

Não coloque no cadastro principal de `Fornecedor` uma coleção desnormalizada de materiais, preços ou colunas numeradas.

Quando a relação Material × Fornecedor possuir atributos próprios, modele-a através de uma entidade associativa.

Exemplo conceitual:

```text
Material
    |
    | N
    |
MaterialFornecedor
    |
    | N
    |
Fornecedor
```

A entidade associativa pode futuramente armazenar informações próprias da oferta, como:

```text
precoCompra
unidadeCompra
codigoFornecedor
ativo
```

Somente implemente atributos cuja necessidade esteja confirmada pelo domínio ou pelos dados reais.

Evite `@ManyToMany` simples quando a relação possuir atributos próprios.

- Para cada combinação de Material e Fornecedor deve existir no máximo um vínculo,
  independentemente de estar ativo ou inativo.

- Ao cadastrar novamente uma combinação Material + Fornecedor já existente e
  inativa, reative o vínculo existente em vez de criar outro registro, preservando
  sua identidade.

- Criação, reativação e manutenção de um vínculo MaterialFornecedor exigem que
  Material e Fornecedor estejam ativos. A inativação de uma entidade-base não
  deve apagar nem inativar automaticamente vínculos históricos existentes.

- Atributos específicos da relação, como unidade de compra ou histórico de preços,
  só devem ser adicionados quando houver uma regra de negócio comprovada; não
  duplique informações do cadastro de Material preventivamente.
---

## 13. Orçamentos e histórico comercial

Cadastros representam o estado atual do catálogo.

Itens de orçamento representam aquilo que foi negociado em determinado momento.

Portanto, mudanças posteriores em:

```text
Servico
Material
Fornecedor
```

não devem alterar retroativamente o conteúdo financeiro ou descritivo de um orçamento já existente.

`item_orcamento` deve preservar os dados necessários para reconstruir historicamente a negociação.

Quando necessário, isso inclui snapshot de:

```text
descricao
quantidade
valorUnitario
desconto
valorTotal
```

mesmo existindo relacionamento com `servico`.

Da mesma forma, `material_orcamento` deve preservar os valores utilizados naquele orçamento e não depender do preço atual do cadastro de material ou fornecedor.

Nunca recalcule automaticamente um orçamento histórico utilizando preços atuais de catálogo.

---

## 14. Cálculos monetários

Valores monetários devem utilizar:

```java
BigDecimal
```

Não utilize:

```java
float
double
```

para valores financeiros.

Precisão e escala devem permanecer alinhadas ao PostgreSQL.

Cálculos importantes do domínio devem ficar centralizados no Service ou em componentes de domínio específicos.

Não replique a mesma fórmula em:

* Controller;
* Mapper;
* importador;
* diferentes Services.

Quando existir subtotal ou total derivado, defina claramente qual valor é:

* informado pelo usuário;
* calculado pelo sistema;
* persistido;
* recalculável.

---

## 15. Migração do Microsoft Access

O Microsoft Access legado é:

* fonte de dados históricos;
* fonte para descoberta de regras de negócio;
* referência para conferência da migração.

Ele **não é referência arquitetural para o novo banco**.

Não replique automaticamente uma tabela do Access no PostgreSQL.

Antes de migrar uma tabela legada:

1. identifique quais conceitos de negócio ela contém;
2. identifique dados duplicados ou derivados;
3. identifique relacionamentos implícitos;
4. normalize esses conceitos no modelo atual;
5. defina como cada registro legado será transformado.

Uma tabela legada pode conter dados pertencentes a várias entidades novas.

Da mesma forma, uma entidade nova pode ser alimentada por várias tabelas legadas.

Exemplo conceitual:

```text
Tabela Access misturada
    ↓
análise semântica
    ↓
Fornecedor
Material
MaterialFornecedor
```

Nunca mantenha uma estrutura ruim apenas porque ela existe no legado.

---

## 16. Identificadores legados

IDs do Access não devem definir automaticamente os IDs primários das novas entidades.

As novas entidades utilizam sua própria identidade no PostgreSQL.

Quando for necessário preservar rastreabilidade entre os sistemas, mantenha uma correspondência explícita entre:

```text
origem
idLegado
entidadeNova
idNovo
```

Esse mapeamento pode ser temporário durante a migração ou persistente caso exista necessidade real de auditoria.

Não contamine o modelo de domínio permanentemente com campos legados sem uma necessidade comprovada.

---

## 17. Deduplicação de dados legados

Dados vindos do Access podem possuir:

* erros de digitação;
* diferenças de acentuação;
* abreviações;
* nomes duplicados;
* registros parcialmente duplicados.

Não faça merge destrutivo automático apenas por similaridade textual.

Exemplos como:

```text
CASA TABUAO
CASA TABUÃO
CASA TABUAO *
```

podem ser candidatos à mesma entidade, mas devem ser tratados como possíveis duplicatas até haver confirmação suficiente.

Ferramentas de importação podem:

* normalizar para comparação;
* detectar candidatos;
* produzir relatório;
* sugerir agrupamentos.

Não devem silenciosamente excluir ou fundir dados ambíguos.

---

## 18. Flyway e migração de dados

Flyway é responsável pela evolução estrutural do banco permanente.

Exemplos:

```text
CREATE TABLE
ALTER TABLE
CREATE INDEX
ADD CONSTRAINT
```

Novas migrations devem seguir:

```text
V<versao>__<descricao>.sql
```

Exemplo:

```text
V7__criar_material_fornecedor.sql
```

Nunca:

* reutilize versão aplicada;
* renomeie migration aplicada;
* altere migration aplicada;
* remova migration aplicada;

sem verificar primeiro:

```text
flyway_schema_history
```

e sem autorização explícita.

Não execute:

```text
flyway clean
```

em banco compartilhado ou que contenha dados relevantes.

### Dados legados

Migração estrutural e migração de dados legados são problemas diferentes.

Não coloque importações complexas do Access dentro de migrations Flyway apenas para aproveitar o mecanismo de migration.

Prefira fluxo separado:

```text
dados legados
    ↓
leitura
    ↓
normalização
    ↓
validação
    ↓
mapeamento
    ↓
persistência
    ↓
relatório de importação
```

---

## 19. Importação de planilhas

O projeto utiliza Apache POI.

Os importadores existentes podem depender de posições fixas de coluna.

Antes de alterar um layout:

* confirme o arquivo utilizado pelo negócio;
* não presuma que o nome da planilha corresponde perfeitamente à entidade;
* preserve compatibilidade quando necessário.

Toda importação deve considerar explicitamente:

* cabeçalho;
* linhas vazias;
* células vazias;
* células numéricas;
* datas;
* strings;
* duplicidades;
* tamanho máximo de arquivo;
* registros inválidos;
* comportamento transacional;
* política de rollback;
* relatório de erros.

Importadores não devem ignorar regras fundamentais de consistência do domínio.

Sempre que possível, reutilize componentes de validação e regras de negócio em vez de duplicá-las.

---

## 20. Autenticação e autorização

Não presuma que exista identidade de usuário autenticado se Spring Security ainda não estiver implementado.

Antes de adicionar segurança ao sistema, defina explicitamente:

* modelo de usuário;
* autenticação;
* autorização;
* papéis/perfis;
* endpoints públicos;
* proteção da documentação;
* política CORS;
* política CSRF;
* estratégia de sessão ou token.

Não introduza Spring Security parcialmente em uma tarefa não relacionada sem autorização.

---

## 21. Exceções e contratos de erro

Tratamento HTTP é centralizado em:

```text
GlobalExceptionHandler
```

O formato padrão deve continuar baseado em:

```text
ProblemDetail
```

Utilize:

```text
ResourceNotFoundException
```

para recursos inexistentes.

Utilize:

```text
BusinessException
```

para violações de regras de negócio quando apropriado.

Ao ampliar tratamento de erros:

* mantenha formato consistente;
* forneça mensagens compreensíveis ao consumidor da API;
* não exponha implementação interna.

Nunca exponha:

* stack trace;
* SQL;
* senha;
* token;
* caminho local;
* configuração sensível;
* detalhes internos desnecessários.

---

## 22. Testes

Toda nova regra de negócio relevante deve possuir cobertura adequada.

### Service

Priorize testes unitários para:

* regras de negócio;
* validações;
* cálculos;
* conflitos;
* transições de estado.

### Mapper

Teste conversões relevantes entre:

```text
Request
Entity
Response
```

### Controller

Use testes MVC para validar:

* status HTTP;
* Bean Validation;
* payload de entrada;
* payload de saída;
* erros esperados.

### Repository

Teste consultas customizadas relevantes e regras dependentes do banco.

### Migrations

Quando o schema mudar, valide:

* criação;
* constraints;
* relacionamentos;
* compatibilidade JPA.

### Importação

Use arquivos pequenos e determinísticos cobrindo:

* cenário válido;
* linha inválida;
* duplicidade;
* célula ausente;
* tipo inesperado;
* rollback quando aplicável.

Testes não devem depender de registros previamente existentes no banco do desenvolvedor.

Prefira ambiente de banco isolado para testes de integração.

Se o ambiente impedir a execução dos testes, informe claramente o motivo em vez de presumir sucesso.

---

## 23. Disciplina para alterações

Antes de editar:

```text
git status
```

Inspecione alterações existentes e preserve trabalho do usuário.

Não altere arquivos fora do escopo solicitado.

Não execute automaticamente:

```text
git reset
git checkout .
git clean
DROP DATABASE
DROP TABLE
TRUNCATE
flyway clean
```

ou qualquer outra operação destrutiva.

Mudanças persistentes devem considerar como uma única decisão:

```text
schema
migration
Entity
Repository
Service
DTO
Mapper
Controller
testes
dados existentes
```

Mudanças de contrato devem considerar em conjunto:

```text
Request
Response
Validation
Mapper
Controller
OpenAPI
Testes
```

Não faça refatoração ampla ou correção de dívida técnica fora do escopo apenas porque ela foi encontrada.

Relate-a ao final.

---

## 24. Processo esperado antes de implementar

Para qualquer alteração relevante:

1. leia este `AGENTS.md`;
2. execute `git status`;
3. identifique os módulos afetados;
4. leia implementações semelhantes já existentes;
5. localize migrations relacionadas;
6. entenda o schema real;
7. identifique relacionamentos e impactos;
8. somente então implemente.

Não crie código baseado exclusivamente no nome de uma tabela, classe ou arquivo.

Para funcionalidades originadas do Access, entenda primeiro o significado dos dados.

---

## 25. Processo esperado depois de implementar

Após uma alteração:

1. revise `git diff`;
2. verifique se somente arquivos necessários foram modificados;
3. compile com Java 21;
4. execute testes relevantes;
5. execute a suíte completa quando o ambiente permitir;
6. verifique compatibilidade Entity × schema;
7. relate qualquer limitação.

O relatório final deve informar objetivamente:

* arquivos criados;
* arquivos alterados;
* migrations criadas;
* endpoints criados ou alterados;
* principais regras implementadas;
* testes criados;
* testes executados;
* resultado dos testes;
* validações que não puderam ser executadas;
* riscos encontrados;
* dívida técnica fora do escopo.

Não declare que algo funciona se não tiver sido verificado.

---

## 26. Revisão arquitetural ao final de uma sessão

Ao concluir uma tarefa relevante, revise as decisões tomadas durante a sessão.

Identifique somente decisões que devam continuar válidas em sessões futuras, como:

* decisões arquiteturais;
* convenções;
* regras permanentes de domínio;
* restrições importantes;
* decisões sobre persistência;
* decisões sobre integração com o legado.

Compare essas decisões com este `AGENTS.md`.

Sugira atualização somente quando existir informação nova que seja:

```text
permanente
+
relevante para futuras implementações
+
não facilmente inferível pelo código
```

Não registre no `AGENTS.md`:

* progresso da tarefa;
* arquivos modificados naquela sessão;
* bugs temporários;
* falhas de compilação momentâneas;
* resultado de execução de testes;
* detalhes de implementação triviais;
* TODOs locais;
* problemas que possam ser inferidos diretamente pelo código.

Se nenhuma decisão permanente nova tiver sido tomada, informe que o `AGENTS.md` não precisa ser alterado.

---

## 27. Manutenção deste arquivo

Este documento representa contexto permanente e decisões arquiteturais do projeto.

Mantenha-o curto o suficiente para continuar sendo útil ao agente.

Quando uma regra ficar obsoleta:

* atualize-a;
* substitua-a;
* ou remova-a.

Não acumule histórico de decisões antigas.

O `AGENTS.md` deve descrever **como o projeto deve ser desenvolvido agora**, e não como ele evoluiu ao longo do tempo.
