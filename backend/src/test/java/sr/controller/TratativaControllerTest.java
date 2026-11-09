
package sr.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.server.ResponseStatusException;

import sr.dto.LoginResponse;
import sr.service.SincronizacaoOcorrenciasService;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * TESTES UNITARIOS - TRATATIVAS
 *
 * Projeto: Sistema de Riscos Financeiros
 *
 * Testa:
 * - Fila de ocorrencias
 * - Historico
 * - Filtros
 * - Atribuicao de responsavel
 * - Solicitacao de parecer
 * - Conclusao
 * - Auditoria
 * - Bloqueio de reabertura
 *
 * Os testes utilizam Mockito.
 * Nao acessam o PostgreSQL real.
 */
@ExtendWith(MockitoExtension.class)
class TratativaControllerTest {

    @Mock
    private JdbcTemplate jdbc;

    /**
     * IMPORTANTE:
     * Utilizar o servico principal.
     *
     * Nao utilizar
     * SincronizacaoOcorrenciasServiceTest.
     */
    @Mock
    private SincronizacaoOcorrenciasService sincronizacao;

    @InjectMocks
    private TratativaController controller;

    private static final long OCORRENCIA_ID = 47L;
    private static final long ANALISTA_ID = 18L;

    /**
     * Simula a autenticacao de um analista.
     */
    private MockHttpServletRequest requisicaoAnalista() {

        MockHttpServletRequest request =
                new MockHttpServletRequest();

        request.getSession().setAttribute(
                "usuarioLogado",
                new LoginResponse(
                        ANALISTA_ID,
                        "Analista Teste",
                        "analista@exemplo.test",
                        "ANALISTA"
                )
        );

        return request;
    }

    /**
     * Executa a listagem de ocorrencias
     * com parametros de teste.
     */
    private void listar(
            String visao,
            String inicio,
            String fim) {

        controller.listar(
                visao,
                null,
                null,
                inicio,
                fim,
                null,
                null,
                null
        );
    }

    /**
     * Prepara dados simulados de uma ocorrencia.
     *
     * O resultado nao vem do banco real.
     */
    private void prepararConsultas(
            String status,
            String categoria,
            Long responsavelId) {

        Map<String, Object> bloqueado =
                new HashMap<>();

        bloqueado.put("id", OCORRENCIA_ID);
        bloqueado.put("status", status);
        bloqueado.put("categoria", categoria);
        bloqueado.put(
                "responsavel_id",
                responsavelId
        );

        Map<String, Object> atualizado =
                Map.of(
                        "id", OCORRENCIA_ID,
                        "status", "EM_ANALISE",
                        "categoria", categoria
                );

        doAnswer(invocation -> {

            String sql = invocation.getArgument(0);

            if (sql.contains("FOR UPDATE")) {
                return List.of(bloqueado);
            }

            if (sql.contains("WHERE t.id=?")) {
                return List.of(atualizado);
            }

            if (sql.contains(
                    "FROM public.ocorrencia_historico")) {

                return List.of(
                        Map.of(
                                "id", 1L,
                                "ocorrencia_id",
                                OCORRENCIA_ID,
                                "executor_id",
                                ANALISTA_ID,
                                "acao", "ASSUMIR",
                                "status_novo",
                                "EM_ANALISE"
                        )
                );
            }

            return List.of();

        }).when(jdbc).queryForList(
                anyString(),
                eq(OCORRENCIA_ID)
        );
    }

    /**
     * TESTE 1
     *
     * Ocorrencias concluidas nao devem
     * aparecer na fila operacional.
     */
    @Test
    void rf40_filaNaoDeveExibirOcorrenciasConcluidas() {

        listar("FILA", null, null);

        ArgumentCaptor<String> sql =
                ArgumentCaptor.forClass(String.class);

        verify(jdbc).queryForList(
                sql.capture(),
                any(Object[].class)
        );

        assertTrue(
                sql.getValue().contains(
                        "t.status<>'CONCLUIDA'"
                )
        );
    }

    /**
     * TESTE 2
     *
     * A visao de historico deve listar
     * somente ocorrencias concluidas.
     */
    @Test
    void rf40_historicoDeveExibirSomenteConcluidas() {

        listar("HISTORICO", null, null);

        ArgumentCaptor<String> sql =
                ArgumentCaptor.forClass(String.class);

        verify(jdbc).queryForList(
                sql.capture(),
                any(Object[].class)
        );

        assertTrue(
                sql.getValue().contains(
                        "t.status='CONCLUIDA'"
                )
        );
    }

    /**
     * TESTE 3
     *
     * Rejeita periodo com data inicial
     * maior que a data final.
     */
    @Test
    void rf29_deveRejeitarPeriodoComDataInicialMaiorQueFinal() {

        ResponseStatusException erro =
                assertThrows(
                        ResponseStatusException.class,
                        () -> listar(
                                "TODAS",
                                "2026-11-10",
                                "2026-11-08"
                        )
                );

        assertEquals(
                HttpStatus.BAD_REQUEST,
                erro.getStatusCode()
        );

        verifyNoInteractions(jdbc);
    }

    /**
     * TESTE 4
     *
     * Rejeita uma visao desconhecida.
     */
    @Test
    void rf40_deveRejeitarVisaoDesconhecida() {

        ResponseStatusException erro =
                assertThrows(
                        ResponseStatusException.class,
                        () -> listar(
                                "QUALQUER",
                                null,
                                null
                        )
                );

        assertEquals(
                HttpStatus.BAD_REQUEST,
                erro.getStatusCode()
        );

        verifyNoInteractions(jdbc);
    }

