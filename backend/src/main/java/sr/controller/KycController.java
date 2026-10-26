package sr.controller;

import sr.model.Kyc;
import sr.service.KycService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/kycs")
public class KycController {

    private final KycService service;

    public KycController(KycService service) {
        this.service = service;
    }

    @GetMapping
    public List<Kyc> listarTodos() {
        return service.listarTodos();
    }

    @GetMapping("/{id}")
    public Kyc buscarPorId(@PathVariable String id) {
        return service.buscarPorId(id);
    }

    @PostMapping
    public ResponseEntity<Kyc> criar(@Valid @RequestBody Kyc objeto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.salvar(objeto));
    }

    @PutMapping("/{id}")
    public Kyc atualizar(@PathVariable String id, @Valid @RequestBody Kyc objeto) {
        return service.atualizar(id, objeto);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluir(@PathVariable String id) {
        service.excluir(id);
        return ResponseEntity.noContent().build();
    }
    @PostMapping("/{id}/decisao")
    @org.springframework.transaction.annotation.Transactional
    public Kyc decidir(@PathVariable String id, @RequestBody java.util.Map<String,String> body) {
        String decisao=body.get("decisao"), parecer=body.get("parecer");
        if (!java.util.List.of("ACEITO","NAO_ACEITO").contains(decisao == null ? "" : decisao)
                || parecer == null || parecer.isBlank() || parecer.length()>5000)
            throw new org.springframework.web.server.ResponseStatusException(HttpStatus.BAD_REQUEST,"Decisão ou parecer inválido");
        Kyc kyc=service.buscarPorId(id);
        if (java.util.List.of("ACEITO","NAO_ACEITO").contains(kyc.getStatusKyc()))
            throw new org.springframework.web.server.ResponseStatusException(HttpStatus.CONFLICT,"Análise já concluída");
        kyc.setStatusKyc(decisao);
        kyc.setMotivoDecisao(parecer.trim());
        return service.salvar(kyc);
    }
}
