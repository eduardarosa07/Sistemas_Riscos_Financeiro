package sr.service;

import sr.dto.ClienteHistoricoDTO;
import sr.exception.RecursoNaoEncontradoException;
import sr.model.*;
import sr.repository.*;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class ClienteService {

    private final ClienteRepository clienteRepository;
    private final ContaRepository contaRepository;
    private final TransacaoRepository transacaoRepository;
    private final KycRepository kycRepository;
    private final AlertaFraudeRepository alertaFraudeRepository;
    private final AlertaPldRepository alertaPldRepository;
    private final ChargebackRepository chargebackRepository;
    private final RiscoClienteRepository riscoClienteRepository;
    private final JdbcTemplate jdbc;

    public ClienteService(
            ClienteRepository clienteRepository,
            ContaRepository contaRepository,
            TransacaoRepository transacaoRepository,
            KycRepository kycRepository,
            AlertaFraudeRepository alertaFraudeRepository,
            AlertaPldRepository alertaPldRepository,
            ChargebackRepository chargebackRepository,
            RiscoClienteRepository riscoClienteRepository,
            JdbcTemplate jdbc) {

        this.clienteRepository = clienteRepository;
        this.contaRepository = contaRepository;
        this.transacaoRepository = transacaoRepository;
        this.kycRepository = kycRepository;
        this.alertaFraudeRepository = alertaFraudeRepository;
        this.alertaPldRepository = alertaPldRepository;
        this.chargebackRepository = chargebackRepository;
        this.riscoClienteRepository = riscoClienteRepository;
        this.jdbc = jdbc;
    }

    /**
     * Consulta a visão consolidada sem criar ou recalcular o risco do cliente.
     * Score, nível e contadores são lidos diretamente da fonte externa.
     */
    public List<Map<String, Object>> listarComRisco() {
        return jdbc.queryForList("""
            SELECT c.id_cliente AS "idCliente",c.tipo_pessoa AS "tipoPessoa",
             c.nome_razao_social AS "nomeRazaoSocial",c.documento_ficticio AS "documentoFicticio",
             c.status_cliente AS "statusCliente",c.cidade,c.uf,
             COALESCE(r.nivel_risco,'NÃO CLASSIFICADO') AS "riscoGeral",
             r.score_risco_geral AS "scoreRiscoGeral",
             COALESCE(r.qtd_alertas_fraude,0) AS "qtdAlertasFraude",
             COALESCE(r.qtd_alertas_pld,0) AS "qtdAlertasPld",
             COALESCE(r.qtd_chargebacks,0) AS "qtdChargebacks"
            FROM public.dim_cliente c
            LEFT JOIN public.fato_risco_cliente r ON r.id_cliente=c.id_cliente
            ORDER BY c.nome_razao_social,c.id_cliente
            """);
    }

    public List<Cliente> listarTodos() {
        return clienteRepository.findAll();
    }

    public Cliente buscarPorId(String id) {
        return clienteRepository.findById(id)
                .orElseThrow(() ->
                        new RecursoNaoEncontradoException(
                                "Cliente não encontrado: " + id
                        )
                );
    }

    public ClienteHistoricoDTO buscarHistorico(String idCliente) {

        Cliente cliente = buscarPorId(idCliente);

        List<Conta> contas =
                contaRepository.findByCliente_IdCliente(idCliente);

        List<Transacao> transacoes =
                transacaoRepository
                        .findByClienteOrigem_IdClienteOrClienteDestino_IdCliente(
                                idCliente,
                                idCliente
                        );

        List<Kyc> kycs =
                kycRepository.findByCliente_IdCliente(idCliente);

        List<AlertaFraude> alertasFraude =
                alertaFraudeRepository
                        .findByClienteOrigem_IdClienteOrClienteDestino_IdClienteOrClienteVitima_IdClienteOrClienteSuspeito_IdCliente(
                                idCliente,
                                idCliente,
                                idCliente,
                                idCliente
                        );

        List<AlertaPld> alertasPld =
                alertaPldRepository
                        .findByClienteAnalisado_IdCliente(idCliente);

        List<Chargeback> chargebacks =
                chargebackRepository
                        .findByCliente_IdCliente(idCliente);

        RiscoCliente riscoCliente =
                riscoClienteRepository.findById(idCliente)
                        .orElse(null);

        return new ClienteHistoricoDTO(
                cliente,
                contas,
                transacoes,
                kycs,
                alertasFraude,
                alertasPld,
                chargebacks,
                riscoCliente
        );
    }
}
