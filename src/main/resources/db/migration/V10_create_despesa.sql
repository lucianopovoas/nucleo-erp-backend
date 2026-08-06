CREATE TABLE despesa (

    id BIGSERIAL PRIMARY KEY,

    orcamento_id BIGINT,

    descricao VARCHAR(255) NOT NULL,

    valor NUMERIC(10,2) NOT NULL,

    data_despesa DATE,

    observacao TEXT,

    CONSTRAINT fk_despesa_orcamento
        FOREIGN KEY (orcamento_id)
        REFERENCES orcamento(id)

);