
package sr.service;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * SERVICO DE SINCRONIZACAO DE OCORRENCIAS
 *
 * Sistema de Riscos Financeiros
 *
 * Categorias:
 * - PLD
 * - FRAUDE
 * - CHARGEBACK
 * - KYC
 *
 * Objetivos:
 * - Sincronizar novas ocorrencias.
 * - Evitar duplicidade.
 * - Reconhecer status historicos encerrados.
 * - Preservar decisoes e responsaveis existentes.
 * - Controlar a sincronizacao automatica.
 *
 * Durante a homologacao, o agendamento automatico
 * permanece desabilitado por padrao.
 */
@Service
public class SincronizacaoOcorrenciasService {

    private final JdbcTemplate jdbc;

    /**
     * Configuracao da sincronizacao automatica.
     *
     * Valor padrao: false.
     *
     * A sincronizacao automatica somente sera
     * executada quando explicitamente habilitada.
     */
    @Value("${sgr.sincronizacao.automatica:false}")
    private boolean sincronizacaoAutomatica;

    public SincronizacaoOcorrenciasService(
            JdbcTemplate jdbc) {

        this.jdbc = jdbc;
    }

    /**
     * Sincroniza as quatro categorias.
     *
     * @Transactional permite reverter a operacao
     * se ocorrer um erro durante a execucao
     * gerenciada pelo Spring.
     *
     * ON CONFLICT DO NOTHING impede duplicidade
     * das ocorrencias ja cadastradas.
     *
     * Os UPDATEs finais preenchem somente
     * produtos que ainda estao nulos.
     */
    @Transactional
    public Map<String, Integer> atualizar() {

        Map<String, Integer> inseridas =
                new LinkedHashMap<>();

        // ========================================
        // 1. SINCRONIZAR PLD
        // ========================================

        /*
         * FECHADO representa uma situacao
         * historica de encerramento.
         */
        inseridas.put("PLD", jdbc.update("""
            INSERT INTO public.ocorrencia_tratativa
                (
                    categoria,
                    origem_id,
                    status,
                    cliente_id,
                    data_ocorrencia,
                    valor,
                    risco
                )
            SELECT
                'PLD',
                p.id_alerta_pld,

                CASE
                    WHEN UPPER(
                        TRIM(COALESCE(p.status_analise, ''))
                    ) IN (
                        'CONCLUIDA',
                        'CONCLUIDO',
                        'FINALIZADO',
                        'ENCERRADO',
                        'FECHADO'
                    )
                    THEN 'CONCLUIDA'
                    ELSE 'PENDENTE'
                END,

                p.id_cliente_analisado,
                p.data_alerta,
                p.valor_relacionado,
                p.severidade

            FROM public.fato_alerta_pld p

            ON CONFLICT (categoria, origem_id)
            DO NOTHING
            """));

        // ========================================
        // 2. SINCRONIZAR FRAUDE
        // ========================================

        /*
         * Reconhece os registros historicos
         * que ja foram encerrados.
         */
        inseridas.put("FRAUDE", jdbc.update("""
            INSERT INTO public.ocorrencia_tratativa
                (
                    categoria,
                    origem_id,
                    status,
                    cliente_id,
                    data_ocorrencia,
                    valor,
                    risco
                )
            SELECT
                'FRAUDE',
                f.id_alerta_fraude,

                CASE
                    WHEN UPPER(
                        TRIM(COALESCE(f.status_analise, ''))
                    ) IN (
                        'CONCLUIDA',
                        'CONCLUIDO',
                        'FINALIZADO',
                        'ENCERRADO',
                        'FECHADO'
                    )
                    THEN 'CONCLUIDA'
                    ELSE 'PENDENTE'
                END,

                f.id_cliente_origem,
                f.data_alerta,
                f.valor_exposto,
                f.severidade

            FROM public.fato_alerta_fraude f

            ON CONFLICT (categoria, origem_id)
            DO NOTHING
            """));

        // ========================================
        // 3. SINCRONIZAR CHARGEBACK
        // ========================================

        /*
         * Reconhece ENCERRADO, RESOLVIDO
         * e demais status definitivos.
         */
        inseridas.put("CHARGEBACK", jdbc.update("""
            INSERT INTO public.ocorrencia_tratativa
                (
                    categoria,
                    origem_id,
                    status,
                    cliente_id,
                    data_ocorrencia,
                    valor
                )
            SELECT
                'CHARGEBACK',
                c.id_chargeback,

                CASE
                    WHEN UPPER(
                        TRIM(COALESCE(c.status_chargeback, ''))
                    ) IN (
                        'CONCLUIDA',
                        'CONCLUIDO',
                        'FINALIZADO',
                        'ENCERRADO',
                        'FECHADO',
                        'RESOLVIDO'
                    )
                    THEN 'CONCLUIDA'
                    ELSE 'PENDENTE'
                END,

                c.id_cliente,
                c.data_transacao,
                c.valor_contestado

            FROM public.fato_chargeback c

            ON CONFLICT (categoria, origem_id)
            DO NOTHING
            """));

        // ========================================
        // 4. SINCRONIZAR KYC
        // ========================================

        /*
         * COMPATIBILIDADE HISTORICA
         *
         * Status definitivos:
         * - ACEITO
         * - NAO_ACEITO
         * - APROVADO
         * - REPROVADO
         *
         * Todos sao considerados CONCLUIDA.
         *
         * Somente ACEITO e NAO_ACEITO entram
         * diretamente na coluna decisao.
         *
         * APROVADO e REPROVADO permanecem
         * preservados em fato_kyc.
         *
         * A data_analise nao sera usada para
         * inventar a data de conclusao.
         */
        inseridas.put("KYC", jdbc.update("""
            INSERT INTO public.ocorrencia_tratativa
                (
                    categoria,
                    origem_id,
                    status,
                    cliente_id,
                    data_ocorrencia,
                    risco,
                    decisao,
                    justificativa,
                    concluido_em
                )
            SELECT
                'KYC',
                k.id_kyc,

                CASE
                    WHEN UPPER(
                        TRIM(COALESCE(k.status_kyc, ''))
                    ) IN (
                        'ACEITO',
                        'NAO_ACEITO',
                        'APROVADO',
                        'REPROVADO'
                    )
                    THEN 'CONCLUIDA'
                    ELSE 'PENDENTE'
                END,

                k.id_cliente,
                k.data_analise::timestamp,
                k.nivel_risco_kyc,

                CASE
                    WHEN UPPER(
                        TRIM(COALESCE(k.status_kyc, ''))
                    ) IN (
                        'ACEITO',
                        'NAO_ACEITO'
                    )
                    THEN UPPER(TRIM(k.status_kyc))
                    ELSE NULL
                END,

                k.motivo_decisao,
                NULL

            FROM public.fato_kyc k

            ON CONFLICT (categoria, origem_id)
            DO NOTHING
            """));

        // ========================================
        // 5. COMPLEMENTAR PRODUTO - PLD
        // ========================================

        /*
         * Preenche produto somente quando NULL.
         *
         * Nao altera:
         * - Status
         * - Decisao
         * - Justificativa
         * - Responsavel
         */
        jdbc.update("""
            UPDATE public.ocorrencia_tratativa o
            SET produto = tr.tipo_transacao

            FROM public.fato_alerta_pld p
            JOIN public.fato_transacao tr
                ON tr.id_transacao =
                   p.id_transacao_referencia

            WHERE o.categoria = 'PLD'
              AND o.origem_id = p.id_alerta_pld
              AND o.produto IS NULL
            """);

        // ========================================
        // 6. COMPLEMENTAR PRODUTO - FRAUDE
        // ========================================

        jdbc.update("""
            UPDATE public.ocorrencia_tratativa o
            SET produto = tr.tipo_transacao

            FROM public.fato_alerta_fraude f
            JOIN public.fato_transacao tr
                ON tr.id_transacao = f.id_transacao

            WHERE o.categoria = 'FRAUDE'
              AND o.origem_id = f.id_alerta_fraude
              AND o.produto IS NULL
            """);

        // ========================================
        // 7. COMPLEMENTAR PRODUTO - CHARGEBACK
        // ========================================

        jdbc.update("""
            UPDATE public.ocorrencia_tratativa o
            SET produto = tr.tipo_transacao

            FROM public.fato_chargeback c
            JOIN public.fato_transacao tr
                ON tr.id_transacao = c.id_transacao

            WHERE o.categoria = 'CHARGEBACK'
              AND o.origem_id = c.id_chargeback
              AND o.produto IS NULL
            """);

        // Retorna as quantidades inseridas.
        return inseridas;
    }

    // ==========================================
    // 8. SINCRONIZACAO AUTOMATICA
    // ==========================================

    /**
     * Controla a execucao periodica.
     *
     * Por padrao:
     * sincronizacaoAutomatica = false
     *
     * Quando estiver false, a rotina nao
     * executa os comandos SQL.
     *
     * IMPORTANTE:
     * O agendamento interno ainda precisara
     * ser revisado quanto ao limite transacional
     * antes de ser habilitado em producao.
     */
    @Scheduled(
        fixedDelayString =
            "${sgr.sincronizacao.intervalo-ms:60000}",
        initialDelayString =
            "${sgr.sincronizacao.inicio-ms:60000}"
    )
    public void executarAgendado() {

        if (!sincronizacaoAutomatica) {
            return;
        }

        atualizar();
    }
}