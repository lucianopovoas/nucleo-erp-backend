CREATE TABLE item_orcamento (

    id BIGSERIAL PRIMARY KEY,

    orcamento_id BIGINT NOT NULL,

    servico_id BIGINT NOT NULL,

    quantidade NUMERIC(10,2) NOT NULL,

    valor_unitario NUMERIC(10,2) NOT NULL,

    desconto NUMERIC(10,2) DEFAULT 0,

    subtotal NUMERIC(10,2) NOT NULL,

    CONSTRAINT fk_item_orcamento
        FOREIGN KEY (orcamento_id)
        REFERENCES orcamento(id)
        ON DELETE CASCADE,

    CONSTRAINT fk_item_servico
        FOREIGN KEY (servico_id)
        REFERENCES servico(id)

);