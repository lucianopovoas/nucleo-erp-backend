# Estado atual do projeto

> Inventário derivado do código no commit base `0b09f04` e dos testes adicionados nesta etapa. “Existente” significa encontrado no código; os resultados executados estão registrados em seção própria.

## Resumo

- 162 arquivos Java de produção;
- 74 classes Java de teste;
- 19 migrations Flyway;
- 18 Controllers;
- 82 operações HTTP;
- 16 pacotes funcionais/de domínio, além de `config` e `exception`;
- diretório `/docs` introduzido por esta documentação.

## Inventário funcional

| Módulo | Implementação encontrada | Testes próprios encontrados |
|---|---|---|
| Cliente | Entity, Repository, Service, Mapper, DTOs, Controller | Service, Mapper, Controller |
| Fornecedor | CRUD completo | Service, Mapper, Controller |
| Material | CRUD completo | Service, Mapper, Controller |
| MaterialFornecedor | vínculo, reativação, constraints e CRUD | Service, Mapper, Controller, Repository integration |
| CategoriaServico | CRUD, unicidade e cascata de inativação | Service, Mapper, Controller, Repository e inativação integration |
| Servico | CRUD e regras de categoria | Service, Mapper, Controller, Repository integration |
| StatusOrcamento | cadastro, código funcional e imutabilidade | Service, Mapper, Controller, Repository e código integration |
| UnidadeMaoDeObra | cadastro e unicidade normalizada | Service, Mapper, Controller, Repository integration |
| Orcamento | criação da raiz/V1, consulta e correção do Cliente | Service unitário, rotas versionadas e cenários integrados de versionamento |
| OrcamentoVersao | guarda, política, clonagem, totais e concorrência | Controller, Mapper, Guard, Totais, Policy e integrações de fluxo, constraint, concorrência, rollback e migration |
| ItemOrcamento | CRUD contextual e cálculo comercial | Controller, Mapper, Service e fluxo integrado de versionamento |
| MaterialOrcamento | CRUD contextual, snapshot e cálculo de custo | Controller, Mapper, Service e fluxo integrado de versionamento |
| MaoDeObraOrcamento | CRUD contextual, snapshot e cálculo de custo | Controller, Mapper, Service e fluxo integrado de versionamento |
| DespesaOrcamento | CRUD contextual e validação de valor | Controller, Mapper, Service e fluxo integrado de versionamento |
| StatusOrdemServico | cadastro persistente separado | Controller, Service, Mapper e integração |
| OrdemServico | criação, filtros, update, status, locks e constraints | Service, Controller, Mapper, Policy, integração HTTP e três integrações de domínio |
| Importacao | leitor Excel e três endpoints/importadores | integração documental de layouts, inválidos, duplicidade e rollback |
| Configuração | CORS e OpenAPI | CORS |
| Erros | handler central com quatro exceções | exercitado parcialmente por Controllers; sem teste dedicado do handler |

Também existem `NucleoErpApplicationTests`, `CadastroDefaultsIntegrationTests` e dois testes específicos de migrations.

## Persistência entregue

- schema gerenciado por Flyway V1–V19;
- `ddl-auto=validate`;
- sequences separadas para número de orçamento e ordem;
- constraints de não negatividade nas colunas monetárias aplicáveis;
- unicidade normalizada de nomes onde explicitamente definida;
- unicidade do vínculo MaterialFornecedor;
- unicidade do número da versão dentro do orçamento;
- uma versão aprovada por orçamento;
- uma OrdemServico por OrcamentoVersao;
- triggers de imutabilidade dos códigos de status;
- índices para FKs, agregações e filtros operacionais.

O estado real de um banco instalado não foi consultado; não se afirma que V1–V19 estejam aplicadas em qualquer instância específica.

## Integrações e configuração

- PostgreSQL via driver JDBC;
- Flyway;
- Testcontainers com PostgreSQL 16 para testes;
- Apache POI para arquivos Excel;
- Springdoc OpenAPI, com `/swagger` e `/api-docs`;
- CORS externo por `APP_CORS_ALLOWED_ORIGINS`.

Não foram encontrados Spring Security, cache distribuído, filas, storage externo, serviços de e-mail ou conectores com sistemas de terceiros.

## Validação executada em 2026-08-23

