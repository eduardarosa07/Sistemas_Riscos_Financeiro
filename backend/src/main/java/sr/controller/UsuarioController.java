
package sr.controller;

import java.util.List;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import sr.dto.AlterarStatusUsuarioRequest;
import sr.dto.AtualizarUsuarioRequest;
import sr.dto.CadastroUsuarioRequest;
import sr.dto.LoginResponse;
import sr.dto.RedefinirSenhaRequest;
import sr.dto.UsuarioResponse;
import sr.service.UsuarioService;

@RestController
@RequestMapping("/api/usuarios")
public class UsuarioController {

    private final UsuarioService usuarioService;

    public UsuarioController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    // Identifica o usuário autenticado pela sessão HTTP.
    // O ID do executor nunca deve ser recebido do frontend.

    private Long identificarExecutor(HttpServletRequest request) {

        HttpSession sessao = request.getSession(false);

        if (sessao == null) {
            throw new org.springframework.web.server
                .ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Sessão não encontrada."
                );
        }

        Object autenticado = sessao.getAttribute("usuarioLogado");

        if (!(autenticado instanceof LoginResponse login)) {
            throw new org.springframework.web.server
                .ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Usuário não autenticado."
                );
        }

        if (!"ADMINISTRADOR".equals(login.getPerfil())) {
            throw new org.springframework.web.server
                .ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Acesso permitido somente ao Administrador."
                );
        }

        return login.getId();
    }

    // LISTAR USUÁRIOS

    @GetMapping
    public List<UsuarioResponse> listar(
            HttpServletRequest request) {

        identificarExecutor(request);

        return usuarioService.listar();
    }

    // CADASTRAR NOVO USUÁRIO

    @PostMapping
    public ResponseEntity<UsuarioResponse> cadastrar(
            @Valid @RequestBody CadastroUsuarioRequest request,
            HttpServletRequest httpRequest) {

        Long executor = identificarExecutor(httpRequest);

        UsuarioResponse usuario =
                usuarioService.cadastrar(request, executor);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(usuario);
    }

    // ATUALIZAR DADOS DO USUÁRIO

    @PutMapping("/{id}")
    public UsuarioResponse atualizar(
            @PathVariable Long id,
            @Valid @RequestBody AtualizarUsuarioRequest request,
            HttpServletRequest httpRequest) {

        Long executor = identificarExecutor(httpRequest);

        // O UsuarioService verifica se o administrador
        // está tentando alterar o próprio perfil.

        return usuarioService.atualizar(
                id,
                request,
                executor
        );
    }

    // ATIVAR OU INATIVAR USUÁRIO

    @PatchMapping("/{id}/status")
    public UsuarioResponse alterarStatus(
            @PathVariable Long id,
            @Valid @RequestBody AlterarStatusUsuarioRequest request,
            HttpServletRequest httpRequest) {

        Long executor = identificarExecutor(httpRequest);

        // O UsuarioService impede a auto-inativação
        // e protege o último administrador ativo.

        return usuarioService.alterarStatus(
                id,
                request.getStatus(),
                executor
        );
    }

    // REDEFINIR SENHA DE OUTRO USUÁRIO

    @PostMapping("/{id}/redefinir-senha")
    public ResponseEntity<Void> redefinirSenha(
            @PathVariable Long id,
            @Valid @RequestBody RedefinirSenhaRequest request,
            HttpServletRequest httpRequest) {

        Long executor = identificarExecutor(httpRequest);

        // A própria senha deve ser alterada pelo
        // endpoint específico de autenticação.

        if (id.equals(executor)) {
            throw new org.springframework.web.server
                .ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Utilize a opção Alterar minha senha."
                );
        }

        usuarioService.redefinirSenha(
                id,
                request.novaSenha(),
                executor
        );

        return ResponseEntity.noContent().build();
    }
}
