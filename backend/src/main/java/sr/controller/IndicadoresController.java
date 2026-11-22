package sr.controller;

import java.util.List;
import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import sr.service.IndicadoresService;

/**
 * API de indicadores gerenciais.
 *
 * A validação dos filtros e as consultas ao PostgreSQL pertencem à camada de
 * serviço; o controller preserva somente o contrato HTTP consumido pelo
 * Angular.
 */
@RestController
@RequestMapping("/api/indicadores")
public class IndicadoresController {

    private final IndicadoresService service;

    public IndicadoresController(IndicadoresService service) {
        this.service = service;
    }

    @GetMapping("/resumo")
    public Map<String, Long> resumo() {
        return service.resumo();
    }

    @GetMapping("/ocorrencias")
    public Map<String, Object> ocorrencias(
            @RequestParam(required = false) String inicio,
            @RequestParam(required = false) String fim,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String categoria,
            @RequestParam(required = false) String produto) {

        return service.ocorrencias(inicio, fim, status, categoria, produto);
    }

    @GetMapping("/produtos")
    public List<String> produtos() {
        return service.produtos();
    }
}
