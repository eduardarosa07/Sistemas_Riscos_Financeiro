package sr.config;

import sr.dto.LoginResponse;
import sr.model.Usuario;
import sr.repository.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AutenticacaoInterceptorTest {
 private final UsuarioRepository repo=mock(UsuarioRepository.class);
 private final AutenticacaoInterceptor guard=new AutenticacaoInterceptor(repo);
 private MockHttpServletRequest request(String perfil,String rota,String metodo){
  var req=new MockHttpServletRequest(metodo,rota);req.setRequestURI(rota);
  req.getSession().setAttribute("usuarioLogado",new LoginResponse(8L,"Usuário Teste","teste@exemplo.test",perfil));
  req.getSession().setAttribute("sessaoVersao",0L);
  var u=new Usuario();u.setId(8L);u.setNome("Usuário Teste");u.setEmail("teste@exemplo.test");u.setPerfil(perfil);u.setStatus("ATIVO");
  when(repo.findById(8L)).thenReturn(Optional.of(u));return req;
 }
 private int status(String perfil,String path,String method){var res=new MockHttpServletResponse();guard.preHandle(request(perfil,path,method),res,null);return res.getStatus();}
 @Test void rf05_usuarioNaoPodeConsultarClientes(){assertEquals(403,status("USUARIO","/api/clientes","GET"));}
 @Test void rf05_usuarioPodeConsultarIndicadores(){assertEquals(200,status("USUARIO","/api/indicadores/ocorrencias","GET"));}
 @Test void rf34_gestorNaoPodeAdministrarUsuarios(){assertEquals(403,status("GESTOR","/api/usuarios","GET"));}
 @Test void rf34_administradorPodeAdministrarUsuarios(){assertEquals(200,status("ADMINISTRADOR","/api/usuarios","GET"));}
 @Test void rf23_gestorNaoPodeTratarOcorrencia(){assertEquals(403,status("GESTOR","/api/tratativas/4/concluir","POST"));}
 @Test void rf23_analistaPodeTratarOcorrencia(){assertEquals(200,status("ANALISTA","/api/tratativas/4/concluir","POST"));}
 @Test void rf35_apenasAnalistaPodeDecidirKyc(){assertEquals(403,status("GESTOR","/api/kycs/KYC1/decisao","POST"));assertEquals(200,status("ANALISTA","/api/kycs/KYC1/decisao","POST"));}
 @Test void rf05_administradorNaoPodeConsultarOperacional(){assertEquals(403,status("ADMINISTRADOR","/api/clientes","GET"));}
}
