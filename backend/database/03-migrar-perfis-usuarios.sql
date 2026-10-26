-- Faça backup antes de executar. Execute uma única vez após revisar os usuários afetados.
BEGIN;
UPDATE public.usuario SET perfil='ANALISTA' WHERE UPPER(TRIM(perfil)) IN ('ANALISTA DE RISCOS','ANALISTA DE COMPLIANCE');
UPDATE public.usuario SET perfil=UPPER(TRIM(perfil));
DO $$ BEGIN
 IF EXISTS (SELECT 1 FROM public.usuario WHERE perfil NOT IN ('USUARIO','ANALISTA','GESTOR','ADMINISTRADOR')) THEN
  RAISE EXCEPTION 'Existem perfis desconhecidos. Revisar antes da migração.';
 END IF;
END $$;
COMMIT;
