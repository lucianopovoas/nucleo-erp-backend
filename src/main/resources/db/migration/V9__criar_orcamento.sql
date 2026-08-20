CREATE SEQUENCE orcamento_numero_seq
    AS BIGINT
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

CREATE TABLE orcamento (
    id BIGSERIAL PRIMARY KEY,

    numero BIGINT NOT NULL DEFAULT nextval('orcamento_numero_seq'),
    cliente_id BIGINT NOT NULL,
    status_orcamento_id BIGINT NOT NULL,
    observacao TEXT,

    criado_em TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uk_orcamento_numero
        UNIQUE (numero),

    CONSTRAINT fk_orcamento_cliente
        FOREIGN KEY (cliente_id)
        REFERENCES cliente(id),

    CONSTRAINT fk_orcamento_status_orcamento
        FOREIGN KEY (status_orcamento_id)
        REFERENCES status_orcamento(id)
);

ALTER SEQUENCE orcamento_numero_seq
    OWNED BY orcamento.numero;
