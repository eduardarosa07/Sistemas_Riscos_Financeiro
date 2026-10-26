package sr.controller;
import jakarta.servlet.http.*;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import sr.dto.*;
import sr.model.Usuario;
import sr.repository.UsuarioRepository;
import sr.service.*;
import sr.config.ProtecaoCsrfInterceptor;
import java.util.Map;
@RestController @RequestMapping("/api/auth") public class AuthController {
 private final ControleAutenticacao autenticacao;
 private final UsuarioRepository repo;
 private final UsuarioService usuarios;
 public AuthController(ControleAutenticacao a,UsuarioRepository r,UsuarioService u){autenticacao=a;repo=r;usuarios=u;}
 @GetMapping("/csrf") public Map<String,String> csrf(HttpSession session){return Map.of("token",ProtecaoCsrfInterceptor.token(session));}
 @PostMapping("/login") public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request,HttpServletRequest http){
  Usuario usuario=autenticacao.autenticar(request.getEmail(),request.getSenha());
  HttpSession antiga=http.getSession(false);if(antiga!=null)antiga.invalidate();
  HttpSession sessao=http.getSession(true);
  LoginResponse resposta=resposta(usuario);sessao.setAttribute("usuarioLogado",resposta);
  sessao.setAttribute("sessaoVersao",usuario.getSessaoVersao());
  return ResponseEntity.ok(resposta);
 }
 @GetMapping("/me") public ResponseEntity<LoginResponse> me(HttpServletRequest req){
  HttpSession s=req.getSession(false);if(s==null || !(s.getAttribute("usuarioLogado") instanceof LoginResponse login))return ResponseEntity.status(401).build();
  Usuario u=repo.findById(login.getId()).orElse(null);
  if(u==null||!"ATIVO".equals(u.getStatus())||!u.getSessaoVersao().equals(s.getAttribute("sessaoVersao"))){s.invalidate();return ResponseEntity.status(401).build();}
  return ResponseEntity.ok(resposta(u));
 }
 @PostMapping("/logout") public ResponseEntity<Void> logout(HttpServletRequest req){HttpSession s=req.getSession(false);if(s!=null)s.invalidate();return ResponseEntity.noContent().build();}
 @PostMapping("/minha-senha") public ResponseEntity<Void> minhaSenha(@Valid @RequestBody AlterarMinhaSenhaRequest body,HttpSession sessao){
  LoginResponse login=(LoginResponse)sessao.getAttribute("usuarioLogado");
  usuarios.alterarMinhaSenha(login.getId(),body.senhaAtual(),body.novaSenha());sessao.invalidate();return ResponseEntity.noContent().build();
 }
 private LoginResponse resposta(Usuario u){return new LoginResponse(u.getId(),u.getNome(),u.getEmail(),u.getPerfil());}
}
