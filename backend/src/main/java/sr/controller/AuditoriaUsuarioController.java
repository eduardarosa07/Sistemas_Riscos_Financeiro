package sr.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import sr.model.AuditoriaUsuario;
import sr.service.AuditoriaUsuarioService;

@RestController
@RequestMapping("/api/auditoria-usuarios")
public class AuditoriaUsuarioController {

    private final AuditoriaUsuarioService service;

    public AuditoriaUsuarioController(AuditoriaUsuarioService service) {
        this.service = service;
    }

    @GetMapping
    public List<AuditoriaUsuario> listar() {
        return service.listar();
    }
}
