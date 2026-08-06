CREATE TABLE material_orcamento (

    id BIGSERIAL PRIMARY KEY,

    orcamento_id BIGINT NOT NULL,

    material_id BIGINT NOT NULL,

    quantidade NUMERIC(10,2) NOT NULL,

    valor_unitario NUMERIC(10,2) NOT NULL,

    subtotal NUMERIC(10,2) NOT NULL,

    observacao TEXT,

    CONSTRAINT fk_material_orcamento
        FOREIGN KEY (orcamento_id)
        REFERENCES orcamento(id)
        ON DELETE CASCADE,

    CONSTRAINT fk_material
        FOREIGN KEY (material_id)
        REFERENCES material(id)

);