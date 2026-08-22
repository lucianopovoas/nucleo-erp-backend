CREATE INDEX idx_ordem_servico_criado_em
    ON ordem_servico (criado_em);

CREATE INDEX idx_orcamento_cliente_id
    ON orcamento (cliente_id);
