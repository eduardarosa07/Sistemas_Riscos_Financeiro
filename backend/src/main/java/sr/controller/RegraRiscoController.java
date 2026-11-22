package sr.controller;

import sr.model.RegraRisco;
import sr.service.RegraRiscoService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/regras-risco")
public class RegraRiscoController {

    private final RegraRiscoService service;

    public RegraRiscoController(RegraRiscoService service) {
        this.service = service;
    }

    @GetMapping
    public List<RegraRisco> listarTodos() {
        return service.listarTodos();
    }

    @GetMapping("/{id}")
    public RegraRisco buscarPorId(@PathVariable String id) {
        return service.buscarPorId(id);
    }

}