- Git estava limpo antes da documentação.
- O Java padrão era 17.0.17, incompatível com o projeto.
- Java 21.0.12 foi localizado e selecionado temporariamente no processo.
- Compilação Maven com testes ignorados: sucesso.
- Docker Desktop 28.1.1 foi iniciado para a validação isolada.
- O Testcontainers iniciou PostgreSQL 16.15 em porta aleatória; Flyway aplicou V1–V19 em banco vazio e o Hibernate validou o schema.
- Suíte completa: 74 classes, 490 testes, 0 falhas, 0 erros e 0 ignorados.
- A execução gerou somente artefatos em `target/`, que já é área de build.
- Foram exercitados os testes de PostgreSQL, concorrência, rollback, constraints e migrations sem usar o banco de desenvolvimento.

## Limitações conhecidas

### Ambiente de testes

- a suíte depende de Docker Engine ativo e de acesso às imagens `postgres:16-alpine` e `testcontainers/ryuk:0.14.0` na primeira execução;
- as imagens permanecem no cache Docker após os containers descartáveis serem removidos;
- o Mockito carrega dinamicamente o agente Byte Buddy no Java 21 e emite aviso de incompatibilidade futura.

### API

- listagens não possuem paginação;
- Cliente, Fornecedor e Material incluem inativos, ao contrário dos demais cadastros operacionais;
- o contrato OpenAPI comercial e de importação é verificado no documento gerado; cadastros legados ainda possuem anotações de erro menos detalhadas;
- não há autenticação nem autorização;
- falhas de integridade não reconhecidas pelo domínio continuam sem tradução específica.

### Importação

- primeira aba apenas;
- layouts definidos por índices numéricos no código;
- nenhuma validação de cabeçalho;
- células são reduzidas a String, incluindo números convertidos para `long`;
- tratamento de linhas vazias/inválidas é desigual entre importadores;
- não há detecção/relatório de duplicidade, rastreabilidade ou resultado por linha;
- duplicidades são persistidas como registros independentes;
- arquivo de zero bytes é traduzido para `400 ProblemDetail`; as demais limitações funcionais da importação permanecem;
- persistência ocorre diretamente pelos Repositories, sem reutilizar explicitamente as regras dos Services CRUD.

### Contratos e schema

- DTOs de Cliente, Fornecedor e Material estão alinhados às nulabilidades, tamanhos e precisões relevantes do schema;
- defaults confirmados, como `ativo`, permanecem opcionais na entrada e não chegam nulos ao estado persistido;
- todas as entidades combinam `@CreationTimestamp` com defaults de timestamp no banco;
- não há handler genérico documentado para conflitos de integridade não reconhecidos.

## Regras críticas sem cobertura específica

Após esta etapa, ainda faltam testes específicos para:

1. Controller e Mapper próprios de `Orcamento`; as rotas principais possuem cobertura integrada, mas não uma matriz MVC unitária equivalente às versões/linhas.
2. Fluxo HTTP integrado de sucesso dos três endpoints de importação; os Services, transações e erros técnicos do Controller estão cobertos separadamente.
3. Falha intermediária na criação inicial do orçamento para demonstrar rollback da raiz quando a persistência da V1 falha; o caminho de sucesso está coberto em integração e a ordem de persistência em teste unitário.
4. Validação exaustiva do documento OpenAPI para os cadastros legados; o núcleo comercial, OS, importação e schemas compartilhados possuem teste integrado.

## Riscos técnicos

- indisponibilidade do Docker ou do registry impede a suíte integrada, embora os testes unitários ainda possam ser executados seletivamente;
- falhas de integridade não traduzidas podem chegar como erro genérico caso escapem das validações e regras já implementadas;
- importação pode persistir dados incompletos ou inconsistentes sem relatório operacional;
- ausência de paginação tende a degradar listagens conforme o volume crescer;
- ausência de autenticação expõe toda a API se ela for publicada em rede;
- alterações futuras em DTO/Controller podem divergir do OpenAPI se não ampliarem o teste integrado do documento gerado;
- carregamento dinâmico do agente Mockito produziu warnings no Java 21 e poderá exigir ajuste em JDKs futuros.

## Antes de ampliar o frontend

Prioridade recomendada:

1. Consolidar o cliente frontend sobre `api.md`, `/api-docs` e `acoesPermitidas`, mantendo o backend como autoridade.
2. Definir se listagens administrativas devem incluir inativos e como o frontend solicita isso.
3. Definir autenticação/autorização antes de qualquer exposição fora de ambiente controlado.
4. Planejar separadamente a revisão funcional da importação e seu relatório por linha.
