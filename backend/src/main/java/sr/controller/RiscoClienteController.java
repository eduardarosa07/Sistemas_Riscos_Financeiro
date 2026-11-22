package sr.controller;

import sr.model.RiscoCliente;
import sr.service.RiscoClienteService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/riscos-clientes")
public class RiscoClienteController {

    private final RiscoClienteService service;

    public RiscoClienteController(RiscoClienteService service) {
        this.service = service;
    }

    @GetMapping
    public List<RiscoCliente> listarTodos() {
        return service.listarTodos();
    }

    @GetMapping("/{id}")
    public RiscoCliente buscarPorId(@PathVariable String id) {
        return service.buscarPorId(id);
    }

}
