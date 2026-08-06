CREATE TABLE fornecedor (

    id BIGSERIAL PRIMARY KEY,

    nome VARCHAR(200) NOT NULL,

    endereco TEXT,

    celular VARCHAR(20),

    email VARCHAR(150),

    contato VARCHAR(150),

    criado_em TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    ativo BOOLEAN NOT NULL

);