package sr.config;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import static org.junit.jupiter.api.Assertions.*;

class ProtecaoCsrfInterceptorTest {
 private final ProtecaoCsrfInterceptor guard=new ProtecaoCsrfInterceptor();
 @Test void be12_postSemTokenDeveSerBloqueado(){var req=new MockHttpServletRequest("POST","/api/tratativas");var res=new MockHttpServletResponse();assertFalse(guard.preHandle(req,res,null));assertEquals(403,res.getStatus());}
 @Test void be12_postComTokenValidoDevePassar(){var req=new MockHttpServletRequest("POST","/api/tratativas");String token=ProtecaoCsrfInterceptor.token(req.getSession());req.addHeader("X-CSRF-Token",token);assertTrue(guard.preHandle(req,new MockHttpServletResponse(),null));}
 @Test void be12_tokenErradoDeveSerBloqueado(){var req=new MockHttpServletRequest("POST","/api/tratativas");ProtecaoCsrfInterceptor.token(req.getSession());req.addHeader("X-CSRF-Token","invalido");var res=new MockHttpServletResponse();assertFalse(guard.preHandle(req,res,null));assertEquals(403,res.getStatus());}
 @Test void be12_getNaoExigeCsrf(){assertTrue(guard.preHandle(new MockHttpServletRequest("GET","/api/indicadores/ocorrencias"),new MockHttpServletResponse(),null));}
}
