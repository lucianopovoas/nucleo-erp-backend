CREATE TABLE servico (
    id BIGSERIAL PRIMARY KEY,

    categoria_servico_id BIGINT NOT NULL,

    descricao VARCHAR(255) NOT NULL,

    unidade VARCHAR(20) NOT NULL,

    preco_venda NUMERIC(10,2) NOT NULL,

    preco_custo NUMERIC(10,2),

    ativo BOOLEAN NOT NULL DEFAULT TRUE,

    criado_em TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_servico_categoria
        FOREIGN KEY (categoria_servico_id)
        REFERENCES categoria_servico(id)
);