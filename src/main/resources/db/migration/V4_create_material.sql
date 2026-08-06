CREATE TABLE material (

    id BIGSERIAL PRIMARY KEY,

    nome VARCHAR(200) NOT NULL,

    unidade VARCHAR(20) NOT NULL,

    custo_medio NUMERIC(10,2),

    descricao TEXT,

    ativo BOOLEAN NOT NULL DEFAULT TRUE,

    criado_em TIMESTAMP DEFAULT CURRENT_TIMESTAMP

);