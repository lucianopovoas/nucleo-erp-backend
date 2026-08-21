CREATE TABLE orcamento_versao (
    id BIGSERIAL PRIMARY KEY,

    orcamento_id BIGINT NOT NULL,
    numero_versao INTEGER NOT NULL,
    status_orcamento_id BIGINT NOT NULL,
    observacao TEXT,
    criado_em TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_orcamento_versao_orcamento
        FOREIGN KEY (orcamento_id)
        REFERENCES orcamento(id),

    CONSTRAINT fk_orcamento_versao_status_orcamento
        FOREIGN KEY (status_orcamento_id)
        REFERENCES status_orcamento(id),

    CONSTRAINT uk_orcamento_versao_numero
        UNIQUE (orcamento_id, numero_versao),

    CONSTRAINT uk_orcamento_versao_orcamento_id_id
        UNIQUE (orcamento_id, id),

    CONSTRAINT ck_orcamento_versao_numero_positivo
        CHECK (numero_versao > 0)
);

CREATE INDEX idx_orcamento_versao_status_orcamento_id
    ON orcamento_versao (status_orcamento_id);

ALTER TABLE orcamento
    ADD COLUMN versao_atual_id BIGINT;

ALTER TABLE item_orcamento
    ADD COLUMN orcamento_versao_id BIGINT;

ALTER TABLE material_orcamento
    ADD COLUMN orcamento_versao_id BIGINT;

ALTER TABLE mao_de_obra_orcamento
    ADD COLUMN orcamento_versao_id BIGINT;

ALTER TABLE despesa_orcamento
    ADD COLUMN orcamento_versao_id BIGINT;

INSERT INTO orcamento_versao (
    orcamento_id,
    numero_versao,
    status_orcamento_id,
    observacao,
    criado_em
)
SELECT
    id,
    1,
    status_orcamento_id,
    observacao,
    criado_em
FROM orcamento;

UPDATE orcamento
SET versao_atual_id = versao.id
FROM orcamento_versao versao
WHERE versao.orcamento_id = orcamento.id
  AND versao.numero_versao = 1;

UPDATE item_orcamento linha
SET orcamento_versao_id = versao.id
FROM orcamento_versao versao
WHERE versao.orcamento_id = linha.orcamento_id
  AND versao.numero_versao = 1;

UPDATE material_orcamento linha
SET orcamento_versao_id = versao.id
FROM orcamento_versao versao
WHERE versao.orcamento_id = linha.orcamento_id
  AND versao.numero_versao = 1;

UPDATE mao_de_obra_orcamento linha
SET orcamento_versao_id = versao.id
FROM orcamento_versao versao
WHERE versao.orcamento_id = linha.orcamento_id
  AND versao.numero_versao = 1;

UPDATE despesa_orcamento linha
SET orcamento_versao_id = versao.id
FROM orcamento_versao versao
WHERE versao.orcamento_id = linha.orcamento_id
  AND versao.numero_versao = 1;

ALTER TABLE orcamento
    ADD CONSTRAINT fk_orcamento_versao_atual
        FOREIGN KEY (id, versao_atual_id)
        REFERENCES orcamento_versao (orcamento_id, id);

ALTER TABLE item_orcamento
    ADD CONSTRAINT fk_item_orcamento_orcamento_versao
        FOREIGN KEY (orcamento_versao_id)
        REFERENCES orcamento_versao(id);

ALTER TABLE material_orcamento
    ADD CONSTRAINT fk_material_orcamento_orcamento_versao
        FOREIGN KEY (orcamento_versao_id)
        REFERENCES orcamento_versao(id);

ALTER TABLE mao_de_obra_orcamento
    ADD CONSTRAINT fk_mao_de_obra_orcamento_orcamento_versao
        FOREIGN KEY (orcamento_versao_id)
        REFERENCES orcamento_versao(id);

ALTER TABLE despesa_orcamento
    ADD CONSTRAINT fk_despesa_orcamento_orcamento_versao
        FOREIGN KEY (orcamento_versao_id)
        REFERENCES orcamento_versao(id);

CREATE INDEX idx_item_orcamento_orcamento_versao_id
    ON item_orcamento (orcamento_versao_id);

CREATE INDEX idx_material_orcamento_orcamento_versao_id
    ON material_orcamento (orcamento_versao_id);

CREATE INDEX idx_mao_de_obra_orcamento_orcamento_versao_id
    ON mao_de_obra_orcamento (orcamento_versao_id);

CREATE INDEX idx_despesa_orcamento_orcamento_versao_id
    ON despesa_orcamento (orcamento_versao_id);

DO $$
DECLARE
    status_aprovado_id BIGINT;
BEGIN
    SELECT id
    INTO STRICT status_aprovado_id
    FROM status_orcamento
    WHERE codigo = 'APROVADO';

    EXECUTE FORMAT(
        'CREATE UNIQUE INDEX uk_orcamento_versao_aprovada_por_orcamento '
        'ON orcamento_versao (orcamento_id) WHERE status_orcamento_id = %s',
        status_aprovado_id
    );
END
$$;

DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM orcamento orcamento
        LEFT JOIN orcamento_versao versao
          ON versao.orcamento_id = orcamento.id
         AND versao.numero_versao = 1
        GROUP BY orcamento.id
        HAVING COUNT(versao.id) <> 1
    ) THEN
        RAISE EXCEPTION 'Nem todo orcamento possui exatamente uma V1 apos o backfill.';
    END IF;

    IF EXISTS (
        SELECT 1
        FROM orcamento orcamento
        LEFT JOIN orcamento_versao versao
          ON versao.id = orcamento.versao_atual_id
         AND versao.orcamento_id = orcamento.id
        WHERE versao.id IS NULL
    ) THEN
        RAISE EXCEPTION 'Versao atual ausente ou pertencente a outro orcamento.';
    END IF;

    IF EXISTS (SELECT 1 FROM item_orcamento WHERE orcamento_versao_id IS NULL)
       OR EXISTS (SELECT 1 FROM material_orcamento WHERE orcamento_versao_id IS NULL)
       OR EXISTS (SELECT 1 FROM mao_de_obra_orcamento WHERE orcamento_versao_id IS NULL)
       OR EXISTS (SELECT 1 FROM despesa_orcamento WHERE orcamento_versao_id IS NULL) THEN
        RAISE EXCEPTION 'Existem linhas de orcamento sem versao apos o backfill.';
    END IF;
END
$$;
