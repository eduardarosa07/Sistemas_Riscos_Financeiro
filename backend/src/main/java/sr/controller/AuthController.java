package sr.controller;

import java.util.Map;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import sr.config.ProtecaoCsrfInterceptor;
import sr.dto.AlterarMinhaSenhaRequest;
import sr.dto.LoginRequest;
import sr.dto.LoginResponse;
import sr.model.Usuario;
import sr.repository.UsuarioRepository;
import sr.service.ControleAutenticacao;
import sr.service.UsuarioService;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final ControleAutenticacao autenticacao;
    private final UsuarioRepository repository;
    private final UsuarioService usuarios;

    public AuthController(
            ControleAutenticacao autenticacao,
            UsuarioRepository repository,
            UsuarioService usuarios) {

        this.autenticacao = autenticacao;
        this.repository = repository;
        this.usuarios = usuarios;
    }

    @GetMapping("/csrf")
    public Map<String, String> csrf(HttpSession session) {
        return Map.of("token", ProtecaoCsrfInterceptor.token(session));
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest http) {

        Usuario usuario = autenticacao.autenticar(
            request.getEmail(),
            request.getSenha()
        );

        HttpSession sessaoAnterior = http.getSession(false);
        if (sessaoAnterior != null) {
            sessaoAnterior.invalidate();
        }

        HttpSession sessao = http.getSession(true);
        LoginResponse resposta = resposta(usuario);
        sessao.setAttribute("usuarioLogado", resposta);
        sessao.setAttribute("sessaoVersao", usuario.getSessaoVersao());

        return ResponseEntity.ok(resposta);
    }

    @GetMapping("/me")
    public ResponseEntity<LoginResponse> me(HttpServletRequest request) {
        HttpSession sessao = request.getSession(false);

        if (sessao == null
                || !(sessao.getAttribute("usuarioLogado")
                    instanceof LoginResponse login)) {
            return ResponseEntity.status(401).build();
        }

        Usuario usuario = repository.findById(login.getId()).orElse(null);
        boolean sessaoValida = usuario != null
            && "ATIVO".equals(usuario.getStatus())
            && usuario.getSessaoVersao().equals(
                sessao.getAttribute("sessaoVersao")
            );

        if (!sessaoValida) {
            sessao.invalidate();
            return ResponseEntity.status(401).build();
        }

        return ResponseEntity.ok(resposta(usuario));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest request) {
        HttpSession sessao = request.getSession(false);
        if (sessao != null) {
            sessao.invalidate();
        }
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/minha-senha")
    public ResponseEntity<Void> minhaSenha(
            @Valid @RequestBody AlterarMinhaSenhaRequest body,
            HttpSession sessao) {

        LoginResponse login =
            (LoginResponse) sessao.getAttribute("usuarioLogado");

        usuarios.alterarMinhaSenha(
            login.getId(),
            body.senhaAtual(),
            body.novaSenha()
        );
        sessao.invalidate();

        return ResponseEntity.noContent().build();
    }

    private LoginResponse resposta(Usuario usuario) {
        return new LoginResponse(
            usuario.getId(),
            usuario.getNome(),
            usuario.getEmail(),
            usuario.getPerfil()
        );
    }
}
