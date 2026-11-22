package sr.controller;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import sr.service.ClienteService;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ClienteControllerTest {

    @Test
    void visaoConsolidadaDelegaConsultaAoService() {
        ClienteService service = mock(ClienteService.class);
        ClienteController controller = new ClienteController(service);
        List<Map<String, Object>> resposta = List.of(
            Map.of("idCliente", "CLI001", "riscoGeral", "ALTO")
        );
        when(service.listarComRisco()).thenReturn(resposta);

        assertSame(resposta, controller.listarComRisco());
        verify(service).listarComRisco();
    }
}
