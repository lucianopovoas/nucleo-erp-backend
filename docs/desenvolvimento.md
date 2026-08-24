# Desenvolvimento do backend

> Este documento combina requisitos confirmados no projeto com um fluxo recomendado. Recomendações não significam que automações correspondentes já existam.

## Requisitos

- JDK 21;
- Maven Wrapper incluído no repositório;
- Docker Engine acessível para executar a suíte de testes isolada;
- PostgreSQL acessível e `DB_PASSWORD` fornecida externamente para executar a aplicação fora dos testes.

O `pom.xml` compila para Java 21. Antes de qualquer build:

```powershell
java -version
.\mvnw.cmd -version
```

Nesta auditoria foi encontrado Java 21 em `C:\Program Files\Java\jdk-21.0.12`, embora o `PATH` padrão apontasse para Java 17. Uma forma temporária e segura de selecionar o JDK somente na sessão PowerShell é:

```powershell
$env:JAVA_HOME = 'C:\Program Files\Java\jdk-21.0.12'
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
java -version
```

Isso não altera permanentemente o sistema. Em outra máquina, o caminho deve ser confirmado, não copiado por suposição.

## Variáveis de ambiente

| Variável | Uso |
|---|---|
| `DB_PASSWORD` | senha do PostgreSQL usado pela aplicação fora dos testes |
| `APP_CORS_ALLOWED_ORIGINS` | origens CORS exatas, separadas por vírgula; vazia não autoriza origem cross-origin |

Não registre valores dessas variáveis no Git.

## Comandos

Windows:

```powershell
.\mvnw.cmd compile
.\mvnw.cmd test
.\mvnw.cmd spring-boot:run
```

Unix:

```bash
./mvnw compile
./mvnw test
./mvnw spring-boot:run
```

Executar `test` inicia PostgreSQL 16 descartável pelo Testcontainers, aplica Flyway e valida o schema pelo Hibernate. O Docker Engine precisa estar ativo; não é necessário criar `nucleo_erp_test` nem fornecer `DB_PASSWORD` para a suíte.

## PostgreSQL de teste isolado

O isolamento está configurado em `src/test/resources/application.yaml` com:

```text
jdbc:tc:postgresql:16-alpine:///nucleo_erp_test
```

O driver JDBC do Testcontainers cria uma instância PostgreSQL exclusiva da JVM Maven, em porta aleatória. O Flyway aplica V1–V19 em banco vazio e o Hibernate valida os mapeamentos. As duas classes que validam migrations usam a mesma instância descartável e schemas aleatórios próprios; a limpeza feita por esses testes fica restrita a esses schemas dentro do container.

Ao encerrar a JVM, o Ryuk remove os containers. As imagens Docker baixadas permanecem no cache local. Não substitua essa URL por `localhost` e não desabilite o isolamento para contornar falhas de ambiente.

## Fluxo recomendado para alterações

### Antes de editar

1. Ler `AGENTS.md` e o documento de domínio relevante.
2. Executar `git status` e preservar alterações existentes.
3. Ler Controller, DTOs, Service, Entity, Repository, Mapper, migration e testes apenas do módulo afetado.
4. Confirmar schema real, dados existentes e regras de negócio.

### Mudança persistente

Tratar como uma única decisão coerente:

```text
nova migration -> Entity -> Repository -> Service -> DTO -> Mapper
               -> Controller/OpenAPI -> testes
```

- criar uma nova migration `V<versao>__<descricao>.sql`;
- não alterar, renomear ou remover migration aplicada;
- não depender de `ddl-auto` para criar schema;
- manter constraint PostgreSQL para invariantes permanentes e validação preventiva no Service para mensagem amigável;
- validar compatibilidade com dados já existentes antes de impor nova restrição.

### Mudança de contrato HTTP

Revisar em conjunto:

```text
Request / Response -> Bean Validation -> Mapper -> Controller
                   -> OpenAPI -> testes MVC
```

Não exponha Entity e não documente campo que não esteja no DTO real.

### Regras e transações

- regras entre entidades ficam no Service ou componente de política/guarda já existente;
- leituras usam transação read-only;
- escritas em negociações existentes respeitam os locks e o contexto `Orcamento -> OrcamentoVersao -> linha`;
- não introduza interface de Service, `ServiceImpl` ou CRUD genérico sem necessidade arquitetural concreta.

### Validação

1. Revisar `git diff` e arquivos fora do escopo.
2. Confirmar Java 21.
3. Compilar.
4. Executar testes unitários e MVC do módulo.
5. Executar integração em PostgreSQL descartável quando houver persistência, concorrência, transação ou migration.
6. Executar a suíte completa quando o ambiente isolado estiver disponível.

## Disciplina de escopo

- não fazer refatoração oportunista;
- não alterar migration aplicada;
- não executar manualmente `flyway clean`, `DROP` ou `TRUNCATE` contra banco persistente, nem `git reset --hard`, `git clean` ou operações equivalentes;
- não acessar banco pessoal como efeito colateral de testes;
- não transformar recomendações ou TODOs em regra implementada;
- relatar comandos realmente executados e limitações reais.

## Validação realizada em 2026-08-23

Com o Java 21 instalado selecionado apenas no processo:

- `mvnw -DskipTests compile`: concluído com sucesso;
- teste de fumaça do contexto: PostgreSQL 16.15 descartável iniciado, V1–V19 aplicadas e Hibernate validado;
- `mvnw test`: 490 testes, 0 falhas, 0 erros e 0 ignorados;
- foram executados testes unitários, MVC, integração, concorrência, rollback, constraints e migrations.

Essa execução valida o schema criado do zero pelas migrations no container. Ela não verifica o estado nem os dados de qualquer banco de desenvolvimento ou produção.
