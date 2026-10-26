
package sr.service;

import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import sr.dto.*;
import sr.exception.*;
import sr.model.Usuario;
import sr.repository.UsuarioRepository;

@Service
public class UsuarioService {

    private static final Set<String> PERFIS = Set.of(
        "USUARIO",
        "ANALISTA",
        "GESTOR",
        "ADMINISTRADOR"
    );

    private final UsuarioRepository repo;
    private final PasswordEncoder encoder;
    private final AuditoriaUsuarioService audit;

    public UsuarioService(
            UsuarioRepository repo,
            PasswordEncoder encoder,
            AuditoriaUsuarioService audit) {

        this.repo = repo;
        this.encoder = encoder;
        this.audit = audit;
    }

    // LISTAR USUÁRIOS

    public List<UsuarioResponse> listar() {
        return repo.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    // CADASTRAR USUÁRIO

    @Transactional
    public UsuarioResponse cadastrar(
            CadastroUsuarioRequest request,
            Long executor) {

        validarEmail(request.getEmail(), null);

        Usuario usuario = new Usuario();

        usuario.setNome(request.getNome().trim());
        usuario.setEmail(normalizar(request.getEmail()));
        usuario.setSenhaHash(
                encoder.encode(request.getSenha())
        );
        usuario.setPerfil(perfil(request.getPerfil()));
        usuario.setStatus("ATIVO");

        repo.saveAndFlush(usuario);

        audit.registrar(
                executor,
                usuario.getId(),
                "CADASTRO",
                "Perfil: " + usuario.getPerfil()
        );

        return toResponse(usuario);
    }

    // ATUALIZAR USUÁRIO

    @Transactional
    public UsuarioResponse atualizar(
            Long id,
            AtualizarUsuarioRequest request,
            Long executor) {

        Usuario usuario = buscar(id);

        String novoPerfil = perfil(request.getPerfil());
        String perfilAnterior = usuario.getPerfil();

        // SEGURANÇA:
        // O administrador não pode alterar o próprio perfil.
        // A validação ocorre no backend, independentemente
        // do bloqueio visual implementado no Angular.

        if (id.equals(executor)
                && "ADMINISTRADOR".equals(perfilAnterior)
                && !perfilAnterior.equals(novoPerfil)) {

            throw new IllegalArgumentException(
                "O administrador não pode alterar o próprio perfil."
            );
        }

        // Impede que o último administrador ativo
        // perca o perfil ADMINISTRADOR.

        if ("ADMINISTRADOR".equals(perfilAnterior)
                && !"ADMINISTRADOR".equals(novoPerfil)) {

            protegerUltimo(usuario);
        }

        validarEmail(request.getEmail(), id);

        usuario.setNome(request.getNome().trim());
        usuario.setEmail(normalizar(request.getEmail()));
        usuario.setPerfil(novoPerfil);

        // Revoga sessões anteriores quando o perfil mudar.

        if (!perfilAnterior.equals(novoPerfil)) {
            usuario.revogarSessoes();
        }

        // A senha não é alterada neste método.
        // Edições cadastrais preservam o hash existente.

        repo.saveAndFlush(usuario);

        audit.registrar(
                executor,
                id,
                "EDICAO",
                "Dados cadastrais e perfil: " + novoPerfil
        );

        return toResponse(usuario);
    }

    // ATIVAR OU INATIVAR USUÁRIO

    @Transactional
    public UsuarioResponse alterarStatus(
            Long id,
            String status,
            Long executor) {

        Usuario usuario = buscar(id);

        String novoStatus = status == null
                ? ""
                : status.trim().toUpperCase(Locale.ROOT);

        if (!Set.of("ATIVO", "INATIVO").contains(novoStatus)) {
            throw new IllegalArgumentException(
                "Status deve ser ATIVO ou INATIVO."
            );
        }

        // SEGURANÇA:
        // Nenhum administrador pode inativar a própria conta.

        if ("INATIVO".equals(novoStatus)
                && id.equals(executor)) {

            throw new IllegalArgumentException(
                "Não é permitido inativar a própria conta."
            );
        }

        // Impede inativar o último administrador ativo.

        if ("INATIVO".equals(novoStatus)
                && "ATIVO".equals(usuario.getStatus())) {

            protegerUltimo(usuario);
        }

        if (!usuario.getStatus().equals(novoStatus)) {

            usuario.setStatus(novoStatus);
            usuario.revogarSessoes();

            repo.saveAndFlush(usuario);

            audit.registrar(
                    executor,
                    id,
                    "ALTERACAO_STATUS",
                    "Status: " + novoStatus
            );
        }

        return toResponse(usuario);
    }

    // REDEFINIR SENHA PELO ADMINISTRADOR

    @Transactional
    public void redefinirSenha(
            Long id,
            String novaSenha,
            Long executor) {

        Usuario usuario = buscar(id);

        usuario.setSenhaHash(
                encoder.encode(novaSenha)
        );

        usuario.revogarSessoes();

        repo.saveAndFlush(usuario);

        audit.registrar(
                executor,
                id,
                "REDEFINICAO_SENHA",
                "Senha redefinida pelo administrador"
        );
    }

    // ALTERAR A PRÓPRIA SENHA

    @Transactional
    public void alterarMinhaSenha(
            Long id,
            String senhaAtual,
            String novaSenha) {

        Usuario usuario = buscar(id);

        if (!encoder.matches(
                senhaAtual,
                usuario.getSenhaHash())) {

            throw new IllegalArgumentException(
                "Senha atual incorreta."
            );
        }

        usuario.setSenhaHash(
                encoder.encode(novaSenha)
        );

        usuario.revogarSessoes();

        repo.saveAndFlush(usuario);

        audit.registrar(
                id,
                id,
                "ALTERACAO_PROPRIA_SENHA",
                "Usuário alterou sua senha"
        );
    }

    // BUSCAR USUÁRIO

    private Usuario buscar(Long id) {

        return repo.findById(id)
                .orElseThrow(
                    () -> new RecursoNaoEncontradoException(
                        "Usuário não encontrado"
                    )
                );
    }

    // PROTEGER O ÚLTIMO ADMINISTRADOR ATIVO

    private void protegerUltimo(Usuario usuario) {

        if ("ADMINISTRADOR".equals(usuario.getPerfil())
                && "ATIVO".equals(usuario.getStatus())
                && repo.countByPerfilAndStatus(
                        "ADMINISTRADOR",
                        "ATIVO"
                   ) <= 1) {

            throw new IllegalArgumentException(
                "É necessário manter um administrador ativo."
            );
        }
    }

    // VALIDAR PERFIL

    private String perfil(String valor) {

        String normalizado = valor == null
                ? ""
                : valor.trim().toUpperCase(Locale.ROOT);

        if (!PERFIS.contains(normalizado)) {
            throw new IllegalArgumentException(
                "Perfil não permitido."
            );
        }

        return normalizado;
    }

    // NORMALIZAR E-MAIL

    private String normalizar(String email) {

        return email == null
                ? null
                : email.trim().toLowerCase(Locale.ROOT);
    }

    // VALIDAR UNICIDADE DO E-MAIL

    private void validarEmail(String email, Long id) {

        repo.findByEmail(normalizar(email))
                .ifPresent(usuario -> {

                    if (id == null
                            || !id.equals(usuario.getId())) {

                        throw new EmailDuplicadoException(
                            "E-mail já cadastrado"
                        );
                    }
                });
    }

    // CONVERTER ENTIDADE PARA DTO

    public UsuarioResponse toResponse(Usuario usuario) {

        return new UsuarioResponse(
                usuario.getId(),
                usuario.getNome(),
                usuario.getEmail(),
                usuario.getPerfil(),
                usuario.getStatus(),
                usuario.getCreatedAt(),
                usuario.getUpdatedAt()
        );
    }
}
