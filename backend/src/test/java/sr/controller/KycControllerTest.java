
package sr.controller;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.server.ResponseStatusException;

import sr.dto.LoginResponse;
import sr.model.Kyc;
import sr.service.KycService;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * TESTES UNITARIOS - KYC
 *
 * Projeto: Sistema de Riscos Financeiros
 *
 * Requisitos:
 * RF35 - Registro do analista responsavel.
 * RF36 - Registro da decisao e parecer tecnico.
 * RF43 - Historico das analises KYC.
 *
 * Os testes usam Mockito.
 * Nenhuma operacao e executada no PostgreSQL real.
 */
@ExtendWith(MockitoExtension.class)
class KycControllerTest {

    @Mock
    private KycService service;

    @Mock
    private JdbcTemplate jdbc;

    @InjectMocks
    private KycController controller;

    /**
     * TESTE 1
     *
     * Verifica que o sistema impede uma decisao
     * quando o parecer tecnico esta vazio.
     */
    @Test
    void rf35_deveRejeitarParecerVazio() {

        MockHttpServletRequest request =
                new MockHttpServletRequest();

        ResponseStatusException erro = assertThrows(
                ResponseStatusException.class,
                () -> controller.decidir(
                        "KYC001",
                        Map.of(
                                "decisao", "ACEITO",
                                "parecer", " "
                        ),
                        request
                )
        );

        assertEquals(
                HttpStatus.BAD_REQUEST,
                erro.getStatusCode()
        );

        // Nao deve consultar nem alterar o banco.
        verifyNoInteractions(jdbc);
    }

    /**
     * TESTE 2
     *
     * Verifica uma recusa KYC:
     *
     * 1. Identifica o analista autenticado.
     * 2. Registra decisao e parecer.
     * 3. Inativa o cliente recusado.
     * 4. Conclui a ocorrencia.
     * 5. Registra o historico auditavel.
     *
     * Utiliza a estrutura existente:
     * public.ocorrencia_historico
     */
    @Test
    void rf35_deveRegistrarResponsavelEInativarClienteRecusado() {

        // --------------------------------------
        // 1. SIMULAR O ANALISTA AUTENTICADO
        // --------------------------------------

        MockHttpServletRequest request =
                new MockHttpServletRequest();

        request.getSession().setAttribute(
                "usuarioLogado",
                new LoginResponse(
                        18L,
                        "Analista Teste",
                        "analista@exemplo.test",
                        "ANALISTA"
                )
        );

        // --------------------------------------
        // 2. SIMULAR A CONSULTA DO KYC
        // --------------------------------------

        when(jdbc.queryForList(
                argThat(sql ->
                        sql.contains("FROM public.fato_kyc")
                        && sql.contains("FOR UPDATE")
                ),
                eq("KYC001")
        )).thenReturn(
                List.of(
                        Map.of(
                                "id_kyc", "KYC001",
                                "id_cliente", "CLI001",
                                "status_kyc", "PENDENTE",
                                "data_analise",
                                LocalDate.of(2026, 11, 1),
                                "nivel_risco_kyc", "ALTO"
                        )
                )
        );

        // --------------------------------------
        // 3. SIMULAR A OCORRENCIA VINCULADA
        // --------------------------------------

        when(jdbc.queryForMap(
                argThat(sql ->
                        sql.contains("public.ocorrencia_tratativa")
                        && sql.contains("origem_id = ?")
                        && sql.contains("FOR UPDATE")
                ),
                eq("KYC001")
        )).thenReturn(
                Map.of(
                        "id", 47L,
                        "status", "PENDENTE",
                        "categoria", "KYC"
                )
        );

        /*
         * O campo responsavel_id nao esta presente
         * no Map acima.
         *
         * Isso representa uma ocorrencia que ainda
         * nao possui analista responsavel.
         */

        // --------------------------------------
        // 4. SIMULAR A INATIVACAO DO CLIENTE
        // --------------------------------------

        /*
         * doReturn evita o problema de stubbing
         * identificado anteriormente.
         *
         * lenient permite que outros comandos
         * jdbc.update sejam executados no teste.
         */
        lenient().doReturn(1).when(jdbc).update(
                argThat(sql ->
                        sql.contains("UPDATE public.dim_cliente")
                        && sql.contains(
                                "status_cliente = 'INATIVO'"
                        )
                ),
                eq("CLI001")
        );

        // --------------------------------------
        // 5. SIMULAR O RETORNO DO SERVICO
        // --------------------------------------

        when(service.buscarPorId("KYC001"))
                .thenReturn(new Kyc());

        // --------------------------------------
        // 6. EXECUTAR A DECISAO
        // --------------------------------------

        Kyc resultado = controller.decidir(
                "KYC001",
                Map.of(
                        "decisao", "NAO_ACEITO",
                        "parecer",
                        "Documentação não validada"
                ),
                request
        );

        assertNotNull(resultado);

        // --------------------------------------
        // 7. VALIDAR A DECISAO KYC
        // --------------------------------------

        verify(jdbc).update(
                argThat(sql ->
                        sql.contains("UPDATE public.fato_kyc")
                        && sql.contains("status_kyc = ?")
                        && sql.contains("motivo_decisao = ?")
                ),
                eq("NAO_ACEITO"),
                eq("Documentação não validada"),
                eq("KYC001")
        );

        // --------------------------------------
        // 8. VALIDAR A INATIVACAO DO CLIENTE
        // --------------------------------------

        verify(jdbc).update(
                argThat(sql ->
                        sql.contains("UPDATE public.dim_cliente")
                        && sql.contains(
                                "status_cliente = 'INATIVO'"
                        )
                ),
                eq("CLI001")
        );

        // --------------------------------------
        // 9. VALIDAR A CONCLUSAO DA OCORRENCIA
        // --------------------------------------

        verify(jdbc).update(
                argThat(sql ->
                        sql.contains(
                                "UPDATE public.ocorrencia_tratativa"
                        )
                        && sql.contains(
                                "status = 'CONCLUIDA'"
                        )
                        && sql.contains(
                                "responsavel_id = ?"
                        )
                ),
                eq(18L),
                eq("NAO_ACEITO"),
                eq("Documentação não validada"),
                eq(47L)
        );

        // --------------------------------------
        // 10. VALIDAR O HISTORICO
        // --------------------------------------

        /*
         * Verifica a tabela atual do PostgreSQL:
         *
         * ocorrencia_historico
         *
         * Responsavel anterior: NULL
         * Responsavel novo: 18
         * Status anterior: PENDENTE
         * Status novo: CONCLUIDA
         */

        verify(jdbc).update(
                argThat(sql ->
                        sql.contains(
                                "INSERT INTO public.ocorrencia_historico"
                        )
                        && sql.contains(
                                "responsavel_anterior_id"
                        )
                        && sql.contains(
                                "responsavel_novo_id"
                        )
                        && sql.contains(
                                "'CONCLUIDA'"
                        )
                        && sql.contains(
                                "'DECISAO_KYC'"
                        )
                ),
                eq(47L),
                eq(18L),
                isNull(),
                eq(18L),
                eq("PENDENTE"),
                eq("NAO_ACEITO"),
                eq("Documentação não validada")
        );

        // --------------------------------------
        // 11. VALIDAR O RETORNO FINAL
        // --------------------------------------

        verify(service).buscarPorId("KYC001");
    }
}
