# Contrato atual da API

> Inventário dos Controllers, DTOs, Services e do `GlobalExceptionHandler` no commit base `0b09f04`. Não há prefixo global `/api`. A suíte completa foi executada com PostgreSQL descartável; isso não substitui testes MVC específicos dos endpoints apontados como lacuna em [estado-projeto.md](estado-projeto.md).

## Convenções

- JSON é o formato de request/response, exceto importações multipart.
- `POST` de criação retorna `201 Created`, salvo importações, que retornam `200 OK` sem body.
- `GET` e `PUT` retornam `200 OK`.
- `DELETE` retorna `204 No Content`.
- Coleções são arrays sem paginação.
- IDs de path são `Long`; `numeroVersao` é `Integer`.
- Datas e horas de resposta são `LocalDateTime` no formato Jackson padrão; filtros de data usam ISO `yyyy-MM-dd`.

## Capacidades informadas pelo backend

As respostas completas de `OrcamentoVersao` e `OrdemServico` incluem `acoesPermitidas`. O campo é informativo e calculado no `Service` pelas mesmas políticas que validam as operações. Ele não concede autorização nem substitui a validação transacional do backend.

Para uma versão em `RASCUNHO` atual, por exemplo:

```json
{
  "acoesPermitidas": {
    "editarConteudo": true,
    "criarNovaVersao": false,
    "alterarStatusPara": [
      { "id": 2, "codigo": "ENVIADO", "nome": "Enviado" },
      { "id": 5, "codigo": "CANCELADO", "nome": "Cancelado" }
    ]
  }
}
```

Para Ordem de Serviço:

```json
{
  "acoesPermitidas": {
    "editarObservacao": true,
    "alterarStatusPara": [
      { "id": 2, "codigo": "EM_EXECUCAO", "nome": "Em execução" }
    ]
  }
}
```

`alterarStatusPara` contém somente destinos simultaneamente permitidos pela máquina atual e ativos no cadastro. Versões históricas e estados terminais retornam listas vazias e capacidades falsas. Reenvio idempotente do status atual continua aceito pelo Service, mas não é apresentado como transição. Uma operação pode deixar de ser válida entre a leitura e a escrita; o frontend deve sempre tratar eventual `ProblemDetail` de negócio.

## Endpoints de cadastro

Cada recurso desta tabela expõe cinco operações:

| Método | Rota | Entrada | Saída |
|---|---|---|---|
| POST | `{base}` | request JSON | response JSON, `201` |
| GET | `{base}` | — | array de responses, `200` |
| GET | `{base}/{id}` | path `id` | response JSON, `200` |
| PUT | `{base}/{id}` | path `id` + request JSON | response JSON, `200` |
| DELETE | `{base}/{id}` | path `id` | sem body, `204` |

| Base | POST/PUT | Response | Listagem |
|---|---|---|---|
| `/clientes` | `ClienteRequest` | `ClienteResponse` | inclui ativos e inativos |
| `/fornecedores` | `FornecedorRequest` | `FornecedorResponse` | inclui ativos e inativos |
| `/materiais` | `MaterialRequest` | `MaterialResponse` | inclui ativos e inativos |
| `/categorias-servico` | `CategoriaServicoRequest` | `CategoriaServicoResponse` | somente ativos |
| `/servicos` | `ServicoRequest` | `ServicoResponse` | somente ativos |
| `/materiais-fornecedores` | `MaterialFornecedorRequest` | `MaterialFornecedorResponse` | somente ativos |
| `/unidades-mao-de-obra` | `UnidadeMaoDeObraRequest` | `UnidadeMaoDeObraResponse` | somente ativos |
| `/status-orcamentos` | POST `StatusOrcamentoRequest`; PUT `StatusOrcamentoUpdateRequest` | `StatusOrcamentoResponse` | somente ativos |
| `/status-ordens-servico` | POST `StatusOrdemServicoRequest`; PUT `StatusOrdemServicoUpdateRequest` | `StatusOrdemServicoResponse` | somente ativos |

DELETE é lógico nesses recursos. GET por ID pode retornar registro inativo.

## Orçamentos

| Método | Rota | Parâmetros/body | Response | Status |
|---|---|---|---|---|
| POST | `/orcamentos` | `OrcamentoRequest` | `OrcamentoResponse` | 201 |
| GET | `/orcamentos` | — | `OrcamentoResponse[]` | 200 |
| GET | `/orcamentos/{id}` | path `id` | `OrcamentoResponse` | 200 |
| PUT | `/orcamentos/{id}` | path `id`; `OrcamentoUpdateRequest` | `OrcamentoResponse` | 200 |

