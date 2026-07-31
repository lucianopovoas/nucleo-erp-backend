CREATE TABLE funcionario (

    id BIGSERIAL PRIMARY KEY,

    nome VARCHAR(200) NOT NULL,

    cargo VARCHAR(100),

    valor_hora NUMERIC(10,2),

    ativo BOOLEAN DEFAULT TRUE
);