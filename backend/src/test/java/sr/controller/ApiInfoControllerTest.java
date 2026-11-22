package sr.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Map;

import org.junit.jupiter.api.Test;

class ApiInfoControllerTest {

    @Test
    void raizIdentificaApiEApontaParaAngular() {
        ApiInfoController controller =
            new ApiInfoController("http://localhost:4200");

        Map<String, String> resposta = controller.informacoes();

        assertEquals("online", resposta.get("status"));
        assertEquals("Backend REST", resposta.get("servico"));
        assertEquals("http://localhost:4200", resposta.get("interface"));
        assertEquals("/api/status", resposta.get("health"));
    }
}
