package sr.controller;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

/** Agregações calculadas no PostgreSQL e filtros compartilhados em todos os indicadores. */
@RestController
@RequestMapping("/api/indicadores")
public class IndicadoresController {
    private static final Set<String> STATUS = Set.of("PENDENTE", "EM_ANALISE", "AGUARDANDO_PARECER", "CONCLUIDA");
    private static final Set<String> CATEGORIAS = Set.of("KYC", "PLD", "FRAUDE", "CHARGEBACK");
    private final JdbcTemplate db;
    public IndicadoresController(JdbcTemplate db) { this.db = db; }

    @GetMapping("/resumo")
    public Map<String,Long> resumo() {
        return Map.of("clientes",contar("dim_cliente"),"transacoes",contar("fato_transacao"),
            "fraude",contar("fato_alerta_fraude"),"pld",contar("fato_alerta_pld"),
            "kyc",contar("fato_kyc"),"chargeback",contar("fato_chargeback"));
    }
    private Long contar(String tabela) {
        return db.queryForObject("SELECT COUNT(*) FROM public." + tabela,Long.class);
    }
    private static List<String> lista(String raw, Set<String> permitidos, String campo) {
        if(raw == null || raw.isBlank()) return List.of();
        List<String> itens=Arrays.stream(raw.split(",",-1)).map(String::trim).toList();
        if(itens.isEmpty() || itens.size()>30 || itens.stream().anyMatch(x->x.isEmpty() || !permitidos.contains(x)))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Filtro inválido: " + campo);
        return itens.stream().distinct().toList();
    }
    private static void incluir(StringBuilder sql, List<Object> args,String campo,List<String> itens){
        if(!itens.isEmpty()) {
            sql.append(" AND ").append(campo).append(" IN (")
                .append(String.join(",",Collections.nCopies(itens.size(),"?"))).append(")");
            args.addAll(itens);
        }
    }
    private static LocalDate data(String raw) {
        if(raw==null || raw.isBlank()) return null;
        try {return LocalDate.parse(raw);}catch(DateTimeParseException ex){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Informe a data no formato AAAA-MM-DD");
        }
    }
    @GetMapping("/ocorrencias")
    public Map<String,Object> ocorrencias(@RequestParam(required=false) String inicio,
        @RequestParam(required=false) String fim, @RequestParam(required=false) String status,
        @RequestParam(required=false) String categoria, @RequestParam(required=false) String produto){
        LocalDate a=data(inicio),b=data(fim);
        if(a!=null && b!=null && a.isAfter(b))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"A data inicial não pode ser posterior à final");
        StringBuilder filtro=new StringBuilder(" WHERE 1=1");
        List<Object> args=new ArrayList<>();
        if(a!=null){filtro.append(" AND data_ocorrencia >= ?");args.add(Timestamp.valueOf(a.atStartOfDay()));}
        if(b!=null){filtro.append(" AND data_ocorrencia < ?");args.add(Timestamp.valueOf(b.plusDays(1).atStartOfDay()));}
        incluir(filtro,args,"status",lista(status,STATUS,"status"));
        incluir(filtro,args,"categoria",lista(categoria,CATEGORIAS,"categoria"));
        if(produto!=null&&!produto.isBlank()){
            List<String> produtos=Arrays.stream(produto.split(",",-1)).map(String::trim).distinct().toList();
            if(produtos.size()>30 || produtos.stream().anyMatch(x->x.isEmpty()||x.length()>160))
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Produto inválido");
            incluir(filtro,args,"produto",produtos);
        }
        String base=" FROM public.ocorrencia_tratativa"+filtro;
        Object[] params=args.toArray();
        Map<String,Object> contagens=db.queryForMap("SELECT COUNT(*) AS total,"
            +" COUNT(*) FILTER (WHERE status='CONCLUIDA') AS concluidas,"
            +" COUNT(*) FILTER (WHERE data_ocorrencia::date=CURRENT_DATE) AS dia,"
            +" COUNT(*) FILTER (WHERE date_trunc('month',data_ocorrencia)=date_trunc('month',CURRENT_TIMESTAMP)) AS mes"+base,params);
        long total=((Number)contagens.get("total")).longValue();
        long concluidas=((Number)contagens.get("concluidas")).longValue();
        Map<String,Object> resultado=new LinkedHashMap<>();
        resultado.put("total",total);resultado.put("concluidas",concluidas);
        resultado.put("abertas",total-concluidas);
        resultado.put("diaAtual",contagens.get("dia"));resultado.put("mesAtual",contagens.get("mes"));
        resultado.put("porCategoria",db.queryForList("SELECT categoria,COUNT(*) AS total"+base+" GROUP BY categoria ORDER BY categoria",params));
        resultado.put("porStatus",db.queryForList("SELECT status,COUNT(*) AS total"+base+" GROUP BY status ORDER BY status",params));
        resultado.put("porCategoriaStatus",db.queryForList("SELECT categoria,status,COUNT(*) AS total"+base+" GROUP BY categoria,status ORDER BY categoria,status",params));
        resultado.put("porProduto",db.queryForList("SELECT produto,COUNT(*) AS total"+base+" AND produto IS NOT NULL GROUP BY produto ORDER BY produto",params));
        resultado.put("evolucao",db.queryForList("SELECT TO_CHAR(date_trunc('month',data_ocorrencia),'YYYY-MM') AS periodo,COUNT(*) AS total"+base+" GROUP BY date_trunc('month',data_ocorrencia) ORDER BY date_trunc('month',data_ocorrencia)",params));
        return resultado;
    }
    @GetMapping("/produtos")
    public List<String> produtos(){
        return db.queryForList("SELECT DISTINCT produto FROM public.ocorrencia_tratativa WHERE produto IS NOT NULL AND TRIM(produto)<>'' ORDER BY produto",String.class);
    }
}
