CREATE TABLE orcamento (

    id BIGSERIAL PRIMARY KEY,

    numero BIGINT NOT NULL UNIQUE,

    cliente_id BIGINT NOT NULL,

    data_orcamento DATE NOT NULL,

    desconto NUMERIC(10,2) DEFAULT 0,

    percentual_imposto NUMERIC(5,2) DEFAULT 0,

    valor_total NUMERIC(10,2) DEFAULT 0,

    observacao TEXT,

    status status_orcamento NOT NULL DEFAULT 'RASCUNHO',

    criado_em TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_orcamento_cliente
        FOREIGN KEY (cliente_id)
        REFERENCES cliente(id)

);