package sr.service;

import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import sr.exception.AutenticacaoException;
import sr.model.Usuario;
import sr.repository.UsuarioRepository;

@Service
public class ControleAutenticacao {

    private static final int LIMITE_TENTATIVAS = 5;
    private static final long DURACAO_BLOQUEIO_MS = 120_000;

    private final UsuarioRepository repository;
    private final PasswordEncoder encoder;
    private final ConcurrentHashMap<String, Tentativas> tentativas =
        new ConcurrentHashMap<>();

    private static final class Tentativas {
        private int quantidade;
        private long bloqueadoAte;
    }

    public ControleAutenticacao(
            UsuarioRepository repository,
            PasswordEncoder encoder) {
        this.repository = repository;
        this.encoder = encoder;
    }

    public Usuario autenticar(String email, String senha) {
        String emailNormalizado = email == null
            ? ""
            : email.trim().toLowerCase(Locale.ROOT);

        Tentativas estado = tentativas.computeIfAbsent(
            emailNormalizado,
            chave -> new Tentativas()
        );

        synchronized (estado) {
            if (System.currentTimeMillis() < estado.bloqueadoAte) {
                throw new AutenticacaoException(
                    "Muitas tentativas. Tente novamente em alguns minutos"
                );
            }
        }

        Usuario usuario = repository.findByEmail(emailNormalizado).orElse(null);
        boolean credenciaisValidas = usuario != null
            && "ATIVO".equals(usuario.getStatus())
            && encoder.matches(senha, usuario.getSenhaHash());

        if (!credenciaisValidas) {
            registrarFalha(estado);
            throw new AutenticacaoException("Credenciais inválidas");
        }

        tentativas.remove(emailNormalizado);
        return usuario;
    }

    private void registrarFalha(Tentativas estado) {
        synchronized (estado) {
            estado.quantidade++;
            if (estado.quantidade >= LIMITE_TENTATIVAS) {
                estado.quantidade = 0;
                estado.bloqueadoAte =
                    System.currentTimeMillis() + DURACAO_BLOQUEIO_MS;
            }
        }
    }
}
