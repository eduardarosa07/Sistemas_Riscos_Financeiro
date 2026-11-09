
package sr.controller;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import sr.dto.LoginResponse;
import sr.domain.RegraTratativa;
import sr.service.SincronizacaoOcorrenciasService;

/**
 * Controlador de tratativas do Sistema de Riscos Financeiros.
 *
 * Responsabilidades:
 * - Consultar ocorrencias de KYC, PLD, Fraude e Chargeback.
 * - Aplicar filtros de pesquisa.
 * - Consultar o contexto do cliente.
 * - Permitir assumir ocorrencias.
 * - Registrar solicitacoes de parecer.
 * - Concluir ocorrencias.
 * - Preservar o historico auditavel.
 *
 * Historico:
 * Utiliza public.ocorrencia_historico, tabela existente
 * no PostgreSQL sistema_riscos.
 */
@RestController
@RequestMapping("/api/tratativas")
public class TratativaController {

    private static final Set<String> CATEGORIAS = Set.of(
        "KYC", "PLD", "FRAUDE", "CHARGEBACK"
    );

    private static final Set<String> ESTADOS = Set.of(
        "PENDENTE",
        "EM_ANALISE",
        "AGUARDANDO_PARECER",
        "CONCLUIDA"
    );

    /**
     * Campos utilizados pelas consultas de ocorrencias.
     *
     * Os aliases preservam o contrato utilizado
     * pelo frontend Angular.
     */
    private static final String COL =
        "t.id,t.categoria,t.origem_id AS \"origemId\",t.status,"
        + "t.responsavel_id AS \"responsavelId\","
        + "u.nome AS \"responsavelNome\","
        + "t.produto,t.cliente_id AS \"clienteId\","
        + "t.data_ocorrencia AS \"dataOcorrencia\","
        + "t.valor,t.risco,t.decisao,t.justificativa,"
        + "t.criado_em AS \"criadoEm\","
        + "t.atualizado_em AS \"atualizadoEm\","
        + "t.concluido_em AS \"concluidoEm\"";

    private static final String FROM =
        " FROM public.ocorrencia_tratativa t"
        + " LEFT JOIN public.usuario u"
        + " ON u.id=t.responsavel_id";

    private final JdbcTemplate db;

    private final SincronizacaoOcorrenciasService sincronizacao;

    public TratativaController(
            JdbcTemplate db,
            SincronizacaoOcorrenciasService sincronizacao) {

        this.db = db;
        this.sincronizacao = sincronizacao;
    }

    /**
     * Recupera o usuario autenticado na sessao.
     */
    private LoginResponse usuario(HttpServletRequest request) {

        var session = request.getSession(false);

        if (session == null
                || !(session.getAttribute("usuarioLogado")
                    instanceof LoginResponse login)) {

            throw new ResponseStatusException(
                HttpStatus.UNAUTHORIZED,
                "Sessão não encontrada"
            );
        }

        return login;
    }

    /**
     * Valida uma lista de valores recebida no filtro.
     */
    private static List<String> valores(
            String raw,
            Set<String> aceitos,
            String nome) {

        if (raw == null || raw.isBlank()) {
            return List.of();
        }

        List<String> lista = Arrays.stream(raw.split(",", -1))
            .map(String::trim)
            .filter(x -> !x.isBlank())
            .distinct()
            .toList();

        if (lista.isEmpty() || !aceitos.containsAll(lista)) {

            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Filtro inválido: " + nome
            );
        }

