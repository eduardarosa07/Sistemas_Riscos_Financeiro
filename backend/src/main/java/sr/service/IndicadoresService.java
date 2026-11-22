package sr.service;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

/**
 * Valida os filtros gerenciais e executa as agregações no PostgreSQL.
 *
 * Nenhuma classificação de risco é criada aqui: as consultas apenas contam e
 * agrupam ocorrências já existentes.
 */
@Service
public class IndicadoresService {

    private static final Set<String> STATUS = Set.of(
        "PENDENTE",
        "EM_ANALISE",
        "AGUARDANDO_PARECER",
        "CONCLUIDA"
    );

    private static final Set<String> CATEGORIAS = Set.of(
        "KYC",
        "PLD",
        "FRAUDE",
        "CHARGEBACK"
    );

    private final JdbcTemplate db;

    public IndicadoresService(JdbcTemplate db) {
        this.db = db;
    }

    public Map<String, Long> resumo() {
        return Map.of(
            "clientes", contar("dim_cliente"),
            "transacoes", contar("fato_transacao"),
            "fraude", contar("fato_alerta_fraude"),
            "pld", contar("fato_alerta_pld"),
            "kyc", contar("fato_kyc"),
            "chargeback", contar("fato_chargeback")
        );
    }

    public Map<String, Object> ocorrencias(
            String inicio,
            String fim,
            String status,
            String categoria,
            String produto) {

        LocalDate dataInicial = data(inicio);
        LocalDate dataFinal = data(fim);

        if (dataInicial != null
                && dataFinal != null
                && dataInicial.isAfter(dataFinal)) {

            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "A data inicial não pode ser posterior à final"
            );
        }

        StringBuilder filtro = new StringBuilder(" WHERE 1=1");
        List<Object> argumentos = new ArrayList<>();

        if (dataInicial != null) {
            filtro.append(" AND data_ocorrencia >= ?");
            argumentos.add(Timestamp.valueOf(dataInicial.atStartOfDay()));
        }

        if (dataFinal != null) {
            filtro.append(" AND data_ocorrencia < ?");
            argumentos.add(
                Timestamp.valueOf(dataFinal.plusDays(1).atStartOfDay())
            );
        }

        incluir(filtro, argumentos, "status", lista(status, STATUS, "status"));
        incluir(
            filtro,
            argumentos,
            "categoria",
            lista(categoria, CATEGORIAS, "categoria")
        );
        incluirProdutos(filtro, argumentos, produto);

        String base = " FROM public.ocorrencia_tratativa" + filtro;
        Object[] parametros = argumentos.toArray();

        Map<String, Object> contagens = db.queryForMap(
            "SELECT COUNT(*) AS total,"
                + " COUNT(*) FILTER (WHERE status='CONCLUIDA') AS concluidas,"
                + " COUNT(*) FILTER (WHERE data_ocorrencia::date=CURRENT_DATE) AS dia,"
                + " COUNT(*) FILTER (WHERE date_trunc('month',data_ocorrencia)="
                + "date_trunc('month',CURRENT_TIMESTAMP)) AS mes"
                + base,
            parametros
        );

        long total = numero(contagens, "total");
        long concluidas = numero(contagens, "concluidas");

        Map<String, Object> resultado = new LinkedHashMap<>();
        resultado.put("total", total);
        resultado.put("concluidas", concluidas);
        resultado.put("abertas", total - concluidas);
        resultado.put("diaAtual", contagens.get("dia"));
        resultado.put("mesAtual", contagens.get("mes"));

        resultado.put(
            "porCategoria",
            db.queryForList(
                "SELECT categoria,COUNT(*) AS total"
                    + base
                    + " GROUP BY categoria ORDER BY categoria",
                parametros
            )
        );
        resultado.put(
            "porStatus",
            db.queryForList(
                "SELECT status,COUNT(*) AS total"
                    + base
                    + " GROUP BY status ORDER BY status",
                parametros
            )
        );
        resultado.put(
            "porCategoriaStatus",
            db.queryForList(
                "SELECT categoria,status,COUNT(*) AS total"
                    + base
                    + " GROUP BY categoria,status ORDER BY categoria,status",
                parametros
            )
        );
        resultado.put(
            "porProduto",
            db.queryForList(
                "SELECT produto,COUNT(*) AS total"
                    + base
                    + " AND produto IS NOT NULL"
                    + " GROUP BY produto ORDER BY produto",
                parametros
            )
        );
        resultado.put(
            "evolucao",
            db.queryForList(
                "SELECT TO_CHAR(date_trunc('month',data_ocorrencia),'YYYY-MM')"
                    + " AS periodo,COUNT(*) AS total"
                    + base
                    + " GROUP BY date_trunc('month',data_ocorrencia)"
                    + " ORDER BY date_trunc('month',data_ocorrencia)",
                parametros
            )
        );

        return resultado;
    }

    public List<String> produtos() {
        return db.queryForList(
            "SELECT DISTINCT produto"
                + " FROM public.ocorrencia_tratativa"
                + " WHERE produto IS NOT NULL"
                + " AND TRIM(produto)<>''"
                + " ORDER BY produto",
            String.class
        );
    }

    private Long contar(String tabela) {
        return db.queryForObject(
            "SELECT COUNT(*) FROM public." + tabela,
            Long.class
        );
    }

    private static List<String> lista(
            String valor,
            Set<String> permitidos,
            String campo) {

        if (valor == null || valor.isBlank()) {
            return List.of();
        }

        List<String> itens = Arrays.stream(valor.split(",", -1))
            .map(String::trim)
            .toList();

        boolean invalida = itens.isEmpty()
            || itens.size() > 30
            || itens.stream().anyMatch(
                item -> item.isEmpty() || !permitidos.contains(item)
            );

        if (invalida) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Filtro inválido: " + campo
            );
        }

        return itens.stream().distinct().toList();
    }

    private static void incluir(
            StringBuilder sql,
            List<Object> argumentos,
            String campo,
            List<String> itens) {

        if (itens.isEmpty()) {
            return;
        }

        sql.append(" AND ")
            .append(campo)
            .append(" IN (")
            .append(String.join(",", Collections.nCopies(itens.size(), "?")))
            .append(")");

        argumentos.addAll(itens);
    }

    private static void incluirProdutos(
            StringBuilder filtro,
            List<Object> argumentos,
            String produto) {

        if (produto == null || produto.isBlank()) {
            return;
        }

        List<String> produtos = Arrays.stream(produto.split(",", -1))
            .map(String::trim)
            .distinct()
            .toList();

        boolean invalida = produtos.size() > 30
            || produtos.stream().anyMatch(
                item -> item.isEmpty() || item.length() > 160
            );

        if (invalida) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Produto inválido"
            );
        }

        incluir(filtro, argumentos, "produto", produtos);
    }

    private static LocalDate data(String valor) {
        if (valor == null || valor.isBlank()) {
            return null;
        }

        try {
            return LocalDate.parse(valor);
        } catch (DateTimeParseException exception) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Informe a data no formato AAAA-MM-DD"
            );
        }
    }

    private static long numero(Map<String, Object> valores, String chave) {
        return ((Number) valores.get(chave)).longValue();
    }
}
