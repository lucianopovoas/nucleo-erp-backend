ALTER TABLE status_orcamento
    ADD COLUMN codigo VARCHAR(50);

UPDATE status_orcamento
SET codigo = CASE LOWER(BTRIM(nome))
    WHEN 'rascunho' THEN 'RASCUNHO'
    WHEN 'enviado' THEN 'ENVIADO'
    WHEN 'aprovado' THEN 'APROVADO'
    WHEN 'recusado' THEN 'RECUSADO'
    WHEN 'cancelado' THEN 'CANCELADO'
END
WHERE LOWER(BTRIM(nome)) IN ('rascunho', 'enviado', 'aprovado', 'recusado', 'cancelado');

DO $$
DECLARE
    codigo_canonico TEXT;
BEGIN
    FOREACH codigo_canonico IN ARRAY ARRAY[
        'RASCUNHO', 'ENVIADO', 'APROVADO', 'RECUSADO', 'CANCELADO'
    ] LOOP
        IF (SELECT COUNT(*) FROM status_orcamento WHERE codigo = codigo_canonico) <> 1 THEN
            RAISE EXCEPTION
                'Nao foi possivel mapear de forma inequivoca o status canonico %.',
                codigo_canonico;
        END IF;
    END LOOP;
END
$$;

WITH codigos_gerados AS (
    SELECT
        id,
        BTRIM(
            REGEXP_REPLACE(
                UPPER(TRANSLATE(
                    BTRIM(nome),
                    'ÁÀÂÃÄÉÈÊËÍÌÎÏÓÒÔÕÖÚÙÛÜÇÑáàâãäéèêëíìîïóòôõöúùûüçñ',
                    'AAAAAEEEEIIIIOOOOOUUUUCNaaaaaeeeeiiiiooooouuuucn'
                )),
                '[^A-Z0-9]+',
                '_',
                'g'
            ),
            '_'
        ) AS codigo
    FROM status_orcamento
    WHERE codigo IS NULL
)
UPDATE status_orcamento status
SET codigo = gerado.codigo
FROM codigos_gerados gerado
WHERE status.id = gerado.id;

DO $$
DECLARE
    problemas TEXT;
BEGIN
    SELECT STRING_AGG(FORMAT('id=%s nome="%s" codigo="%s"', id, nome, codigo), '; ' ORDER BY id)
    INTO problemas
    FROM status_orcamento
    WHERE codigo IS NULL
       OR codigo = ''
       OR LENGTH(codigo) > 50
       OR codigo !~ '^[A-Z][A-Z0-9_]*$';

    IF problemas IS NOT NULL THEN
        RAISE EXCEPTION
            'Codigos de status nao puderam ser gerados com seguranca: %. Defina mapeamento explicito.',
            problemas;
    END IF;

    SELECT STRING_AGG(FORMAT('codigo="%s" ids=%s', codigo, ids), '; ' ORDER BY codigo)
    INTO problemas
    FROM (
        SELECT codigo, STRING_AGG(id::TEXT, ',' ORDER BY id) AS ids
        FROM status_orcamento
        GROUP BY codigo
        HAVING COUNT(*) > 1
    ) duplicados;

    IF problemas IS NOT NULL THEN
        RAISE EXCEPTION
            'Colisao ambigua na geracao de codigos de status: %. Defina mapeamento explicito.',
            problemas;
    END IF;

    SELECT STRING_AGG(FORMAT('id=%s nome="%s" codigo="%s"', id, nome, codigo), '; ' ORDER BY id)
    INTO problemas
    FROM status_orcamento
    WHERE codigo IN ('RASCUNHO', 'ENVIADO', 'APROVADO', 'RECUSADO', 'CANCELADO')
      AND LOWER(BTRIM(nome)) NOT IN ('rascunho', 'enviado', 'aprovado', 'recusado', 'cancelado');

    IF problemas IS NOT NULL THEN
        RAISE EXCEPTION
            'Status nao canonico colidiu com codigo reservado: %. Defina mapeamento explicito.',
            problemas;
    END IF;
END
$$;

ALTER TABLE status_orcamento
    ALTER COLUMN codigo SET NOT NULL,
    ADD CONSTRAINT uk_status_orcamento_codigo UNIQUE (codigo),
    ADD CONSTRAINT ck_status_orcamento_codigo_formato
        CHECK (codigo ~ '^[A-Z][A-Z0-9_]*$');

CREATE FUNCTION impedir_alteracao_codigo_status_orcamento()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
BEGIN
    IF OLD.codigo IS DISTINCT FROM NEW.codigo THEN
        RAISE EXCEPTION 'O codigo do status de orcamento e imutavel.';
    END IF;
    RETURN NEW;
END
$$;

CREATE TRIGGER trg_status_orcamento_codigo_imutavel
    BEFORE UPDATE OF codigo ON status_orcamento
    FOR EACH ROW
    EXECUTE FUNCTION impedir_alteracao_codigo_status_orcamento();
