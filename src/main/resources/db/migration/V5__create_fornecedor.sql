CREATE TABLE fornecedor (

    id BIGSERIAL PRIMARY KEY,

    nome VARCHAR(200) NOT NULL,

    telefone VARCHAR(20),

    email VARCHAR(150),

    contato VARCHAR(150),

    endereco TEXT,

    criado_em TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);