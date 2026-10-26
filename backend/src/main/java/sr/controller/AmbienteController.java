package sr.controller;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoint simples para identificar o ambiente de persistencia da API.
 * Nao expoe usuario, senha ou outros segredos de conexao.
 */
@RestController
@RequestMapping("/api/ambiente")
public class AmbienteController {

    @Value("${spring.datasource.url}")
    private String datasourceUrl;

    @GetMapping
    public Map<String, Object> ambiente() {
        Map<String, Object> resposta = new LinkedHashMap<>();
        resposta.put("banco", "PostgreSQL");
        resposta.put("conectadoVia", "Spring Data JPA");
        resposta.put("urlConfigurada", ocultarDetalhes(datasourceUrl));
        return resposta;
    }

    private String ocultarDetalhes(String url) {
        if (url == null || url.isBlank()) {
            return "nao configurada";
        }
        return url.startsWith("jdbc:postgresql:") ? "jdbc:postgresql://***" : "configurada";
    }
}
