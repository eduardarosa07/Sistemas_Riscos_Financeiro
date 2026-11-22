package sr.controller;

import sr.model.Transacao;
import sr.service.TransacaoService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/transacoes")
public class TransacaoController {

    private final TransacaoService service;

    public TransacaoController(TransacaoService service) {
        this.service = service;
    }

    @GetMapping
    public List<Transacao> listarTodos() {
        return service.listarTodos();
    }

    @GetMapping("/{id}")
    public Transacao buscarPorId(@PathVariable String id) {
        return service.buscarPorId(id);
    }

}
