
package sr.controller;

import java.util.List;
import java.util.Map;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import sr.dto.LoginResponse;
import sr.domain.RegraKyc;
import sr.model.Kyc;
import sr.service.KycService;

@RestController
@RequestMapping("/api/kycs")
public class KycController {

    private final KycService service;
    private final JdbcTemplate jdbc;

    public KycController(KycService service, JdbcTemplate jdbc) {
        this.service = service;
        this.jdbc = jdbc;
    }

    @GetMapping
    public List<Kyc> listarTodos() {
        return service.listarTodos();
    }

    @GetMapping("/fila")
    public List<Kyc> fila(
            @RequestParam(defaultValue = "PENDENTES") String visao) {

        return switch (visao) {
            case "PENDENTES" -> service.listarPendentes();
            case "HISTORICO" -> service.listarConcluidos();
            default -> throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST, "Visão KYC inválida");
        };
    }

    @GetMapping("/{id}")
    public Kyc buscarPorId(@PathVariable String id) {
        return service.buscarPorId(id);
    }

    /**
     * Consulta o histórico KYC na tabela já existente
     * no PostgreSQL: ocorrencia_historico.
     *
     * O nome do executor é obtido por JOIN com usuario.
     */
    @GetMapping("/{id}/historico")
    public List<Map<String, Object>> historico(
            @PathVariable String id) {

        service.buscarPorId(id);

        return jdbc.queryForList("""
            SELECT
                h.*,
                u.nome AS executor_nome
            FROM public.ocorrencia_historico h
            JOIN public.ocorrencia_tratativa t
                ON t.id = h.ocorrencia_id
            LEFT JOIN public.usuario u
                ON u.id = h.executor_id
            WHERE t.categoria = 'KYC'
              AND t.origem_id = ?
            ORDER BY h.registrado_em, h.id
            """, id);
    }

    @PostMapping
    public ResponseEntity<Kyc> criar(
            @Valid @RequestBody Kyc objeto) {

        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(service.salvar(objeto));
    }

    @PutMapping("/{id}")
    public Kyc atualizar(
            @PathVariable String id,
            @Valid @RequestBody Kyc objeto) {

        return service.atualizar(id, objeto);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluir(
            @PathVariable String id) {

        service.excluir(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * RF35 - Decisão KYC.
     *
     * Executa na mesma transação:
     * 1. Validação da análise e da sessão.
     * 2. Controle de concorrência.
     * 3. Registro da decisão e parecer.
     * 4. Inativação do cliente, quando aplicável.
     * 5. Conclusão da ocorrência.
     * 6. Registro do histórico auditável.
     */
    @PostMapping("/{id}/decisao")
    @Transactional
    public Kyc decidir(
            @PathVariable String id,
            @RequestBody Map<String, String> body,
            HttpServletRequest request) {

        String decisao = body.get("decisao");
        String parecer = body.get("parecer");

        if (!RegraKyc.decisaoValida(decisao)
                || !RegraKyc.parecerValido(parecer)) {

            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Decisão ou parecer inválido");
        }

        var session = request.getSession(false);

        if (session == null
                || !(session.getAttribute("usuarioLogado")
                    instanceof LoginResponse usuario)) {

            throw new ResponseStatusException(
                HttpStatus.UNAUTHORIZED,
                "Sessão necessária");
        }

        // Bloqueia o KYC durante a decisão.
        List<Map<String, Object>> registros = jdbc.queryForList("""
            SELECT
                id_kyc,
                id_cliente,
                status_kyc,
                data_analise,
                nivel_risco_kyc
            FROM public.fato_kyc
            WHERE id_kyc = ?
            FOR UPDATE
            """, id);

        if (registros.isEmpty()) {
            throw new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Análise KYC não encontrada");
        }

        Map<String, Object> k = registros.get(0);

        if (RegraKyc.encerrada(
                String.valueOf(k.get("status_kyc")))) {

            throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                "Decisão KYC já concluída");
        }

        // Garante a existência da ocorrência sem duplicar.
        jdbc.update("""
            INSERT INTO public.ocorrencia_tratativa
                (categoria, origem_id, status,
                 cliente_id, data_ocorrencia, risco)
            VALUES
                ('KYC', ?, 'PENDENTE', ?,
                 CAST(? AS TIMESTAMP), ?)
            ON CONFLICT (categoria, origem_id) DO NOTHING
            """,
            id,
            k.get("id_cliente"),
            k.get("data_analise"),
            k.get("nivel_risco_kyc")
        );

        // Bloqueia a ocorrência para evitar decisões concorrentes.
        Map<String, Object> o = jdbc.queryForMap("""
            SELECT id, status, responsavel_id, categoria
            FROM public.ocorrencia_tratativa
            WHERE categoria = 'KYC'
              AND origem_id = ?
            FOR UPDATE
            """, id);

        if ("CONCLUIDA".equals(o.get("status"))) {
            throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                "A ocorrência KYC está encerrada");
        }

        Number assigned = (Number) o.get("responsavel_id");

        Long responsavelAnterior =
            assigned == null ? null : assigned.longValue();

        if (!RegraKyc.podeDecidir(
                (String) k.get("status_kyc"),
                responsavelAnterior,
                usuario.getId())) {

            throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                "Análise atribuída a outro analista");
        }

        String statusAnterior = (String) o.get("status");
        String justificativa = parecer.trim();

        // Grava a decisão na análise KYC.
        jdbc.update("""
            UPDATE public.fato_kyc
            SET status_kyc = ?,
                motivo_decisao = ?
            WHERE id_kyc = ?
            """,
            decisao,
            justificativa,
            id
        );

        // Inativa o cliente quando a decisão exigir.
        if (RegraKyc.deveInativar(decisao)) {

            int atualizados = jdbc.update("""
                UPDATE public.dim_cliente
                SET status_cliente = 'INATIVO'
                WHERE id_cliente = ?
                """, k.get("id_cliente"));

            if (atualizados != 1) {
                throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Cliente vinculado não encontrado");
            }
        }

        // Conclui a ocorrência e atribui o analista.
        jdbc.update("""
            UPDATE public.ocorrencia_tratativa
            SET status = 'CONCLUIDA',
                responsavel_id = ?,
                decisao = ?,
                justificativa = ?,
                concluido_em = CURRENT_TIMESTAMP,
                atualizado_em = CURRENT_TIMESTAMP
            WHERE id = ?
            """,
            usuario.getId(),
            decisao,
            justificativa,
            o.get("id")
        );

        /*
         * HISTÓRICO
         *
         * Utiliza a tabela existente ocorrencia_historico.
         * O registro é protegido pelo trigger de imutabilidade.
         */
        jdbc.update("""
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
            VALUES (
                ?, ?, ?, ?, ?,
                'CONCLUIDA',
                'DECISAO_KYC',
                ?, ?
            )
            """,
            o.get("id"),
            usuario.getId(),
            responsavelAnterior,
            usuario.getId(),
            statusAnterior,
            decisao,
            justificativa
        );

        return service.buscarPorId(id);
    }
}
