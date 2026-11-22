package sr.service;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class IndicadoresServiceTest {

    private final JdbcTemplate db = mock(JdbcTemplate.class);
    private final IndicadoresService service = new IndicadoresService(db);

    @Test
    void rf29DeveRecusarDatasInvertidas() {
        ResponseStatusException erro = assertThrows(
            ResponseStatusException.class,
            () -> service.ocorrencias(
                "2026-12-05", "2026-12-01", null, null, null
            )
        );

        assertEquals(HttpStatus.BAD_REQUEST, erro.getStatusCode());
        verifyNoInteractions(db);
    }

    @Test
    void rf29DeveRecusarFormatoDataInvalido() {
        assertThrows(
            ResponseStatusException.class,
            () -> service.ocorrencias(
                "01/11/2026", null, null, null, null
            )
        );

        verifyNoInteractions(db);
    }

    @Test
    void rf30DeveRecusarStatusDesconhecido() {
        assertThrows(
            ResponseStatusException.class,
            () -> service.ocorrencias(
                null, null, "INVÁLIDO", null, null
            )
        );

        verifyNoInteractions(db);
    }

    @Test
    void rf31DeveRecusarCategoriaDesconhecida() {
        assertThrows(
            ResponseStatusException.class,
            () -> service.ocorrencias(
                null, null, null, "OUTRA", null
            )
        );

        verifyNoInteractions(db);
    }

    @Test
    void rf32DeveRecusarProdutoVazioNaLista() {
        assertThrows(
            ResponseStatusException.class,
            () -> service.ocorrencias(
                null, null, null, null, "PIX,,TED"
            )
        );

        verifyNoInteractions(db);
    }

    @Test
    void rf33DeveAplicarStatusECategoriaCombinados() {
        when(db.queryForMap(anyString(), any(Object[].class)))
            .thenReturn(Map.of(
                "total", 2L,
                "concluidas", 1L,
                "dia", 1L,
                "mes", 2L
            ));

        service.ocorrencias(
            null,
            null,
            "PENDENTE,CONCLUIDA",
            "PLD,FRAUDE",
            null
        );

        ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
        verify(db).queryForMap(sql.capture(), any(Object[].class));

        assertTrue(sql.getValue().contains("status IN (?,?)"));
        assertTrue(sql.getValue().contains("categoria IN (?,?)"));
    }
}