Não há DELETE. O PUT corrige somente o Cliente e está sujeito ao estado inicial descrito em [dominio.md](dominio.md).

Exemplo de criação:

```json
{
  "clienteId": 10,
  "observacao": "Reforma da área externa"
}
```

Exemplo de resposta:

```json
{
  "id": 25,
  "numero": 1025,
  "cliente": { "id": 10, "nome": "Cliente Exemplo" },
  "versaoAtual": {
    "id": 40,
    "numeroVersao": 1,
    "status": { "id": 1, "codigo": "RASCUNHO", "nome": "Rascunho" },
    "criadoEm": "2026-08-23T12:00:00"
  },
  "criadoEm": "2026-08-23T12:00:00"
}
```

## Versões do orçamento

Todas as operações recebem path `orcamentoId`.

| Método | Rota | Entrada | Response | Status |
|---|---|---|---|---|
| GET | `/orcamentos/{orcamentoId}/versoes` | — | `OrcamentoVersaoResponse[]` | 200 |
| GET | `/orcamentos/{orcamentoId}/versoes/{versaoId}` | path `versaoId` | `OrcamentoVersaoResponse` | 200 |
| PUT | `/orcamentos/{orcamentoId}/versoes/{versaoId}` | `OrcamentoVersaoUpdateRequest` | `OrcamentoVersaoResponse` | 200 |
| PUT | `/orcamentos/{orcamentoId}/versoes/{versaoId}/status` | `OrcamentoVersaoStatusRequest` | `OrcamentoVersaoResponse` | 200 |
| POST | `/orcamentos/{orcamentoId}/versoes/{versaoId}/nova-versao` | sem body | `OrcamentoVersaoResponse` | 201 |

Atualização parcial da observação:

```json
{ "observacao": "Proposta revisada" }
```

O campo pode ser enviado como `null` para limpar a observação. Body `{}` preserva o valor atual.

Mudança de status:

```json
{ "statusOrcamentoId": 2 }
```

## Linhas da versão

As quatro coleções são delimitadas pelos paths `orcamentoId` e `versaoId`.

| Recurso | Base | Create request | Update request | Response |
|---|---|---|---|---|
| Item | `/orcamentos/{orcamentoId}/versoes/{versaoId}/itens` | `ItemOrcamentoRequest` | `ItemOrcamentoUpdateRequest` | `ItemOrcamentoResponse` |
| Material | `/orcamentos/{orcamentoId}/versoes/{versaoId}/materiais` | `MaterialOrcamentoRequest` | `MaterialOrcamentoUpdateRequest` | `MaterialOrcamentoResponse` |
| Mão de obra | `/orcamentos/{orcamentoId}/versoes/{versaoId}/mao-de-obra` | `MaoDeObraOrcamentoRequest` | `MaoDeObraOrcamentoUpdateRequest` | `MaoDeObraOrcamentoResponse` |
| Despesa | `/orcamentos/{orcamentoId}/versoes/{versaoId}/despesas` | `DespesaOrcamentoRequest` | `DespesaOrcamentoUpdateRequest` | `DespesaOrcamentoResponse` |

Cada base oferece:

| Método | Sufixo | Parâmetros/body | Status |
|---|---|---|---|
| POST | — | create request | 201 |
| GET | — | —; retorna array | 200 |
| GET | `/{linhaId}` | ID da linha | 200 |
| PUT | `/{linhaId}` | update request | 200 |
| DELETE | `/{linhaId}` | ID da linha | 204 |

O nome real do path da linha é `itemId`, `materialOrcamentoId`, `maoDeObraOrcamentoId` ou `despesaOrcamentoId`. PUTs são parciais: campos omitidos são preservados. Para `descricao`, envio explícito é distinguido de omissão; nas linhas, `null`, vazio ou apenas espaços é rejeitado.

Exemplo de item:

```json
{
  "servicoId": 5,
  "quantidade": 2.5000,
  "valorUnitario": 150.00,
  "desconto": 20.00
}
```

Exemplo de material previsto:

```json
{
  "materialId": 8,
  "quantidade": 3.0000,
  "custoUnitario": 75.00
}
```

## Ordens de serviço

