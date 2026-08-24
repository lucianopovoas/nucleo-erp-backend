# Arquitetura do backend

> Inventário derivado do código, das migrations e dos testes no commit `0b09f04`. O `AGENTS.md` continua sendo a fonte das regras de desenvolvimento; este documento descreve a implementação encontrada.

## Visão geral

O Núcleo ERP é uma API REST monolítica organizada por domínio. A aplicação usa Java 21, Spring Boot 4.1.0, Spring MVC, Spring Data JPA, PostgreSQL, Flyway, Bean Validation, Lombok, Apache POI e Springdoc OpenAPI.

O fluxo predominante é:

```text
HTTP -> Controller -> Service -> Mapper / Repository -> PostgreSQL
```

Não há frontend, autenticação, autorização, mensageria ou integração externa de negócio neste repositório.

## Responsabilidades das camadas

- **Controller:** declara a rota, recebe path/query/body/multipart, aplica `@Valid`, delega ao Service e determina o status HTTP. A fronteira HTTP usa DTOs.
- **Service:** concentra transações, regras de negócio, cálculos, locks e coordenação entre repositories. As classes usam `@Transactional` no nível do Service e `readOnly = true` nas leituras.
- **Mapper:** conversão manual e estática entre DTOs e entidades. Não consulta o banco.
- **Repository:** interfaces Spring Data `JpaRepository`; algumas também usam `JpaSpecificationExecutor`, projections, consultas JPQL/nativas e locks pessimistas.
- **Entity:** mapeamento JPA do schema criado pelo Flyway. Relacionamentos relevantes são `LAZY`.
- **DTO:** contratos separados de entrada, atualização, resumo e resposta.

`spring.jpa.open-in-view=false`; relações necessárias para DTOs são acessadas dentro da transação. `spring.jpa.hibernate.ddl-auto=validate`; o Hibernate valida o schema e não deve criá-lo.

## Módulos

### Cadastros

- `cliente`, `fornecedor` e `material` possuem CRUD e exclusão lógica.
- `categoria_servico` e `servico` formam o catálogo de serviços.
- `material_fornecedor` representa a oferta de um material por um fornecedor.
- `unidade_mao_de_obra` mantém as unidades selecionáveis nas linhas de mão de obra.
- `status_orcamento` e `status_ordem_servico` são catálogos persistentes e separados.

### Negociação comercial

- `orcamento` é a raiz, com número comercial, Cliente e referência explícita à versão atual.
- `orcamento_versao` representa cada documento comercial e contém status, observação e totais derivados.
- `item_orcamento`, `material_orcamento`, `mao_de_obra_orcamento` e `despesa_orcamento` pertencem a uma versão específica.

### Execução operacional

- `ordem_servico` é uma raiz operacional criada explicitamente a partir da versão atual aprovada.
- A ordem referencia a versão de origem sem copiar preventivamente Cliente, linhas, totais ou margem.

### Infraestrutura

- `importacao` lê a primeira aba de arquivos Excel e persiste clientes, fornecedores ou materiais.
- `config` contém CORS e metadados OpenAPI.
- `exception` centraliza `ProblemDetail` para erros de negócio, validação, recursos inexistentes e falhas HTTP técnicas conhecidas, como JSON, parâmetros, multipart, media type e método inválidos.

## Relações principais

```text
Cliente ----------------------> Orcamento
                                    |
                                    +-- versaoAtual
                                    v
StatusOrcamento -------------> OrcamentoVersao
                                    |
               +--------------------+--------------------+
               |                    |                    |
               v                    v                    v
        ItemOrcamento       MaterialOrcamento    MaoDeObraOrcamento
               |                    |                    |
            Servico              Material         UnidadeMaoDeObra
               |
        CategoriaServico

OrcamentoVersao ------------> DespesaOrcamento
OrcamentoVersao aprovada ----> OrdemServico <---- StatusOrdemServico

Material ----> MaterialFornecedor <---- Fornecedor
```

## Persistência e migrations

O datasource padrão é `jdbc:postgresql://localhost:5432/nucleo_erp`, usuário `postgres`, com senha externa em `DB_PASSWORD`.

Nos testes, o datasource é substituído por uma URL JDBC do Testcontainers. A suíte inicia PostgreSQL 16 em container descartável, aplica as migrations com Flyway e mantém `ddl-auto=validate`, sem acessar o banco padrão de desenvolvimento.

Há 19 migrations imutáveis no histórico atual:

- V1–V5: Cliente, Fornecedor, Material e MaterialFornecedor;
- V6–V8: CategoriaServico, Servico e StatusOrcamento;
- V9–V14: orçamento original e quatro categorias de linha;
- V15: código funcional de StatusOrcamento;
- V16–V17: introdução de OrcamentoVersao, migração das linhas e contração do modelo antigo;
- V18: StatusOrdemServico e OrdemServico;
- V19: índices de filtros operacionais.

As identidades comerciais e operacionais usam sequences PostgreSQL independentes. Constraints e índices protegem, entre outros pontos, números únicos, códigos únicos e imutáveis, nomes normalizados, vínculos únicos, uma versão aprovada por orçamento e uma ordem por versão.

## OpenAPI e erros

O Springdoc disponibiliza:

- Swagger UI: `/swagger`;
- documento OpenAPI: `/api-docs`.

`GlobalExceptionHandler` trata explicitamente `ResourceNotFoundException`, `BusinessException`, `IOException` e `MethodArgumentNotValidException`. O contrato detalhado está em [api.md](api.md).

## CORS

A política é centralizada em `CorsConfig` e cobre `/**` somente quando `APP_CORS_ALLOWED_ORIGINS` contém origens explícitas separadas por vírgula.

- origens vazias: nenhuma configuração cross-origin é registrada;
- qualquer origem contendo `*`: a aplicação falha ao construir a configuração;
- métodos: `GET`, `POST`, `PUT`, `DELETE`, `OPTIONS`;
- headers: `Content-Type`, `Accept`;
- credentials: desabilitadas.

CORS não representa autenticação. Não há Spring Security no projeto.

## Fluxo geral

1. Cadastros ativos fornecem referências para a negociação.
2. Criar um orçamento cria a raiz e a V1 em `RASCUNHO` na mesma transação.
3. A versão atual em `RASCUNHO` recebe observação e linhas.
4. A versão transiciona pelo fluxo comercial; após envio, seu conteúdo fica congelado.
5. Correções posteriores exigem clonagem explícita para uma nova versão em `RASCUNHO`.
6. A versão atual `APROVADO` pode originar uma única OrdemServico.
7. A ordem segue sua máquina operacional sem modificar o documento comercial aprovado.

## Divergências e decisões pendentes

- Cliente, Fornecedor e Material listam todos os registros, enquanto os demais cadastros operacionais com `ativo` listam apenas ativos.
- os DTOs de Cliente, Fornecedor e Material foram alinhados à nulabilidade, aos tamanhos e à precisão relevantes do schema; defaults internos confirmados continuam opcionais na entrada.
- Entidades usam `@CreationTimestamp` e as tabelas também usam `DEFAULT CURRENT_TIMESTAMP`; a estratégia duplicada precisa ser confirmada como intencional.
- Não existe handler genérico: exceções inesperadas continuam sob o tratamento padrão do Spring, evitando transformar falhas desconhecidas em contratos silenciosos.
