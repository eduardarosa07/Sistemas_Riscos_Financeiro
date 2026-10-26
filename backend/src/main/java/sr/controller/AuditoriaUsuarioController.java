package sr.controller;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import sr.model.AuditoriaUsuario;
import sr.service.AuditoriaUsuarioService;
@RestController @RequestMapping("/api/auditoria-usuarios") public class AuditoriaUsuarioController {
 private final AuditoriaUsuarioService svc;
 public AuditoriaUsuarioController(AuditoriaUsuarioService s){svc=s;}
 @GetMapping public List<AuditoriaUsuario> listar(){return svc.listar();}
}
