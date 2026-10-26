package sr.service;
import org.springframework.stereotype.Service;
import sr.model.AuditoriaUsuario;
import sr.repository.AuditoriaUsuarioRepository;
import java.util.List;
@Service public class AuditoriaUsuarioService {
 private final AuditoriaUsuarioRepository repo;
 public AuditoriaUsuarioService(AuditoriaUsuarioRepository r){repo=r;}
 public void registrar(Long executor,Long alvo,String acao,String detalhes){
  AuditoriaUsuario a=new AuditoriaUsuario();a.setExecutorId(executor);a.setAfetadoId(alvo);
  a.setAcao(acao);a.setDetalhes(detalhes);repo.save(a);
 }
 public List<AuditoriaUsuario> listar(){return repo.findAll();}
}
