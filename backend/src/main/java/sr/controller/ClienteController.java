package sr.controller;

import sr.dto.ClienteHistoricoDTO;
import sr.model.Cliente;
import sr.service.ClienteService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;

@RestController
@RequestMapping("/api/clientes")
public class ClienteController {

    private final ClienteService service;
    private final JdbcTemplate jdbc;

    public ClienteController(ClienteService service, JdbcTemplate jdbc) {
        this.service = service;
        this.jdbc = jdbc;
    }

    /** Risco consolidado e contadores calculados/persistidos no backend. */
    @GetMapping("/visao")
    public List<Map<String,Object>> listarComRisco() {
        return jdbc.queryForList("""
            SELECT c.id_cliente AS "idCliente",c.tipo_pessoa AS "tipoPessoa",
             c.nome_razao_social AS "nomeRazaoSocial",c.documento_ficticio AS "documentoFicticio",
             c.status_cliente AS "statusCliente",c.cidade,c.uf,
             COALESCE(r.nivel_risco,'NÃO CLASSIFICADO') AS "riscoGeral",
             COALESCE(r.score_risco_geral,0) AS "scoreRiscoGeral",
             COALESCE(r.qtd_alertas_fraude,0) AS "qtdAlertasFraude",
             COALESCE(r.qtd_alertas_pld,0) AS "qtdAlertasPld",
             COALESCE(r.qtd_chargebacks,0) AS "qtdChargebacks"
            FROM public.dim_cliente c
            LEFT JOIN public.fato_risco_cliente r ON r.id_cliente=c.id_cliente
            ORDER BY c.nome_razao_social,c.id_cliente
            """);
    }

    @GetMapping
    public List<Cliente> listarTodos() {
        return service.listarTodos();
    }

    @GetMapping("/{id}")
    public Cliente buscarPorId(@PathVariable String id) {
        return service.buscarPorId(id);
    }

    @GetMapping("/{id}/historico")
    public ClienteHistoricoDTO buscarHistorico(@PathVariable String id) {
    return service.buscarHistorico(id);
    }
    @PostMapping
    public ResponseEntity<Cliente> criar(@Valid @RequestBody Cliente objeto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.salvar(objeto));
    }

    @PutMapping("/{id}")
    public Cliente atualizar(@PathVariable String id, @Valid @RequestBody Cliente objeto) {
        return service.atualizar(id, objeto);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluir(@PathVariable String id) {
        service.excluir(id);
        return ResponseEntity.noContent().build();
    }
}
