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
        RAISE EXCEPTION 'Contracao abortada: orcamento sem exatamente uma V1.';
    END IF;

    IF EXISTS (
        SELECT 1
        FROM orcamento orcamento
        LEFT JOIN orcamento_versao versao
          ON versao.id = orcamento.versao_atual_id
         AND versao.orcamento_id = orcamento.id
        WHERE versao.id IS NULL
    ) THEN
        RAISE EXCEPTION 'Contracao abortada: versao atual invalida.';
    END IF;

    IF EXISTS (
        SELECT 1 FROM item_orcamento linha
        JOIN orcamento_versao versao ON versao.id = linha.orcamento_versao_id
        WHERE linha.orcamento_versao_id IS NULL OR linha.orcamento_id <> versao.orcamento_id
    ) OR EXISTS (
        SELECT 1 FROM material_orcamento linha
        JOIN orcamento_versao versao ON versao.id = linha.orcamento_versao_id
        WHERE linha.orcamento_versao_id IS NULL OR linha.orcamento_id <> versao.orcamento_id
    ) OR EXISTS (
        SELECT 1 FROM mao_de_obra_orcamento linha
        JOIN orcamento_versao versao ON versao.id = linha.orcamento_versao_id
        WHERE linha.orcamento_versao_id IS NULL OR linha.orcamento_id <> versao.orcamento_id
    ) OR EXISTS (
        SELECT 1 FROM despesa_orcamento linha
        JOIN orcamento_versao versao ON versao.id = linha.orcamento_versao_id
        WHERE linha.orcamento_versao_id IS NULL OR linha.orcamento_id <> versao.orcamento_id
    ) THEN
        RAISE EXCEPTION 'Contracao abortada: linha vinculada a versao de outro orcamento.';
    END IF;

    IF EXISTS (SELECT 1 FROM item_orcamento WHERE orcamento_versao_id IS NULL)
       OR EXISTS (SELECT 1 FROM material_orcamento WHERE orcamento_versao_id IS NULL)
       OR EXISTS (SELECT 1 FROM mao_de_obra_orcamento WHERE orcamento_versao_id IS NULL)
       OR EXISTS (SELECT 1 FROM despesa_orcamento WHERE orcamento_versao_id IS NULL) THEN
        RAISE EXCEPTION 'Contracao abortada: linha sem versao.';
    END IF;
END
$$;

ALTER TABLE item_orcamento
    ALTER COLUMN orcamento_versao_id SET NOT NULL,
    DROP CONSTRAINT fk_item_orcamento_orcamento;

DROP INDEX idx_item_orcamento_orcamento_id;

ALTER TABLE item_orcamento
    DROP COLUMN orcamento_id;

ALTER TABLE material_orcamento
    ALTER COLUMN orcamento_versao_id SET NOT NULL,
    DROP CONSTRAINT fk_material_orcamento_orcamento;

DROP INDEX idx_material_orcamento_orcamento_id;

ALTER TABLE material_orcamento
    DROP COLUMN orcamento_id;

ALTER TABLE mao_de_obra_orcamento
    ALTER COLUMN orcamento_versao_id SET NOT NULL,
    DROP CONSTRAINT fk_mao_de_obra_orcamento_orcamento;

DROP INDEX idx_mao_de_obra_orcamento_orcamento_id;

ALTER TABLE mao_de_obra_orcamento
    DROP COLUMN orcamento_id;

ALTER TABLE despesa_orcamento
    ALTER COLUMN orcamento_versao_id SET NOT NULL,
    DROP CONSTRAINT fk_despesa_orcamento_orcamento;

DROP INDEX idx_despesa_orcamento_orcamento_id;

ALTER TABLE despesa_orcamento
    DROP COLUMN orcamento_id;

ALTER TABLE orcamento
    DROP CONSTRAINT fk_orcamento_status_orcamento,
    DROP COLUMN status_orcamento_id,
    DROP COLUMN observacao;
