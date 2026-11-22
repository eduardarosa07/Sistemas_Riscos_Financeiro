package sr.controller;

import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Identifica o backend quando a porta da API e aberta diretamente.
 *
 * <p>A interface do SGR permanece no Angular. Este endpoint evita que a raiz
 * da porta 8080 seja confundida com uma falha da aplicacao.</p>
 */
@RestController
public class ApiInfoController {

    private final String frontendOrigin;

    public ApiInfoController(
            @Value("${app.frontend.origin:http://localhost:4200}")
            String frontendOrigin) {
        this.frontendOrigin = frontendOrigin;
    }

    @GetMapping("/")
    public Map<String, String> informacoes() {
        return Map.of(
            "aplicacao", "Sistema de Gerenciamento de Riscos",
            "servico", "Backend REST",
            "status", "online",
            "interface", frontendOrigin,
            "health", "/api/status"
        );
    }
}
