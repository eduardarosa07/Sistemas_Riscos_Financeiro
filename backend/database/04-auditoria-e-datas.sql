-- Execute após o script 03. Datas históricas não podem ser reconstruídas: data da migração é estimativa.
BEGIN;
ALTER TABLE public.usuario ADD COLUMN IF NOT EXISTS created_at TIMESTAMPTZ;
ALTER TABLE public.usuario ADD COLUMN IF NOT EXISTS updated_at TIMESTAMPTZ;
UPDATE public.usuario SET created_at=COALESCE(created_at,CURRENT_TIMESTAMP),updated_at=COALESCE(updated_at,CURRENT_TIMESTAMP);
ALTER TABLE public.usuario ALTER COLUMN created_at SET NOT NULL;
ALTER TABLE public.usuario ALTER COLUMN updated_at SET NOT NULL;
ALTER TABLE public.usuario ALTER COLUMN created_at SET DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE public.usuario ALTER COLUMN updated_at SET DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE public.usuario ADD COLUMN IF NOT EXISTS sessao_versao BIGINT NOT NULL DEFAULT 0;
CREATE TABLE IF NOT EXISTS public.auditoria_usuario (
 id BIGSERIAL PRIMARY KEY,usuario_executor_id BIGINT,usuario_afetado_id BIGINT,
 acao VARCHAR(50) NOT NULL,data_hora TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
 detalhes VARCHAR(500)
);
CREATE INDEX IF NOT EXISTS ix_auditoria_data ON public.auditoria_usuario(data_hora DESC);
COMMIT;