    /**
     * TESTE 5
     *
     * Verifica se o analista assume
     * a ocorrencia e registra o historico.
     */
    @Test
    void rf37_deveAssumirOcorrenciaERegistrarHistorico() {

        prepararConsultas(
                "PENDENTE",
                "PLD",
                null
        );

        MockHttpServletRequest request =
                requisicaoAnalista();

        Map<String, Object> resultado =
                controller.assumir(
                        OCORRENCIA_ID,
                        request
                );

        assertNotNull(resultado);

        verify(jdbc).update(
                argThat(sql ->
                        sql.contains(
                                "UPDATE public.ocorrencia_tratativa"
                        )
                        && sql.contains(
                                "status='EM_ANALISE'"
                        )
                        && sql.contains(
                                "responsavel_id=?"
                        )
                ),
                eq(ANALISTA_ID),
                eq(OCORRENCIA_ID)
        );

        verify(jdbc).update(
                contains(
                        "INSERT INTO public.ocorrencia_historico"
                ),
                eq(OCORRENCIA_ID),
                eq(ANALISTA_ID),
                isNull(),
                eq(ANALISTA_ID),
                eq("PENDENTE"),
                eq("EM_ANALISE"),
                eq("ASSUMIR"),
                isNull(),
                isNull()
        );
    }

    /**
     * TESTE 6
     *
     * Verifica a solicitacao de parecer
     * e seu registro no historico.
     */
    @Test
    void rf38_deveSolicitarParecerERegistrarHistorico() {

        prepararConsultas(
                "EM_ANALISE",
                "FRAUDE",
                ANALISTA_ID
        );

        MockHttpServletRequest request =
                requisicaoAnalista();

        controller.parecer(
                OCORRENCIA_ID,
                Map.of(
                        "justificativa",
                        "Necessária análise complementar"
                ),
                request
        );

        verify(jdbc).update(
                argThat(sql ->
                        sql.contains(
                                "UPDATE public.ocorrencia_tratativa"
                        )
                        && sql.contains(
                                "status='AGUARDANDO_PARECER'"
                        )
                ),
                eq("Necessária análise complementar"),
                eq(OCORRENCIA_ID)
        );

        verify(jdbc).update(
                contains(
                        "INSERT INTO public.ocorrencia_historico"
                ),
                eq(OCORRENCIA_ID),
                eq(ANALISTA_ID),
                eq(ANALISTA_ID),
                eq(ANALISTA_ID),
                eq("EM_ANALISE"),
                eq("AGUARDANDO_PARECER"),
                eq("SOLICITAR_PARECER"),
                isNull(),
                eq("Necessária análise complementar")
        );
    }

    /**
     * TESTE 7
     *
     * Verifica a conclusao e a decisao
     * registrada pelo analista.
     */
    @Test
    void rf23_deveConcluirOcorrenciaERegistrarDecisao() {

        prepararConsultas(
                "EM_ANALISE",
                "PLD",
                ANALISTA_ID
        );

        MockHttpServletRequest request =
                requisicaoAnalista();

        controller.concluir(
                OCORRENCIA_ID,
                Map.of(
                        "decisao", "PROCEDENTE",
                        "justificativa",
                        "Evidências analisadas e confirmadas"
                ),
                request
        );

        verify(jdbc).update(
                argThat(sql ->
                        sql.contains(
                                "UPDATE public.ocorrencia_tratativa"
                        )
                        && sql.contains(
                                "status='CONCLUIDA'"
                        )
                        && sql.contains(
                                "concluido_em=CURRENT_TIMESTAMP"
                        )
                ),
                eq("PROCEDENTE"),
                eq("Evidências analisadas e confirmadas"),
                eq(OCORRENCIA_ID)
        );

        verify(jdbc).update(
                contains(
                        "INSERT INTO public.ocorrencia_historico"
                ),
                eq(OCORRENCIA_ID),
                eq(ANALISTA_ID),
                eq(ANALISTA_ID),
                eq(ANALISTA_ID),
                eq("EM_ANALISE"),
                eq("CONCLUIDA"),
                eq("CONCLUIR"),
                eq("PROCEDENTE"),
                eq("Evidências analisadas e confirmadas")
        );
    }

    /**
     * TESTE 8
     *
     * Verifica a consulta do historico
     * na tabela correta.
     */
    @Test
    void rf24_deveConsultarHistoricoExistente() {

        prepararConsultas(
                "EM_ANALISE",
                "PLD",
                ANALISTA_ID
        );

        List<Map<String, Object>> historico =
                controller.historico(OCORRENCIA_ID);

        assertEquals(1, historico.size());

        ArgumentCaptor<String> sql =
                ArgumentCaptor.forClass(String.class);

        verify(jdbc, atLeastOnce()).queryForList(
                sql.capture(),
                eq(OCORRENCIA_ID)
        );

        assertTrue(
                sql.getAllValues()
                        .stream()
                        .anyMatch(consulta ->
                                consulta.contains(
                                        "FROM public.ocorrencia_historico"
                                )
                        )
        );
    }

    /**
     * TESTE 9
     *
     * Impede que uma ocorrencia concluida
     * seja assumida novamente.
     */
    @Test
    void rn07_naoDevePermitirReabrirOcorrenciaConcluida() {

        prepararConsultas(
                "CONCLUIDA",
                "CHARGEBACK",
                ANALISTA_ID
        );

        MockHttpServletRequest request =
                requisicaoAnalista();

        ResponseStatusException erro =
                assertThrows(
                        ResponseStatusException.class,
                        () -> controller.assumir(
                                OCORRENCIA_ID,
                                request
                        )
                );

        assertEquals(
                HttpStatus.CONFLICT,
                erro.getStatusCode()
        );

        verify(jdbc, never()).update(
                anyString(),
                any(Object[].class)
        );
    }
}
