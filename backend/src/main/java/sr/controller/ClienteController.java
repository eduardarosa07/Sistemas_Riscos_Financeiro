package sr.controller;

import sr.dto.ClienteHistoricoDTO;
import sr.model.Cliente;
import sr.service.ClienteService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/clientes")
public class ClienteController {

    private final ClienteService service;

    public ClienteController(ClienteService service) {
        this.service = service;
    }

    /** Consulta o risco e os contadores recebidos da fonte externa. */
    @GetMapping("/visao")
    public List<Map<String,Object>> listarComRisco() {
        return service.listarComRisco();
    }

    @GetMapping
    public List<Cliente> listarTodos() {
        return service.listarTodos();
    }

    @GetMapping("/{id}")
    public Cliente buscarPorId(@PathVariable String id) {
        return service.buscarPorId(id);
    }

    @GetMapping("/{id}/historico")
    public ClienteHistoricoDTO buscarHistorico(@PathVariable String id) {
        return service.buscarHistorico(id);
    }
}
