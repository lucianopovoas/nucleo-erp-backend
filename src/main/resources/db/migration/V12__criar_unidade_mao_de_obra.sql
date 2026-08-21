CREATE TABLE unidade_mao_de_obra (
    id BIGSERIAL PRIMARY KEY,

    nome VARCHAR(100) NOT NULL,

    ativo BOOLEAN NOT NULL DEFAULT TRUE,
    criado_em TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE UNIQUE INDEX uk_unidade_mao_de_obra_nome_normalizado
    ON unidade_mao_de_obra (LOWER(BTRIM(nome)));
