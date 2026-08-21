CREATE TABLE material_orcamento (
    id BIGSERIAL PRIMARY KEY,

    orcamento_id BIGINT NOT NULL,
    material_id BIGINT NOT NULL,
    descricao VARCHAR(200) NOT NULL,
    unidade VARCHAR(10) NOT NULL,
    quantidade NUMERIC(15, 4) NOT NULL,
    custo_unitario NUMERIC(15, 2) NOT NULL,
    custo_total NUMERIC(15, 2) NOT NULL,
    criado_em TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_material_orcamento_orcamento
        FOREIGN KEY (orcamento_id)
        REFERENCES orcamento(id),

    CONSTRAINT fk_material_orcamento_material
        FOREIGN KEY (material_id)
        REFERENCES material(id),

    CONSTRAINT ck_material_orcamento_quantidade_positiva
        CHECK (quantidade > 0),

    CONSTRAINT ck_material_orcamento_custo_unitario_nao_negativo
        CHECK (custo_unitario >= 0),

    CONSTRAINT ck_material_orcamento_custo_total_nao_negativo
        CHECK (custo_total >= 0)
);

CREATE INDEX idx_material_orcamento_orcamento_id
    ON material_orcamento (orcamento_id);
