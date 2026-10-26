package sr.controller;

import sr.model.RegraRisco;
import sr.service.RegraRiscoService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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

    @PostMapping
    public ResponseEntity<RegraRisco> criar(@Valid @RequestBody RegraRisco objeto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.salvar(objeto));
    }

    @PutMapping("/{id}")
    public RegraRisco atualizar(@PathVariable String id, @Valid @RequestBody RegraRisco objeto) {
        return service.atualizar(id, objeto);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluir(@PathVariable String id) {
        service.excluir(id);
        return ResponseEntity.noContent().build();
    }
}
