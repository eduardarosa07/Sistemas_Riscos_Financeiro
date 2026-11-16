-- Diagnóstico sem alteração de schema/dados e sem exibir dados pessoais.
-- psql -X -w -h localhost -U postgres -d sistema_riscos -v ON_ERROR_STOP=1 -f scripts/validar_postgresql_leitura.sql
BEGIN READ ONLY;
SET LOCAL statement_timeout = '15s';

SELECT current_database() AS banco, current_setting('server_version') AS versao,
       current_setting('transaction_read_only') AS somente_leitura;

SELECT table_name FROM information_schema.tables
WHERE table_schema='public' ORDER BY table_name;

SELECT 'clientes' AS conjunto, count(*) AS total FROM dim_cliente
UNION ALL SELECT 'kyc', count(*) FROM fato_kyc
UNION ALL SELECT 'riscos', count(*) FROM fato_risco_cliente
UNION ALL SELECT 'ocorrencias', count(*) FROM ocorrencia_tratativa
UNION ALL SELECT 'historico', count(*) FROM ocorrencia_historico;

SELECT upper(trim(status_kyc)) AS status, count(*) AS total
FROM fato_kyc GROUP BY 1 ORDER BY 1;

SELECT count(*) AS kyc_encerrado_em_fila
FROM ocorrencia_tratativa o JOIN fato_kyc k ON k.id_kyc=o.origem_id
WHERE o.categoria='KYC' AND o.status<>'CONCLUIDA'
  AND upper(trim(k.status_kyc)) IN ('ACEITO','NAO_ACEITO','APROVADO','REPROVADO');

SELECT count(*) AS kyc_sem_ocorrencia FROM fato_kyc k
WHERE NOT EXISTS (SELECT 1 FROM ocorrencia_tratativa o WHERE o.categoria='KYC' AND o.origem_id=k.id_kyc);

SELECT count(*) AS historicos_orfaos
FROM ocorrencia_historico h
LEFT JOIN ocorrencia_tratativa o ON o.id=h.ocorrencia_id
LEFT JOIN usuario u ON u.id=h.executor_id
WHERE o.id IS NULL OR u.id IS NULL;

SELECT count(*) AS clientes_sem_risco
FROM dim_cliente c LEFT JOIN fato_risco_cliente r ON r.id_cliente=c.id_cliente
WHERE r.id_cliente IS NULL;

SELECT count(*) AS riscos_sem_cliente
FROM fato_risco_cliente r LEFT JOIN dim_cliente c ON c.id_cliente=r.id_cliente
WHERE c.id_cliente IS NULL;

SELECT count(*) AS emails_normalizados_duplicados FROM (
  SELECT lower(trim(email)) FROM usuario
  GROUP BY lower(trim(email)) HAVING count(*)>1
) duplicados;

-- Conclusões legadas podem não ter decisão/instante conhecidos. Não inventar valores.
SELECT categoria, count(*) AS concluidas_sem_decisao
FROM ocorrencia_tratativa WHERE status='CONCLUIDA' AND decisao IS NULL GROUP BY categoria;

SELECT c.relname AS tabela, t.tgname AS trigger, pg_get_triggerdef(t.oid) AS definicao
FROM pg_trigger t JOIN pg_class c ON c.oid=t.tgrelid
JOIN pg_namespace n ON n.oid=c.relnamespace
WHERE n.nspname='public' AND NOT t.tgisinternal;

ROLLBACK;
