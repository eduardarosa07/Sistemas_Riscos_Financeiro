package sr.controller;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.server.ResponseStatusException;
import org.mockito.ArgumentCaptor;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;

class IndicadoresControllerTest {
 private final JdbcTemplate db=mock(JdbcTemplate.class);
 private final IndicadoresController controller=new IndicadoresController(db);
 @Test void rf29_deveRecusarDatasInvertidas(){var erro=assertThrows(ResponseStatusException.class,()->controller.ocorrencias("2026-12-05","2026-12-01",null,null,null));assertEquals(HttpStatus.BAD_REQUEST,erro.getStatusCode());verifyNoInteractions(db);}
 @Test void rf29_deveRecusarFormatoDataInvalido(){assertThrows(ResponseStatusException.class,()->controller.ocorrencias("01/11/2026",null,null,null,null));verifyNoInteractions(db);}
 @Test void rf30_deveRecusarStatusDesconhecido(){assertThrows(ResponseStatusException.class,()->controller.ocorrencias(null,null,"INVÁLIDO",null,null));verifyNoInteractions(db);}
 @Test void rf31_deveRecusarCategoriaDesconhecida(){assertThrows(ResponseStatusException.class,()->controller.ocorrencias(null,null,null,"OUTRA",null));verifyNoInteractions(db);}
 @Test void rf32_deveRecusarProdutoVazioNaLista(){assertThrows(ResponseStatusException.class,()->controller.ocorrencias(null,null,null,null,"PIX,,TED"));verifyNoInteractions(db);}
 @Test void rf33_deveAplicarStatusECategoriaCombinados(){
  when(db.queryForMap(anyString(),any(Object[].class))).thenReturn(Map.of("total",2L,"concluidas",1L,"dia",1L,"mes",2L));
  controller.ocorrencias(null,null,"PENDENTE,CONCLUIDA","PLD,FRAUDE",null);
  var sql=ArgumentCaptor.forClass(String.class);verify(db).queryForMap(sql.capture(),any(Object[].class));
  assertTrue(sql.getValue().contains("status IN (?,?)"));assertTrue(sql.getValue().contains("categoria IN (?,?)"));
 }
}
