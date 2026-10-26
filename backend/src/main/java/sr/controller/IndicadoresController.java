package sr.controller;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
@RestController @RequestMapping("/api/indicadores")
public class IndicadoresController {
 private final JdbcTemplate db;
 public IndicadoresController(JdbcTemplate db){this.db=db;}
 @GetMapping("/resumo") public Map<String,Long> resumo(){
  return Map.of("clientes",contar("dim_cliente"),"transacoes",contar("fato_transacao"),"fraude",contar("fato_alerta_fraude"),"pld",contar("fato_alerta_pld"),"kyc",contar("fato_kyc"),"chargeback",contar("fato_chargeback"));
 }
 private Long contar(String tabela){return db.queryForObject("SELECT COUNT(*) FROM public."+tabela,Long.class);}
 @GetMapping("/ocorrencias") public Map<String,Object> indicadores(
  @RequestParam(required=false) String inicio,@RequestParam(required=false) String fim,
  @RequestParam(required=false) String status,@RequestParam(required=false) String categoria,
  @RequestParam(required=false) String produto){
  if(inicio!=null&&fim!=null&&inicio.compareTo(fim)>0)throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Data inicial após data final.");
  String filtro=" WHERE (CAST(? AS DATE) IS NULL OR data_ocorrencia::date >= CAST(? AS DATE)) AND (CAST(? AS DATE) IS NULL OR data_ocorrencia::date <= CAST(? AS DATE)) AND (CAST(? AS TEXT) IS NULL OR status=?) AND (CAST(? AS TEXT) IS NULL OR categoria=?) AND (CAST(? AS TEXT) IS NULL OR produto=?)";
  Object[] args={inicio,inicio,fim,fim,status,status,categoria,categoria,produto,produto};
  Long total=db.queryForObject("SELECT COUNT(*) FROM public.ocorrencia_tratativa"+filtro,Long.class,args);
  Long concluidas=db.queryForObject("SELECT COUNT(*) FROM public.ocorrencia_tratativa"+filtro+" AND status='CONCLUIDA'",Long.class,args);
  List<Map<String,Object>> categorias=db.queryForList("SELECT categoria,COUNT(*) AS total FROM public.ocorrencia_tratativa"+filtro+" GROUP BY categoria ORDER BY categoria",args);
  List<Map<String,Object>> estados=db.queryForList("SELECT status,COUNT(*) AS total FROM public.ocorrencia_tratativa"+filtro+" GROUP BY status ORDER BY status",args);
  Long mes=db.queryForObject("SELECT COUNT(*) FROM public.ocorrencia_tratativa"+filtro+" AND date_trunc('month',data_ocorrencia)=date_trunc('month',CURRENT_TIMESTAMP)",Long.class,args);
  List<Map<String,Object>> cruzamento=db.queryForList("SELECT categoria,status,COUNT(*) AS total FROM public.ocorrencia_tratativa"+filtro+" GROUP BY categoria,status ORDER BY categoria,status",args);
  List<Map<String,Object>> evolucao=db.queryForList("SELECT TO_CHAR(date_trunc('month',data_ocorrencia),'YYYY-MM') AS periodo,COUNT(*) AS total FROM public.ocorrencia_tratativa"+filtro+" GROUP BY date_trunc('month',data_ocorrencia) ORDER BY date_trunc('month',data_ocorrencia)",args);
  return Map.of("total",total==null?0:total,"concluidas",concluidas==null?0:concluidas,"abertas",(total==null?0:total)-(concluidas==null?0:concluidas),"mesAtual",mes==null?0:mes,"porCategoria",categorias,"porStatus",estados,"porCategoriaStatus",cruzamento,"evolucao",evolucao);
 }
 @GetMapping("/produtos") public List<String> produtos(){return db.queryForList("SELECT DISTINCT produto FROM public.ocorrencia_tratativa WHERE produto IS NOT NULL ORDER BY produto",String.class);}
}