| Método | Rota | Parâmetros/body | Response | Status |
|---|---|---|---|---|
| POST | `/orcamentos/{orcamentoId}/versoes/{versaoId}/ordem-servico` | paths; sem body | `OrdemServicoResponse` | 201 |
| GET | `/ordens-servico` | query params opcionais | `OrdemServicoResponse[]` | 200 |
| GET | `/ordens-servico/{ordemServicoId}` | path | `OrdemServicoResponse` | 200 |
| PUT | `/ordens-servico/{ordemServicoId}` | `OrdemServicoUpdateRequest` | `OrdemServicoResponse` | 200 |
| PUT | `/ordens-servico/{ordemServicoId}/status` | `OrdemServicoStatusRequest` | `OrdemServicoResponse` | 200 |

Query params de `GET /ordens-servico`:

| Parâmetro | Tipo | Validação/comportamento |
|---|---|---|
| `numero` | Long | opcional; positivo |
| `status` | String | opcional; até 50; deve conter caractere não branco; normalizado para trim/maiúsculas |
| `clienteId` | Long | opcional; positivo |
| `orcamentoId` | Long | opcional; positivo |
| `criadoDe` | data ISO | opcional; início inclusivo |
| `criadoAte` | data ISO | opcional; fim inclusivo |

`criadoDe > criadoAte` gera erro de negócio. O resultado é ordenado por `numero` ascendente e não é paginado.

Update da observação:

```json
{ "observacao": "Separar materiais antes da execução" }
```

O campo explícito `null` limpa a observação; `{}` preserva. Mudança de status:

```json
{ "statusOrdemServicoId": 2 }
```

## Importações

| Método | Rota | Content-Type | Parâmetro | Response | Status |
|---|---|---|---|---|---|
| POST | `/importacoes/clientes` | `multipart/form-data` | arquivo `MultipartFile` | sem body | 200 |
| POST | `/importacoes/fornecedores` | `multipart/form-data` | arquivo `MultipartFile` | sem body | 200 |
| POST | `/importacoes/materiais` | `multipart/form-data` | arquivo `MultipartFile` | sem body | 200 |

O código lê somente a primeira aba e usa colunas fixas. Não há response com contadores ou erros por linha.

Comportamento atual confirmado em integração:

- Cliente usa as colunas 1–8 e tenta persistir inclusive linha sem nome; uma violação do schema provoca rollback de todo o arquivo.
- Fornecedor ignora linha sem nome na coluna 1; Material ignora linha sem nome na coluna 4.
- duplicidades textuais são persistidas como registros distintos; não há consolidação ou relatório;
- planilha válida contendo somente cabeçalho termina com 200 e não persiste registros;
- bytes que não formam uma planilha resultam em `IOException`, tratada como 400;
- arquivo de zero bytes resulta em `400 ProblemDetail`, título `Erro ao importar arquivo` e detalhe estável `O arquivo enviado está vazio.`.

## DTOs de entrada

`obrigatório` abaixo significa Bean Validation existente, não inferência do schema.

### Cadastros

| DTO | Campos e validações |
|---|---|
| `ClienteRequest` | `nome` obrigatório/não branco/máx. 200; opcionais: `cpf` máx. 14, `cnpj` máx. 18, `telefone` e `celular` máx. 20, `email` válido/máx. 150, `contato` máx. 150, `endereco` texto, `ativo` Boolean |
| `FornecedorRequest` | `nome` obrigatório/não branco/máx. 200; opcionais: `endereco` texto, `celular` máx. 20, `email` válido/máx. 150, `contato` máx. 150, `ativo` |
| `MaterialRequest` | `nome` obrigatório/não branco/máx. 200; `unidade` obrigatória/não branca/máx. 10; opcionais: `descricao` texto, `largura` até 8 inteiros e 2 decimais, `ativo` |
| `CategoriaServicoRequest` | `nome` obrigatório/não branco/máx. 200; `ativo` opcional |
| `ServicoRequest` | `nome` obrigatório/não branco/máx. 200; `categoriaServicoId` obrigatório; `ativo` opcional |
| `MaterialFornecedorRequest` | `materialId` e `fornecedorId` obrigatórios; `precoCompra` opcional, não negativo, até 13 inteiros e 2 decimais |
| `UnidadeMaoDeObraRequest` | `nome` obrigatório/não branco/máx. 100; `ativo` opcional |
| `StatusOrcamentoRequest` | `codigo` obrigatório/máx. 50/padrão `^[A-Za-z][A-Za-z0-9_]*$`; `nome` obrigatório/máx. 100 |
| `StatusOrcamentoUpdateRequest` | `nome` obrigatório/máx. 100; `ativo` opcional; código ausente e imutável |
| `StatusOrdemServicoRequest` | mesmos limites e padrão de código; `nome` obrigatório/máx. 100 |
| `StatusOrdemServicoUpdateRequest` | `nome` obrigatório/máx. 100; `ativo` opcional; código ausente e imutável |

