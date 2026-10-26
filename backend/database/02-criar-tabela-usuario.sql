-- Execute no banco sistema_riscos.
-- A senha NÃO é armazenada em texto puro: somente o hash BCrypt.
CREATE TABLE IF NOT EXISTS public.usuario (
    id BIGSERIAL PRIMARY KEY,
    nome VARCHAR(150) NOT NULL,
    email VARCHAR(254) NOT NULL,
    senha_hash VARCHAR(60) NOT NULL,
    perfil VARCHAR(50) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ATIVO',
    CONSTRAINT ux_usuario_email UNIQUE (email),
    CONSTRAINT ck_usuario_status CHECK (status IN ('ATIVO', 'INATIVO'))
);

CREATE INDEX IF NOT EXISTS ix_usuario_perfil ON public.usuario(perfil);
CREATE INDEX IF NOT EXISTS ix_usuario_status ON public.usuario(status);
