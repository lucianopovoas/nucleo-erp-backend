CREATE TABLE status_ordem_servico (
    id BIGSERIAL PRIMARY KEY,

    codigo VARCHAR(50) NOT NULL,
    nome VARCHAR(100) NOT NULL,

    ativo BOOLEAN NOT NULL DEFAULT TRUE,
    criado_em TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uk_status_ordem_servico_codigo
        UNIQUE (codigo),

    CONSTRAINT ck_status_ordem_servico_codigo_formato
        CHECK (codigo ~ '^[A-Z][A-Z0-9_]*$')
);

CREATE UNIQUE INDEX uk_status_ordem_servico_nome_normalizado
    ON status_ordem_servico (LOWER(BTRIM(nome)));

CREATE FUNCTION impedir_alteracao_codigo_status_ordem_servico()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
BEGIN
    IF OLD.codigo IS DISTINCT FROM NEW.codigo THEN
        RAISE EXCEPTION 'O codigo do status de ordem de servico e imutavel.';
    END IF;
    RETURN NEW;
END
$$;

CREATE TRIGGER trg_status_ordem_servico_codigo_imutavel
    BEFORE UPDATE OF codigo ON status_ordem_servico
    FOR EACH ROW
    EXECUTE FUNCTION impedir_alteracao_codigo_status_ordem_servico();

INSERT INTO status_ordem_servico (codigo, nome)
VALUES
    ('COMPRAR_MATERIAL', 'Comprar material'),
    ('EM_EXECUCAO', 'Em execução'),
    ('INSTALAR', 'Instalar'),
    ('CONCLUIDO', 'Concluído');

CREATE SEQUENCE ordem_servico_numero_seq
    AS BIGINT
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

CREATE TABLE ordem_servico (
    id BIGSERIAL PRIMARY KEY,

    numero BIGINT NOT NULL DEFAULT nextval('ordem_servico_numero_seq'),
    orcamento_versao_id BIGINT NOT NULL,
    status_ordem_servico_id BIGINT NOT NULL,
    observacao TEXT,

    criado_em TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uk_ordem_servico_numero
        UNIQUE (numero),

    CONSTRAINT uk_ordem_servico_orcamento_versao
        UNIQUE (orcamento_versao_id),

    CONSTRAINT fk_ordem_servico_orcamento_versao
        FOREIGN KEY (orcamento_versao_id)
        REFERENCES orcamento_versao(id),

    CONSTRAINT fk_ordem_servico_status_ordem_servico
        FOREIGN KEY (status_ordem_servico_id)
        REFERENCES status_ordem_servico(id)
);

ALTER SEQUENCE ordem_servico_numero_seq
    OWNED BY ordem_servico.numero;

CREATE INDEX idx_ordem_servico_status_ordem_servico_id
    ON ordem_servico (status_ordem_servico_id);
