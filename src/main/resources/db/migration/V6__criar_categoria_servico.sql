CREATE TABLE categoria_servico (
    id BIGSERIAL PRIMARY KEY,

    nome VARCHAR(200) NOT NULL,

    ativo BOOLEAN NOT NULL DEFAULT TRUE,
    criado_em TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE UNIQUE INDEX uk_categoria_servico_nome_normalizado
    ON categoria_servico (LOWER(BTRIM(nome)));
