package sr.controller;

import java.util.*;
import java.time.LocalDate;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import jakarta.servlet.http.HttpServletRequest;
import sr.dto.LoginResponse;

@RestController
@RequestMapping("/api/tratativas")
public class TratativaController {
 private final JdbcTemplate db;
 public TratativaController(JdbcTemplate db){this.db=db;}
 private LoginResponse usuario(HttpServletRequest req){Object u=req.getSession(false).getAttribute("usuarioLogado");if(!(u instanceof LoginResponse l))throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);return l;}
 private static final String COL="id,categoria,origem_id AS \"origemId\",status,responsavel_id AS \"responsavelId\",(SELECT nome FROM public.usuario WHERE id=t.responsavel_id) AS \"responsavelNome\",produto,cliente_id AS \"clienteId\",data_ocorrencia AS \"dataOcorrencia\",valor,risco,decisao,justificativa,criado_em AS \"criadoEm\",atualizado_em AS \"atualizadoEm\",concluido_em AS \"concluidoEm\"";
 @GetMapping public List<Map<String,Object>> listar(@RequestParam(required=false) String categoria,@RequestParam(required=false) String status,@RequestParam(required=false) String inicio,@RequestParam(required=false) String fim,@RequestParam(required=false) String produto){
  StringBuilder sql=new StringBuilder("SELECT "+COL+" FROM public.ocorrencia_tratativa t WHERE 1=1");List<Object> args=new ArrayList<>();
  if(categoria!=null&&!categoria.isBlank()){sql.append(" AND categoria=?");args.add(categoria);}
  if(status!=null&&!status.isBlank()){sql.append(" AND status=?");args.add(status);}
  try{
   if(inicio!=null&&!inicio.isBlank()){sql.append(" AND data_ocorrencia >= ?");args.add(java.sql.Timestamp.valueOf(LocalDate.parse(inicio).atStartOfDay()));}
   if(fim!=null&&!fim.isBlank()){sql.append(" AND data_ocorrencia < ?");args.add(java.sql.Timestamp.valueOf(LocalDate.parse(fim).plusDays(1).atStartOfDay()));}
   if(inicio!=null&&!inicio.isBlank()&&fim!=null&&!fim.isBlank()&&LocalDate.parse(inicio).isAfter(LocalDate.parse(fim)))throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Período inválido");
  }catch(java.time.format.DateTimeParseException ex){throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Data inválida");}
  if(produto!=null&&!produto.isBlank()){sql.append(" AND produto=?");args.add(produto);}
  sql.append(" ORDER BY data_ocorrencia DESC,id DESC LIMIT 5000");return db.queryForList(sql.toString(),args.toArray());
 }
 @GetMapping("/{id}/historico") public List<Map<String,Object>> historico(@PathVariable long id){return db.queryForList("SELECT * FROM public.ocorrencia_tratativa_evento WHERE ocorrencia_id=? ORDER BY registrado_em,id",id);}
 @PostMapping("/{id}/assumir") @Transactional public Map<String,Object> assumir(@PathVariable long id,HttpServletRequest req){
  LoginResponse u=usuario(req);Map<String,Object> before=lock(id);String old=(String)before.get("status");
  if(!List.of("PENDENTE","AGUARDANDO_PARECER").contains(old))throw new ResponseStatusException(HttpStatus.CONFLICT,"Ocorrência não disponível para assumir");
  db.update("UPDATE public.ocorrencia_tratativa SET status='EM_ANALISE',responsavel_id=?,atualizado_em=CURRENT_TIMESTAMP WHERE id=?",u.getId(),id);
  event(id,u,old,"EM_ANALISE","ASSUMIR",null,null);return obter(id);
 }
 @PostMapping("/{id}/parecer") @Transactional public Map<String,Object> parecer(@PathVariable long id,@RequestBody Map<String,String> body,HttpServletRequest req){
  LoginResponse u=usuario(req);Map<String,Object> before=lock(id);validarResponsavel(before,u);String nota=texto(body.get("justificativa"));
  db.update("UPDATE public.ocorrencia_tratativa SET status='AGUARDANDO_PARECER',justificativa=?,atualizado_em=CURRENT_TIMESTAMP WHERE id=?",nota,id);
  event(id,u,"EM_ANALISE","AGUARDANDO_PARECER","SOLICITAR_PARECER",null,nota);return obter(id);
 }
 @PostMapping("/{id}/concluir") @Transactional public Map<String,Object> concluir(@PathVariable long id,@RequestBody Map<String,String> body,HttpServletRequest req){
  LoginResponse u=usuario(req);Map<String,Object> before=lock(id);validarResponsavel(before,u);String nota=texto(body.get("justificativa"));String decisao=body.get("decisao");
  if(!List.of("PROCEDENTE","IMPROCEDENTE","SEM_EVIDENCIAS_SUFICIENTES").contains(decisao))throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Decisão inválida");
  db.update("UPDATE public.ocorrencia_tratativa SET status='CONCLUIDA',decisao=?,justificativa=?,atualizado_em=CURRENT_TIMESTAMP,concluido_em=CURRENT_TIMESTAMP WHERE id=?",decisao,nota,id);
  event(id,u,"EM_ANALISE","CONCLUIDA","CONCLUIR",decisao,nota);return obter(id);
 }
 private String texto(String valor){if(valor==null||valor.isBlank()||valor.length()>5000)throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Justificativa obrigatória (até 5000 caracteres)");return valor.trim();}
 private Map<String,Object> lock(long id){List<Map<String,Object>> r=db.queryForList("SELECT id,status,responsavel_id FROM public.ocorrencia_tratativa WHERE id=? FOR UPDATE",id);if(r.isEmpty())throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Ocorrência não encontrada");return r.get(0);}
 private void validarResponsavel(Map<String,Object> row,LoginResponse u){if(!"EM_ANALISE".equals(row.get("status"))||!Objects.equals(((Number)row.get("responsavel_id")).longValue(),u.getId()))throw new ResponseStatusException(HttpStatus.CONFLICT,"Ocorrência não atribuída ao analista ou não está em análise");}
 private Map<String,Object> obter(long id){return db.queryForMap("SELECT "+COL+" FROM public.ocorrencia_tratativa t WHERE id=?",id);}
 private void event(long id,LoginResponse u,String old,String next,String acao,String decisao,String nota){db.update("INSERT INTO public.ocorrencia_tratativa_evento(ocorrencia_id,executor_id,executor_nome,status_anterior,status_novo,acao,decisao,justificativa) VALUES(?,?,?,?,?,?,?,?)",id,u.getId(),u.getNome(),old,next,acao,decisao,nota);}
}
