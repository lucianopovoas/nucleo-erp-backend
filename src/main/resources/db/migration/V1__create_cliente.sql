CREATE TABLE cliente (

    id BIGSERIAL PRIMARY KEY,

    nome VARCHAR(200) NOT NULL,

    cpf VARCHAR(14),

    cnpj VARCHAR(18),

    telefone VARCHAR(20),

    celular VARCHAR(20),

    email VARCHAR(150),

    contato VARCHAR(150),

    endereco TEXT,

    criado_em TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    ativo BOOLEAN NOT NULL

);