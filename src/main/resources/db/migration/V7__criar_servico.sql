CREATE TABLE servico (
    id BIGSERIAL PRIMARY KEY,

    nome VARCHAR(200) NOT NULL,
    categoria_servico_id BIGINT NOT NULL,

    ativo BOOLEAN NOT NULL DEFAULT TRUE,
    criado_em TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_servico_categoria_servico
        FOREIGN KEY (categoria_servico_id)
        REFERENCES categoria_servico(id)
);

CREATE UNIQUE INDEX uk_servico_categoria_nome_normalizado
    ON servico (categoria_servico_id, LOWER(BTRIM(nome)));
