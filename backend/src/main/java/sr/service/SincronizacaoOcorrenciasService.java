package sr.service;

import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Sincronização incremental idempotente: preserva decisões e eventos existentes. */
@Service
public class SincronizacaoOcorrenciasService {
    private final JdbcTemplate jdbc;
    public SincronizacaoOcorrenciasService(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    @Transactional
    public Map<String,Integer> atualizar() {
        Map<String,Integer> inseridas = new LinkedHashMap<>();
        inseridas.put("PLD", jdbc.update("""
            INSERT INTO public.ocorrencia_tratativa
                (categoria,origem_id,status,cliente_id,data_ocorrencia,valor,risco)
            SELECT 'PLD',p.id_alerta_pld,
              CASE WHEN UPPER(COALESCE(p.status_analise,'')) IN
                 ('CONCLUIDA','CONCLUIDO','CONCLUÍDA','FINALIZADO','ENCERRADO')
                 THEN 'CONCLUIDA' ELSE 'PENDENTE' END,
              p.id_cliente_analisado,p.data_alerta,p.valor_relacionado,p.severidade
            FROM public.fato_alerta_pld p
            ON CONFLICT(categoria,origem_id) DO NOTHING
            """));
        inseridas.put("FRAUDE", jdbc.update("""
            INSERT INTO public.ocorrencia_tratativa
                (categoria,origem_id,status,cliente_id,data_ocorrencia,valor,risco)
            SELECT 'FRAUDE',f.id_alerta_fraude,
              CASE WHEN UPPER(COALESCE(f.status_analise,'')) IN
                 ('CONCLUIDA','CONCLUIDO','CONCLUÍDA','FINALIZADO','ENCERRADO')
                 THEN 'CONCLUIDA' ELSE 'PENDENTE' END,
              f.id_cliente_origem,f.data_alerta,f.valor_exposto,f.severidade
            FROM public.fato_alerta_fraude f
            ON CONFLICT(categoria,origem_id) DO NOTHING
            """));
        inseridas.put("CHARGEBACK", jdbc.update("""
            INSERT INTO public.ocorrencia_tratativa
                (categoria,origem_id,status,cliente_id,data_ocorrencia,valor)
            SELECT 'CHARGEBACK',c.id_chargeback,
              CASE WHEN UPPER(COALESCE(c.status_chargeback,'')) IN
                 ('CONCLUIDA','CONCLUIDO','CONCLUÍDA','FINALIZADO','ENCERRADO','RESOLVIDO')
                 THEN 'CONCLUIDA' ELSE 'PENDENTE' END,
              c.id_cliente,c.data_transacao,c.valor_contestado
            FROM public.fato_chargeback c
            ON CONFLICT(categoria,origem_id) DO NOTHING
            """));
        inseridas.put("KYC", jdbc.update("""
            INSERT INTO public.ocorrencia_tratativa
                (categoria,origem_id,status,cliente_id,data_ocorrencia,risco,decisao,justificativa,concluido_em)
            SELECT 'KYC',k.id_kyc,
              CASE WHEN UPPER(COALESCE(k.status_kyc,'')) IN ('ACEITO','NAO_ACEITO')
                  THEN 'CONCLUIDA' ELSE 'PENDENTE' END,
              k.id_cliente,k.data_analise::timestamp,k.nivel_risco_kyc,
              CASE WHEN UPPER(COALESCE(k.status_kyc,'')) IN ('ACEITO','NAO_ACEITO')
                  THEN UPPER(k.status_kyc) ELSE NULL END,
              k.motivo_decisao,
              CASE WHEN UPPER(COALESCE(k.status_kyc,'')) IN ('ACEITO','NAO_ACEITO')
                  THEN k.data_analise::timestamp ELSE NULL END
            FROM public.fato_kyc k
            ON CONFLICT(categoria,origem_id) DO NOTHING
            """));
        // Tipo da transação vinculada: dado real para os filtros gerenciais de produto.
        jdbc.update("""
            UPDATE public.ocorrencia_tratativa o SET produto=tr.tipo_transacao
            FROM public.fato_alerta_pld p JOIN public.fato_transacao tr ON tr.id_transacao=p.id_transacao_referencia
            WHERE o.categoria='PLD' AND o.origem_id=p.id_alerta_pld AND o.produto IS NULL
            """);
        jdbc.update("""
            UPDATE public.ocorrencia_tratativa o SET produto=tr.tipo_transacao
            FROM public.fato_alerta_fraude f JOIN public.fato_transacao tr ON tr.id_transacao=f.id_transacao
            WHERE o.categoria='FRAUDE' AND o.origem_id=f.id_alerta_fraude AND o.produto IS NULL
            """);
        jdbc.update("""
            UPDATE public.ocorrencia_tratativa o SET produto=tr.tipo_transacao
            FROM public.fato_chargeback c JOIN public.fato_transacao tr ON tr.id_transacao=c.id_transacao
            WHERE o.categoria='CHARGEBACK' AND o.origem_id=c.id_chargeback AND o.produto IS NULL
            """);
        return inseridas;
    }

    // A rotina se executa periodicamente; a integração não depende de carga única.
    @Scheduled(fixedDelayString = "${sgr.sincronizacao.intervalo-ms:60000}",
               initialDelayString = "${sgr.sincronizacao.inicio-ms:60000}")
    public void executarAgendado() { atualizar(); }
}
