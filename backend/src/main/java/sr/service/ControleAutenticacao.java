package sr.service;
import java.util.concurrent.*;
import java.util.Locale;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import sr.exception.AutenticacaoException;
import sr.model.Usuario;
import sr.repository.UsuarioRepository;
@Service public class ControleAutenticacao {
 private final UsuarioRepository repo;private final PasswordEncoder encoder;
 private final ConcurrentHashMap<String,Attempts> attempts=new ConcurrentHashMap<>();
 private static final class Attempts {int count;long until;}
 public ControleAutenticacao(UsuarioRepository r,PasswordEncoder e){repo=r;encoder=e;}
 public Usuario autenticar(String email,String senha){
  String normalized=email==null?"":email.trim().toLowerCase(Locale.ROOT);
  Attempts state=attempts.computeIfAbsent(normalized,k->new Attempts());
  synchronized(state){if(System.currentTimeMillis()<state.until)throw new AutenticacaoException("Muitas tentativas. Tente novamente em alguns minutos");}
  Usuario u=repo.findByEmail(normalized).orElse(null);
  if(u==null || !"ATIVO".equals(u.getStatus()) || !encoder.matches(senha,u.getSenhaHash())){
   synchronized(state){state.count++;if(state.count>=5){state.count=0;state.until=System.currentTimeMillis()+120_000;}}
   throw new AutenticacaoException("Credenciais inválidas");
  }
  attempts.remove(normalized);return u;
 }
}
