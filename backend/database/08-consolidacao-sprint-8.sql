-- SGR - Migração 08: amplia ocorrências para incluir análises KYC.
-- Pré-requisitos: scripts 01 a 07 conforme aplicáveis; realizar backup antes de executar.
-- Esta migração não apaga ocorrências nem eventos anteriores.
BEGIN;
DO $$
DECLARE c record;
BEGIN
  -- A constraint criada pelo script 06 pode ter nome gerado automaticamente.
  FOR c IN SELECT conname FROM pg_constraint
     WHERE conrelid = 'public.ocorrencia_tratativa'::regclass
       AND contype='c' AND pg_get_constraintdef(oid) ILIKE '%categoria%'
  LOOP
    EXECUTE format('ALTER TABLE public.ocorrencia_tratativa DROP CONSTRAINT %I', c.conname);
  END LOOP;
  ALTER TABLE public.ocorrencia_tratativa
    ADD CONSTRAINT ck_ocorrencia_categoria
      CHECK (categoria IN ('KYC','PLD','FRAUDE','CHARGEBACK'));
END $$;

INSERT INTO public.ocorrencia_tratativa
  (categoria,origem_id,status,cliente_id,data_ocorrencia,risco,decisao,justificativa,concluido_em)
SELECT 'KYC', k.id_kyc,
  CASE WHEN UPPER(COALESCE(k.status_kyc,'')) IN ('ACEITO','NAO_ACEITO')
       THEN 'CONCLUIDA' ELSE 'PENDENTE' END,
  k.id_cliente, k.data_analise::timestamp, k.nivel_risco_kyc,
  CASE WHEN UPPER(COALESCE(k.status_kyc,'')) IN ('ACEITO','NAO_ACEITO')
       THEN UPPER(k.status_kyc) ELSE NULL END,
  k.motivo_decisao,
  CASE WHEN UPPER(COALESCE(k.status_kyc,'')) IN ('ACEITO','NAO_ACEITO')
       THEN k.data_analise::timestamp ELSE NULL END
FROM public.fato_kyc k
ON CONFLICT(categoria,origem_id) DO NOTHING;

CREATE INDEX IF NOT EXISTS idx_ocorrencia_cliente ON public.ocorrencia_tratativa(cliente_id);
CREATE INDEX IF NOT EXISTS idx_ocorrencia_responsavel ON public.ocorrencia_tratativa(responsavel_id);
CREATE INDEX IF NOT EXISTS idx_ocorrencia_decisao ON public.ocorrencia_tratativa(decisao);
-- Produto é derivado do tipo de transação correspondente (apenas quando ainda não informado).
UPDATE public.ocorrencia_tratativa o SET produto=tr.tipo_transacao
FROM public.fato_alerta_pld p JOIN public.fato_transacao tr ON tr.id_transacao=p.id_transacao_referencia
WHERE o.categoria='PLD' AND o.origem_id=p.id_alerta_pld AND o.produto IS NULL;
UPDATE public.ocorrencia_tratativa o SET produto=tr.tipo_transacao
FROM public.fato_alerta_fraude f JOIN public.fato_transacao tr ON tr.id_transacao=f.id_transacao
WHERE o.categoria='FRAUDE' AND o.origem_id=f.id_alerta_fraude AND o.produto IS NULL;
UPDATE public.ocorrencia_tratativa o SET produto=tr.tipo_transacao
FROM public.fato_chargeback c JOIN public.fato_transacao tr ON tr.id_transacao=c.id_transacao
WHERE o.categoria='CHARGEBACK' AND o.origem_id=c.id_chargeback AND o.produto IS NULL;
COMMIT;