Na criação, CategoriaServico, Servico, UnidadeMaoDeObra e ambos os status são forçados a ativos pelo Service, mesmo que `ativo=false` seja enviado nos DTOs que possuem o campo. Cliente, Fornecedor e Material aplicam seus mappers atuais, com default ativo quando o campo é omitido.

### Orçamento e linhas

| DTO | Campos e validações |
|---|---|
| `OrcamentoRequest` | `clienteId` obrigatório; `observacao` opcional |
| `OrcamentoUpdateRequest` | `clienteId` obrigatório |
| `OrcamentoVersaoUpdateRequest` | `observacao` opcional, com presença explícita detectada |
| `OrcamentoVersaoStatusRequest` | `statusOrcamentoId` obrigatório |
| `ItemOrcamentoRequest` | `servicoId`, `quantidade` e `valorUnitario` obrigatórios; quantidade positiva, até 11+4 dígitos; valor não negativo, 13+2; `desconto` opcional, não negativo, 13+2 |
| `ItemOrcamentoUpdateRequest` | todos opcionais: `servicoId`, `descricao` máx. 200, quantidade positiva 11+4, `valorUnitario`/`desconto` não negativos 13+2 |
| `MaterialOrcamentoRequest` | `materialId`, quantidade positiva 11+4 e custo unitário não negativo 13+2, todos obrigatórios |
| `MaterialOrcamentoUpdateRequest` | todos opcionais: `materialId`, `descricao` máx. 200, quantidade positiva 11+4, custo unitário não negativo 13+2 |
| `MaoDeObraOrcamentoRequest` | unidade, descrição, quantidade e custo obrigatórios; descrição máx. 200; quantidade positiva 11+4; custo não negativo 13+2 |
| `MaoDeObraOrcamentoUpdateRequest` | todos opcionais, com os mesmos limites quando presentes |
| `DespesaOrcamentoRequest` | descrição obrigatória/máx. 200; valor obrigatório/não negativo/13+2 |
| `DespesaOrcamentoUpdateRequest` | descrição e valor opcionais; mesmos limites quando presentes |
| `OrdemServicoUpdateRequest` | `observacao` opcional, com presença explícita detectada |
| `OrdemServicoStatusRequest` | `statusOrdemServicoId` obrigatório |

Além do Bean Validation, Services rejeitam transições inválidas, referências inativas, contexto incorreto, desconto maior que subtotal e edições de versões/ordens congeladas.

## DTOs de resposta

| DTO | Campos |
|---|---|
| `ClienteResponse` | `id`, `nome`, `cpf`, `cnpj`, `telefone`, `celular`, `email`, `contato`, `endereco`, `ativo`, `criadoEm` |
| `FornecedorResponse` | `id`, `nome`, `endereco`, `celular`, `email`, `contato`, `ativo`, `criadoEm` |
| `MaterialResponse` | `id`, `nome`, `descricao`, `unidade`, `largura`, `ativo`, `criadoEm` |
| `CategoriaServicoResponse` | `id`, `nome`, `ativo`, `criadoEm` |
| `ServicoResponse` | `id`, `nome`, `categoriaServico {id,nome}`, `ativo`, `criadoEm` |
| `MaterialFornecedorResponse` | `id`, `material {id,nome}`, `fornecedor {id,nome}`, `precoCompra`, `ativo`, `criadoEm` |
| `UnidadeMaoDeObraResponse` | `id`, `nome`, `ativo`, `criadoEm` |
| `StatusOrcamentoResponse` / `StatusOrdemServicoResponse` | `id`, `codigo`, `nome`, `ativo`, `criadoEm` |
| `OrcamentoResponse` | `id`, `numero`, `cliente {id,nome}`, `versaoAtual {id,numeroVersao,status,criadoEm}`, `criadoEm` |
| `OrcamentoVersaoResponse` | `id`, `numeroVersao`, `status {id,codigo,nome}`, `observacao`, quatro totais, `margemPrevista`, `percentualMargem`, `criadoEm`, `acoesPermitidas {editarConteudo,criarNovaVersao,alterarStatusPara[]}` |
| `ItemOrcamentoResponse` | `id`, `servico {id,nome}`, `descricao`, `quantidade`, `valorUnitario`, `desconto`, `valorTotal`, `criadoEm` |
| `MaterialOrcamentoResponse` | `id`, `material {id,nome}`, `descricao`, `unidade`, `quantidade`, `custoUnitario`, `custoTotal`, `criadoEm` |
| `MaoDeObraOrcamentoResponse` | `id`, `unidadeMaoDeObra {id,nome}`, `descricao`, `unidade`, `quantidade`, `custoUnitario`, `custoTotal`, `criadoEm` |
| `DespesaOrcamentoResponse` | `id`, `descricao`, `valor`, `criadoEm` |
| `OrdemServicoResponse` | `id`, `numero`, `status {id,codigo,nome}`, `observacao`, `criadoEm`, `origem {orcamento {id,numero}, versao {id,numeroVersao}, cliente {id,nome}}`, `acoesPermitidas {editarObservacao,alterarStatusPara[]}` |

