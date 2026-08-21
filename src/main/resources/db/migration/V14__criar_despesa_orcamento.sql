CREATE TABLE despesa_orcamento (
    id BIGSERIAL PRIMARY KEY,

    orcamento_id BIGINT NOT NULL,
    descricao VARCHAR(200) NOT NULL,
    valor NUMERIC(15, 2) NOT NULL,
    criado_em TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_despesa_orcamento_orcamento
        FOREIGN KEY (orcamento_id)
        REFERENCES orcamento(id),

    CONSTRAINT ck_despesa_orcamento_valor_nao_negativo
        CHECK (valor >= 0)
);

CREATE INDEX idx_despesa_orcamento_orcamento_id
    ON despesa_orcamento (orcamento_id);
