
package sr.service;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * TESTES UNITARIOS DA SINCRONIZACAO
 *
 * Sistema de Riscos Financeiros
 *
 * Categorias:
 * PLD, FRAUDE, CHARGEBACK e KYC.
 *
 * Objetivos:
 * - Validar a sincronizacao das categorias.
 * - Reconhecer os status historicos.
 * - Verificar a protecao contra duplicidade.
 * - Testar o controle do agendamento.
 *
 * Os testes utilizam Mockito.
 * Nenhuma operacao e realizada no banco real.
 */
@ExtendWith(MockitoExtension.class)
class SincronizacaoOcorrenciasServiceTest {

    @Mock
    private JdbcTemplate jdbc;

    @InjectMocks
    private SincronizacaoOcorrenciasService service;

    /**
     * Metodo auxiliar.
     *
     * Executa a sincronizacao simulada e
     * captura os sete comandos SQL produzidos.
     */
    private List<String> capturarComandosSql() {

        service.atualizar();

        ArgumentCaptor<String> captor =
                ArgumentCaptor.forClass(String.class);

        verify(jdbc, times(7)).update(
                captor.capture()
        );

        return captor.getAllValues();
    }

    /**
     * TESTE 1
     *
     * Verifica se as quatro categorias
     * participam da sincronizacao.
     */
    @Test
    void deveSincronizarAsQuatroCategorias() {

        when(jdbc.update(anyString()))
                .thenReturn(1);

        Map<String, Integer> resultado =
                service.atualizar();

        assertEquals(4, resultado.size());

        assertEquals(1, resultado.get("PLD"));
        assertEquals(1, resultado.get("FRAUDE"));
        assertEquals(1, resultado.get("CHARGEBACK"));
        assertEquals(1, resultado.get("KYC"));

        verify(jdbc, times(7))
                .update(anyString());
    }

    /**
     * TESTE 2
     *
     * Verifica os quatro status definitivos
     * das analises KYC.
     */
    @Test
    void deveReconhecerStatusHistoricosKyc() {

        List<String> comandos =
                capturarComandosSql();

        String sqlKyc = comandos.stream()
                .filter(sql ->
                        sql.contains(
                                "FROM public.fato_kyc k"
                        )
                )
                .findFirst()
                .orElseThrow();

        assertTrue(sqlKyc.contains("'APROVADO'"));
        assertTrue(sqlKyc.contains("'REPROVADO'"));
        assertTrue(sqlKyc.contains("'ACEITO'"));
        assertTrue(sqlKyc.contains("'NAO_ACEITO'"));

        assertTrue(
                sqlKyc.contains("THEN 'CONCLUIDA'")
        );

        assertTrue(
                sqlKyc.contains("ELSE 'PENDENTE'")
        );
    }

    /**
     * TESTE 3
     *
     * Verifica se o codigo evita inventar
     * uma data de conclusao historica.
     */
    @Test
    void naoDeveInventarDataDeConclusaoHistorica() {

        List<String> comandos =
                capturarComandosSql();

        String sqlKyc = comandos.stream()
                .filter(sql ->
                        sql.contains(
                                "FROM public.fato_kyc k"
                        )
                )
                .findFirst()
                .orElseThrow();

        String sqlNormalizado =
                sqlKyc.replaceAll("\\s+", " ");

        assertTrue(
                sqlNormalizado.contains(
                        "k.motivo_decisao, NULL FROM public.fato_kyc"
                )
        );
    }

    /**
     * TESTE 4
     *
     * Verifica o reconhecimento do status
     * FECHADO nas categorias PLD e FRAUDE.
     */
    @Test
    void deveReconhecerAlertasHistoricosFechados() {

        List<String> comandos =
                capturarComandosSql();

        String sqlPld = comandos.stream()
                .filter(sql ->
                        sql.contains(
                                "FROM public.fato_alerta_pld p"
                        )
                        && sql.contains(
                                "INSERT INTO public.ocorrencia_tratativa"
                        )
                )
                .findFirst()
                .orElseThrow();

        String sqlFraude = comandos.stream()
                .filter(sql ->
                        sql.contains(
                                "FROM public.fato_alerta_fraude f"
                        )
                        && sql.contains(
                                "INSERT INTO public.ocorrencia_tratativa"
                        )
                )
                .findFirst()
                .orElseThrow();

        assertTrue(
                sqlPld.contains("'FECHADO'")
        );

        assertTrue(
                sqlFraude.contains("'FECHADO'")
        );
    }

    /**
     * TESTE 5
     *
     * Verifica se as quatro instrucoes
     * de insercao possuem protecao contra
     * registros duplicados.
     */
    @Test
    void deveEvitarInsercoesDuplicadas() {

        List<String> comandos =
                capturarComandosSql();

        long protegidos = comandos.stream()
                .filter(sql ->
                        sql.contains("ON CONFLICT")
                        && sql.contains("DO NOTHING")
                )
                .count();

        assertEquals(4, protegidos);
    }

    /**
     * TESTE 6
     *
     * A sincronizacao automatica deve
     * permanecer desabilitada quando
     * a configuracao for false.
     */
    @Test
    void naoDeveSincronizarAutomaticamenteQuandoDesabilitado() {

        ReflectionTestUtils.setField(
                service,
                "sincronizacaoAutomatica",
                false
        );

        service.executarAgendado();

        verifyNoInteractions(jdbc);
    }

    /**
     * TESTE 7
     *
     * Verifica se a sincronizacao automatica
     * pode ser habilitada explicitamente.
     */
    @Test
    void devePermitirSincronizacaoAutomaticaQuandoHabilitada() {

        ReflectionTestUtils.setField(
                service,
                "sincronizacaoAutomatica",
                true
        );

        service.executarAgendado();

        verify(jdbc, times(7))
                .update(anyString());
    }
}
