package sr.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Mantém URLs amigáveis (/home, /clientes etc.) e entrega a SPA estática.
 * As chamadas /api permanecem exclusivamente REST.
 */
@Controller
public class PaginaController {
    @GetMapping({"/", "/login", "/home", "/clientes", "/ocorrencias", "/pld", "/chargeback", "/kyc", "/fraude", "/relatorios", "/usuarios"})
    public String pagina() {
        return "forward:/index.html";
    }
}
