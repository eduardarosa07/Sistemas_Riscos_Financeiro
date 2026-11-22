package sr.controller;

import sr.model.AlertaPld;
import sr.service.AlertaPldService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/alertas-pld")
public class AlertaPldController {

    private final AlertaPldService service;

    public AlertaPldController(AlertaPldService service) {
        this.service = service;
    }

    @GetMapping
    public List<AlertaPld> listarTodos() {
        return service.listarTodos();
    }

    @GetMapping("/{id}")
    public AlertaPld buscarPorId(@PathVariable String id) {
        return service.buscarPorId(id);
    }

}
