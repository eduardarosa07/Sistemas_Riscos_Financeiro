package sr.controller;

import sr.model.Chargeback;
import sr.service.ChargebackService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/chargebacks")
public class ChargebackController {

    private final ChargebackService service;

    public ChargebackController(ChargebackService service) {
        this.service = service;
    }

    @GetMapping
    public List<Chargeback> listarTodos() {
        return service.listarTodos();
    }

    @GetMapping("/{id}")
    public Chargeback buscarPorId(@PathVariable String id) {
        return service.buscarPorId(id);
    }

}
