CREATE TABLE status_orcamento (
    id BIGSERIAL PRIMARY KEY,

    nome VARCHAR(100) NOT NULL,

    ativo BOOLEAN NOT NULL DEFAULT TRUE,
    criado_em TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE UNIQUE INDEX uk_status_orcamento_nome_normalizado
    ON status_orcamento (LOWER(BTRIM(nome)));

INSERT INTO status_orcamento (nome)
VALUES
    ('Rascunho'),
    ('Enviado'),
    ('Aprovado'),
    ('Recusado'),
    ('Cancelado');
