CREATE TABLE mao_obra (

    id BIGSERIAL PRIMARY KEY,

    orcamento_id BIGINT NOT NULL,

    funcionario_id BIGINT NOT NULL,

    horas NUMERIC(10,2) NOT NULL,

    valor_hora NUMERIC(10,2) NOT NULL,

    subtotal NUMERIC(10,2) NOT NULL,

    CONSTRAINT fk_mao_obra_orcamento
        FOREIGN KEY (orcamento_id)
        REFERENCES orcamento(id)
        ON DELETE CASCADE,

    CONSTRAINT fk_mao_obra_funcionario
        FOREIGN KEY (funcionario_id)
        REFERENCES funcionario(id)
);