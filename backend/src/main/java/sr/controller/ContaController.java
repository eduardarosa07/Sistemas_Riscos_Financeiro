package sr.controller;

import sr.model.Conta;
import sr.service.ContaService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/contas")
public class ContaController {

    private final ContaService service;

    public ContaController(ContaService service) {
        this.service = service;
    }

    @GetMapping
    public List<Conta> listarTodos() {
        return service.listarTodos();
    }

    @GetMapping("/{id}")
    public Conta buscarPorId(@PathVariable String id) {
        return service.buscarPorId(id);
    }

}