        return lista;
    }

    /**
     * Adiciona filtros parametrizados na consulta SQL.
     */
    private static void filtroLista(
            StringBuilder sql,
            List<Object> args,
            String campo,
            List<String> valores) {

        if (valores.isEmpty()) {
            return;
        }

        sql.append(" AND ")
           .append(campo)
           .append(" IN (")
           .append(String.join(
               ",",
               Collections.nCopies(valores.size(), "?")
           ))
           .append(")");

        args.addAll(valores);
    }

    /**
     * Lista ocorrencias conforme a visao solicitada.
     *
     * FILA: somente ocorrencias nao concluidas.
     * HISTORICO: somente ocorrencias concluidas.
     * TODAS: todas as ocorrencias.
     */
    @GetMapping
    public List<Map<String, Object>> listar(
            @RequestParam(defaultValue = "FILA") String visao,
            @RequestParam(required = false) String categoria,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String inicio,
            @RequestParam(required = false) String fim,
            @RequestParam(required = false) String produto,
            @RequestParam(required = false) String decisao,
            @RequestParam(required = false) String responsavel) {

        if (!Set.of("FILA", "HISTORICO", "TODAS")
                .contains(visao)) {

            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Visão inválida"
            );
        }

        StringBuilder sql = new StringBuilder(
            "SELECT " + COL + FROM + " WHERE 1=1"
        );

        List<Object> args = new ArrayList<>();

        if ("FILA".equals(visao)) {
            sql.append(" AND t.status<>'CONCLUIDA'");
        }

        if ("HISTORICO".equals(visao)) {
            sql.append(" AND t.status='CONCLUIDA'");
        }

        filtroLista(
            sql,
            args,
            "t.categoria",
            valores(categoria, CATEGORIAS, "categoria")
        );

        filtroLista(
            sql,
            args,
            "t.status",
            valores(status, ESTADOS, "status")
        );

        // Validacao do periodo.
        try {

            LocalDate dataInicial =
                inicio == null || inicio.isBlank()
                    ? null
                    : LocalDate.parse(inicio);

            LocalDate dataFinal =
                fim == null || fim.isBlank()
                    ? null
                    : LocalDate.parse(fim);

            if (dataInicial != null
                    && dataFinal != null
                    && dataInicial.isAfter(dataFinal)) {

                throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Período inválido"
                );
            }

            if (dataInicial != null) {

                sql.append(
                    " AND t.data_ocorrencia>=?"
                );

                args.add(
                    Timestamp.valueOf(
                        dataInicial.atStartOfDay()
                    )
                );
            }

            if (dataFinal != null) {

                sql.append(
                    " AND t.data_ocorrencia<?"
                );

                args.add(
                    Timestamp.valueOf(
                        dataFinal.plusDays(1).atStartOfDay()
                    )
                );
            }

        } catch (DateTimeParseException ex) {

            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Data inválida"
            );
        }

        // Filtro por produto.
        if (produto != null && !produto.isBlank()) {

            List<String> produtos = Arrays.stream(
                    produto.split(",")
                )
                .map(String::trim)
                .filter(x -> !x.isBlank())
                .distinct()
                .toList();

            if (produtos.size() > 30) {

                throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Excesso de produtos"
                );
            }

            filtroLista(sql, args, "t.produto", produtos);
        }

        // Filtro por decisao.
        if (decisao != null && !decisao.isBlank()) {

            List<String> aceitas = valores(
                decisao,
                Set.of(
                    "PROCEDENTE",
                    "IMPROCEDENTE",
                    "SEM_EVIDENCIAS_SUFICIENTES",
                    "ACEITO",
                    "NAO_ACEITO"
                ),
                "decisao"
            );

            filtroLista(
                sql,
                args,
                "t.decisao",
                aceitas
            );
        }

        // Filtro por responsavel.
        if (responsavel != null && !responsavel.isBlank()) {

            String busca = responsavel.trim();

            sql.append(
                " AND (LOWER(u.nome) LIKE LOWER(?)"
                + " OR CAST(t.responsavel_id AS TEXT)=?)"
            );

            args.add(
                "%" + busca
                    .replace("%", "\\%")
                    .replace("_", "\\_") + "%"
            );

            args.add(busca);
        }

        sql.append(
            " ORDER BY t.data_ocorrencia DESC,"
            + "t.id DESC LIMIT 5000"
        );

        return db.queryForList(
            sql.toString(),
            args.toArray()
        );
    }

    /**
     * Consulta uma ocorrencia pelo ID.
     */
    @GetMapping("/{id}")
    public Map<String, Object> detalhe(
            @PathVariable long id) {

        return obter(id);
    }

    /**
     * Consulta uma ocorrencia pelo identificador
     * original da categoria.
     */
    @GetMapping("/origem")
    public Map<String, Object> porOrigem(
            @RequestParam String categoria,
            @RequestParam String origemId) {

        if (!CATEGORIAS.contains(categoria)
                || origemId == null
                || origemId.isBlank()
                || origemId.length() > 120) {

            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Origem inválida"
            );
        }

        String consulta =
            "SELECT " + COL + FROM
            + " WHERE t.categoria=? AND t.origem_id=?";

        List<Map<String, Object>> rows =
            db.queryForList(
                consulta,
                categoria,
                origemId
            );

        if (rows.isEmpty()) {

            sincronizacao.atualizar();

            rows = db.queryForList(
                consulta,
                categoria,
                origemId
            );
        }

        if (rows.isEmpty()) {

            throw new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Ocorrência de origem não encontrada"
            );
        }

        return rows.get(0);
    }

    /**
     * Sincroniza novos registros das quatro categorias.
     */
    @PostMapping("/sincronizar")
    public Map<String, Integer> sincronizar() {

        return sincronizacao.atualizar();
    }

    /**
     * Consulta o contexto do cliente vinculado
     * a ocorrencia.
     */
    @GetMapping("/{id}/contexto")
    public Map<String, Object> contexto(
            @PathVariable long id) {

        List<Map<String, Object>> rows = db.queryForList("""
            SELECT
                t.id,
                t.origem_id AS "origemId",
                t.categoria,
                c.id_cliente AS "clienteId",
                c.nome_razao_social AS "clienteNome",
                c.status_cliente AS "clienteStatus",
                r.nivel_risco AS "riscoGeral",
                r.score_risco_geral AS "scoreRiscoGeral"
            FROM public.ocorrencia_tratativa t
            LEFT JOIN public.dim_cliente c
                ON c.id_cliente=t.cliente_id
            LEFT JOIN public.fato_risco_cliente r
                ON r.id_cliente=t.cliente_id
            WHERE t.id=?
            """, id);

        if (rows.isEmpty()) {

            throw new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Ocorrência não encontrada"
            );
        }

        return rows.get(0);
    }

    /**
     * Consulta o historico auditavel.
     *
     * ALTERACAO:
     * Utiliza public.ocorrencia_historico.
     *
     * O nome do executor e recuperado da
     * tabela usuario, sem duplicar informacoes.
     */
    @GetMapping("/{id}/historico")
    public List<Map<String, Object>> historico(
            @PathVariable long id) {

        obter(id);

        return db.queryForList("""
            SELECT
                h.*,
                u.nome AS executor_nome
            FROM public.ocorrencia_historico h
            LEFT JOIN public.usuario u
                ON u.id=h.executor_id
            WHERE h.ocorrencia_id=?
            ORDER BY h.registrado_em,h.id
            """, id);
    }

    /**
     * Permite que o analista assuma uma ocorrencia.
     *
     * Registra a transicao no historico.
     */
    @PostMapping("/{id}/assumir")
    @Transactional
    public Map<String, Object> assumir(
            @PathVariable long id,
            HttpServletRequest req) {

        LoginResponse u = usuario(req);

        Map<String, Object> before = lock(id);

        String atual = (String) before.get("status");

        if (!"PENDENTE".equals(atual)
                && !"AGUARDANDO_PARECER".equals(atual)) {

            throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                "Ocorrência não disponível"
            );
        }

        Long responsavelAnterior =
            idResponsavel(before);

        if (!RegraTratativa.podeAssumir(
                atual,
                responsavelAnterior,
                u.getId())) {

            throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                "Parecer reservado ao analista responsável"
            );
        }

        db.update("""
            UPDATE public.ocorrencia_tratativa
            SET status='EM_ANALISE',
                responsavel_id=?,
                atualizado_em=CURRENT_TIMESTAMP
            WHERE id=?
            """,
            u.getId(),
            id
        );

        evento(
            id,
            u,
            responsavelAnterior,
            u.getId(),
            atual,
            "EM_ANALISE",
            "ASSUMIR",
            null,
            null
        );

        return obter(id);
    }

    /**
     * Solicita parecer para uma ocorrencia
     * assumida pelo analista responsavel.
     */
    @PostMapping("/{id}/parecer")
    @Transactional
    public Map<String, Object> parecer(
            @PathVariable long id,
            @RequestBody Map<String, String> body,
            HttpServletRequest req) {

        LoginResponse u = usuario(req);

        Map<String, Object> before = lock(id);

        validarResponsavel(before, u);

        String nota = texto(body.get("justificativa"));

        String anterior = (String) before.get("status");

        Long responsavel = idResponsavel(before);

        db.update("""
            UPDATE public.ocorrencia_tratativa
            SET status='AGUARDANDO_PARECER',
                justificativa=?,
                atualizado_em=CURRENT_TIMESTAMP
            WHERE id=?
            """,
            nota,
            id
        );

        evento(
            id,
            u,
            responsavel,
            responsavel,
            anterior,
            "AGUARDANDO_PARECER",
            "SOLICITAR_PARECER",
            null,
            nota
        );

        return obter(id);
    }

    /**
     * Conclui uma ocorrencia.
     *
     * A decisao e justificativa sao obrigatorias.
     *
     * KYC deve ser concluido pelo endpoint
     * especifico de decisao cadastral.
     */
    @PostMapping("/{id}/concluir")
    @Transactional
    public Map<String, Object> concluir(
            @PathVariable long id,
            @RequestBody Map<String, String> body,
            HttpServletRequest req) {

        LoginResponse u = usuario(req);

        Map<String, Object> before = lock(id);

        validarResponsavel(before, u);

        if ("KYC".equals(before.get("categoria"))) {

            throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                "Conclua KYC pela decisão cadastral"
            );
        }

        String nota = texto(body.get("justificativa"));
        String decisao = body.get("decisao");

        if (!RegraTratativa.decisaoValida(decisao)) {

            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Decisão inválida"
            );
        }

        String anterior = (String) before.get("status");

        Long responsavel = idResponsavel(before);

        db.update("""
            UPDATE public.ocorrencia_tratativa
            SET status='CONCLUIDA',
                decisao=?,
                justificativa=?,
                atualizado_em=CURRENT_TIMESTAMP,
                concluido_em=CURRENT_TIMESTAMP
            WHERE id=?
            """,
            decisao,
            nota,
            id
        );

        evento(
            id,
            u,
            responsavel,
            responsavel,
            anterior,
            "CONCLUIDA",
            "CONCLUIR",
            decisao,
            nota
        );

        return obter(id);
    }

    /**
     * Valida a justificativa obrigatoria.
     */
    private static String texto(String valor) {

        if (!RegraTratativa.justificativaValida(valor)) {

            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Justificativa obrigatória (até 5000 caracteres)"
            );
        }

        return valor.trim();
    }

    /**
     * Bloqueia uma ocorrencia durante operacoes
     * de alteracao, evitando concorrencia indevida.
     */
    private Map<String, Object> lock(long id) {

        List<Map<String, Object>> rows = db.queryForList("""
            SELECT
                id,
                status,
                categoria,
                responsavel_id
            FROM public.ocorrencia_tratativa
            WHERE id=?
            FOR UPDATE
            """, id);

        if (rows.isEmpty()) {

            throw new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Ocorrência não encontrada"
            );
        }

        return rows.get(0);
    }

    /**
     * Recupera o identificador do responsavel.
     *
     * Trata valores INTEGER ou BIGINT retornados
     * pelo driver JDBC.
     */
    private static Long idResponsavel(
            Map<String, Object> row) {

        Object valor = row.get("responsavel_id");

        if (valor == null) {
            return null;
        }

        return ((Number) valor).longValue();
    }

    /**
     * Impede que outro analista modifique
     * uma ocorrencia assumida.
     */
    private void validarResponsavel(
            Map<String, Object> row,
            LoginResponse u) {

        Long responsavel = idResponsavel(row);

        if (!RegraTratativa.podeTratar(
                (String) row.get("status"),
                responsavel,
                u.getId())) {

            throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                "Somente o analista responsável pode continuar a tratativa"
            );
        }
    }

    /**
     * Recupera a ocorrencia atualizada.
     */
    private Map<String, Object> obter(long id) {

        List<Map<String, Object>> rows =
            db.queryForList(
                "SELECT " + COL + FROM + " WHERE t.id=?",
                id
            );

        if (rows.isEmpty()) {

            throw new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Ocorrência não encontrada"
            );
        }

        return rows.get(0);
    }

    /**
     * REGISTRO DE HISTORICO
     *
     * Registra quem realizou a operacao,
     * responsavel anterior e novo,
     * status anterior e novo,
     * decisao e justificativa.
     *
     * A tabela possui trigger que impede
     * UPDATE e DELETE em registros historicos.
     */
    private void evento(
            long id,
            LoginResponse u,
            Long responsavelAnterior,
            Long responsavelNovo,
            String statusAnterior,
            String statusNovo,
            String acao,
            String decisao,
            String justificativa) {

        db.update("""
            INSERT INTO public.ocorrencia_historico
                (
                    ocorrencia_id,
                    executor_id,
                    responsavel_anterior_id,
                    responsavel_novo_id,
                    status_anterior,
                    status_novo,
                    acao,
                    decisao,
                    justificativa
                )
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
            """,
            id,
            u.getId(),
            responsavelAnterior,
            responsavelNovo,
            statusAnterior,
            statusNovo,
            acao,
            decisao,
            justificativa
        );
    }
}