## ProblemDetail implementado

### Recurso inexistente — 404

```json
{
  "type": "about:blank",
  "title": "Recurso não encontrado",
  "status": 404,
  "detail": "Orçamento não encontrado. Id: 999",
  "instance": "/orcamentos/999"
}
```

### Regra de negócio — 400

```json
{
  "type": "about:blank",
  "title": "Erro de negócio",
  "status": 400,
  "detail": "Somente uma versão em RASCUNHO pode ter seu conteúdo alterado.",
  "instance": "/orcamentos/25/versoes/40"
}
```

### Bean Validation — 400

```json
{
  "type": "about:blank",
  "title": "Dados inválidos",
  "status": 400,
  "detail": "Um ou mais campos estão inválidos.",
  "instance": "/orcamentos",
  "erros": {
    "clienteId": "O cliente é obrigatório."
  }
}
```

### IOException de importação — 400

Título `Erro ao importar arquivo`; `detail` recebe a mensagem da `IOException`.

### Erros técnicos padronizados

| Cenário | Status | `title` | Detalhe/propriedades |
|---|---:|---|---|
| JSON malformado | 400 | `JSON inválido` | detalhe estável; sem ecoar erro interno do parser |
| path incompatível com o tipo declarado | 400 | `Parâmetro inválido` | `erros` indexado pelo nome da path variable |
| query inválida em DTO validado | 400 | `Dados inválidos` | `erros` indexado pelo query parameter |
| parâmetro obrigatório ausente | 400 | `Requisição inválida` | `erros` indexado pelo parâmetro |
| parte multipart obrigatória ausente | 400 | `Requisição inválida` | `erros.arquivo` |
| arquivo de zero bytes | 400 | `Erro ao importar arquivo` | `O arquivo enviado está vazio.` |
| multipart não processável | 400 | `Multipart inválido` | detalhe estável |
| upload acima do limite configurado | 413 | `Arquivo muito grande` | detalhe estável |
| content-type não suportado | 415 | `Tipo de conteúdo não suportado` | detalhe estável |
| método HTTP não permitido | 405 | `Método HTTP não permitido` | detalhe estável |

Todos usam `application/problem+json`. Mensagens técnicas são deliberadamente estáveis e não expõem exceções, stack traces ou detalhes do parser. O contrato existente de `BusinessException`, `ResourceNotFoundException`, Bean Validation e `IOException` foi preservado.

## Divergências do contrato

- Resolvido no DTO: `MaterialRequest.nome` e `unidade` agora refletem as colunas `NOT NULL`; nome, unidade e largura refletem tamanhos/precisão do schema.
- Resolvido no DTO: nomes de Cliente/Fornecedor usam máximo 200; email e contato usam máximo 150; campos `TEXT` de endereço/descrição não possuem limite artificial de 100.
- Mantido deliberadamente: `ativo` continua opcional nos DTOs em que o Mapper/Service possui default confirmado; isso não conflita com `NOT NULL` no estado persistido.
- Mantido deliberadamente: observações são opcionais porque as colunas correspondentes aceitam `NULL` e os updates distinguem omissão de envio explícito.
- Nenhuma mudança de schema foi necessária e nenhuma migration foi criada ou alterada.

## OpenAPI

- Springdoc 3.1.0, compatível com Spring Boot 4, publica o documento em `/api-docs` e a interface em `/swagger` conforme `application.yaml`.
- Requests, parâmetros e responses de sucesso são inferidos dos Controllers/DTOs reais.
- `ApiProblemDetail` é o schema reutilizado para erros comerciais e de importação.
- As respostas comerciais preservam os sucessos gerados e acrescentam `400` e, quando há contexto por path, `404`.
- Importações documentam `400`, `413` e `415`.
- Os schemas de `acoesPermitidas` fazem parte de `OrcamentoVersaoResponse` e `OrdemServicoResponse`.
- Um teste integrado consulta o JSON gerado e verifica paths, requests, parâmetros, sucessos, erros e capacidades.
