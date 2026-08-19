CREATE TABLE material_fornecedor (
    id BIGSERIAL PRIMARY KEY,

    material_id BIGINT NOT NULL,
    fornecedor_id BIGINT NOT NULL,

    preco_compra NUMERIC(15, 2),

    ativo BOOLEAN NOT NULL DEFAULT TRUE,
    criado_em TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_material_fornecedor_material
        FOREIGN KEY (material_id)
        REFERENCES material(id),

    CONSTRAINT fk_material_fornecedor_fornecedor
        FOREIGN KEY (fornecedor_id)
        REFERENCES fornecedor(id),

    CONSTRAINT uk_material_fornecedor_material_fornecedor
        UNIQUE (material_id, fornecedor_id),

    CONSTRAINT ck_material_fornecedor_preco_compra_nao_negativo
        CHECK (preco_compra IS NULL OR preco_compra >= 0)
);
