CREATE TABLE material (
    id BIGSERIAL PRIMARY KEY,

    nome VARCHAR(200) NOT NULL,

    descricao TEXT,

    unidade VARCHAR(10) NOT NULL,

    largura NUMERIC(10, 2),

    ativo BOOLEAN NOT NULL DEFAULT TRUE,

    criado_em TIMESTAMP NOT NULL
);