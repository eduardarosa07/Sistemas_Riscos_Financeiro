-- Confere dados existentes antes de restringir; não remove nem altera usuários.
BEGIN;
DO $$ BEGIN
 IF EXISTS(SELECT 1 FROM public.usuario GROUP BY LOWER(TRIM(email)) HAVING COUNT(*)>1) THEN
  RAISE EXCEPTION 'Emails equivalentes duplicados. Corrija manualmente antes de prosseguir.';
 END IF;
 IF EXISTS(SELECT 1 FROM public.usuario WHERE email<>LOWER(TRIM(email))) THEN
  RAISE EXCEPTION 'Normalize emails existentes antes de prosseguir.';
 END IF;
END $$;
CREATE UNIQUE INDEX IF NOT EXISTS ux_usuario_email_normalizado ON public.usuario(LOWER(TRIM(email)));
ALTER TABLE public.usuario DROP CONSTRAINT IF EXISTS ck_usuario_perfil;
ALTER TABLE public.usuario ADD CONSTRAINT ck_usuario_perfil CHECK(perfil IN ('USUARIO','ANALISTA','GESTOR','ADMINISTRADOR'));
COMMIT;
