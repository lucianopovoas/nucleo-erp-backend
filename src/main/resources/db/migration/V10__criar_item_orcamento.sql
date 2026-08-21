CREATE TABLE item_orcamento (
    id BIGSERIAL PRIMARY KEY,

    orcamento_id BIGINT NOT NULL,
    servico_id BIGINT NOT NULL,
    descricao VARCHAR(200) NOT NULL,
    quantidade NUMERIC(15, 4) NOT NULL,
    valor_unitario NUMERIC(15, 2) NOT NULL,
    desconto NUMERIC(15, 2) NOT NULL DEFAULT 0,
    valor_total NUMERIC(15, 2) NOT NULL,
    criado_em TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_item_orcamento_orcamento
        FOREIGN KEY (orcamento_id)
        REFERENCES orcamento(id),

    CONSTRAINT fk_item_orcamento_servico
        FOREIGN KEY (servico_id)
        REFERENCES servico(id),

    CONSTRAINT ck_item_orcamento_quantidade_positiva
        CHECK (quantidade > 0),

    CONSTRAINT ck_item_orcamento_valor_unitario_nao_negativo
        CHECK (valor_unitario >= 0),

    CONSTRAINT ck_item_orcamento_desconto_nao_negativo
        CHECK (desconto >= 0),

    CONSTRAINT ck_item_orcamento_valor_total_nao_negativo
        CHECK (valor_total >= 0)
);

CREATE INDEX idx_item_orcamento_orcamento_id
    ON item_orcamento (orcamento_id);
