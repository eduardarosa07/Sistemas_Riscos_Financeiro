-- Carga de teste sintética: 60 clientes novos; execução manual e idempotente.
-- Execute somente em ambiente de desenvolvimento/homologação, após criar as tabelas base.
BEGIN;
INSERT INTO public.dim_cliente(id_cliente,tipo_pessoa,nome_razao_social,documento_ficticio,data_nascimento_abertura,cidade,uf,data_cadastro,status_cliente,segmento,renda_faturamento_mensal,profissao_atividade,origem_cadastro)
SELECT 'KYCTEST'||LPAD(i::text,4,'0'),CASE WHEN i%5=0 THEN 'PJ' ELSE 'PF' END,
 'Cliente Sintético KYC '||LPAD(i::text,3,'0'),'DOC-FICT-KYC-'||LPAD(i::text,4,'0'),DATE '1990-01-01'+i,
 (ARRAY['Goiânia','São Paulo','Rio de Janeiro','Brasília','Uberlândia'])[1+(i%5)],
 (ARRAY['GO','SP','RJ','DF','MG'])[1+(i%5)],CURRENT_DATE,'ATIVO','TESTE_KYC',2500+i*150,'Atividade simulada','CARGA_TESTE'
FROM generate_series(1,60) AS i ON CONFLICT(id_cliente) DO NOTHING;
INSERT INTO public.fato_kyc(id_kyc,id_cliente,data_analise,documento_validado,biometria_validada,endereco_validado,telefone_validado,email_validado,pep,lista_restritiva,midia_adversa,beneficiario_final_identificado,score_kyc,nivel_risco_kyc,status_kyc,motivo_decisao)
SELECT 'KYCTESTAN'||LPAD(i::text,4,'0'),'KYCTEST'||LPAD(i::text,4,'0'),CURRENT_DATE,
 (i%3<>0),(i%4<>0),(i%5<>0),(i%6<>0),(i%7<>0),false,false,false,true,
 20+i%70,CASE WHEN i%3=0 THEN 'ALTO' WHEN i%3=1 THEN 'BAIXO' ELSE 'MEDIO' END,
 CASE WHEN i<=20 THEN 'PENDENTE' WHEN i<=35 THEN 'EM_ANALISE' WHEN i<=45 THEN 'DOCUMENTACAO_PENDENTE' WHEN i<=55 THEN 'AGUARDANDO_PARECER' ELSE 'REVISAO_COMPLEMENTAR' END,
 'Registro sintético para teste; não representa análise real'
FROM generate_series(1,60) AS i ON CONFLICT(id_kyc) DO NOTHING;
COMMIT;
