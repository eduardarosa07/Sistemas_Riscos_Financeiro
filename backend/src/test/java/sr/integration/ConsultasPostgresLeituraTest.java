package sr.integration;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import sr.repository.AlertaFraudeRepository;
import sr.repository.ClienteRepository;
import sr.repository.KycRepository;
import sr.service.ClienteService;
import static org.junit.jupiter.api.Assertions.*;

/** Opt-in: somente SELECT, sem bootstrap, sincronizacao ou alteracao de schema. */
@EnabledIfEnvironmentVariable(named="DB_READONLY_VALIDATION", matches="true")
@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.NONE, properties={
    "sgr.sincronizacao.automatica=false", "app.bootstrap-admin.email=", "app.bootstrap-admin.password=",
    "spring.jpa.hibernate.ddl-auto=validate", "spring.sql.init.mode=never",
    "spring.datasource.hikari.connection-init-sql=SET default_transaction_read_only = on",
    "logging.level.org.springframework=OFF", "logging.level.org.hibernate=OFF"
})
@Transactional(readOnly=true)
class ConsultasPostgresLeituraTest {
    @Autowired JdbcTemplate jdbc;
    @Autowired KycRepository kyc;
    @Autowired AlertaFraudeRepository fraude;
    @Autowired ClienteRepository clientes;
    @Autowired ClienteService historico;

    @Test void conexaoSomenteLeitura() {
        assertEquals("on", jdbc.queryForObject("SHOW transaction_read_only", String.class));
    }
    @Test void filaEHistoricoKycCorrespondemAosEstadosPersistidos() {
        var pendentes=kyc.buscarPendentes();var concluidos=kyc.buscarConcluidos();
        assertEquals(kyc.count(), (long)pendentes.size()+concluidos.size());
        assertTrue(pendentes.stream().noneMatch(k->sr.domain.RegraKyc.encerrada(k.getStatusKyc())));
        assertTrue(concluidos.stream().allMatch(k->sr.domain.RegraKyc.encerrada(k.getStatusKyc())));
    }
    @Test void fraudeSemFiltrosNaoFalhaNemPerdeRegistros() {
        assertEquals(fraude.count(), (long)fraude.filtrar(null,null,null,null,null,null).size());
    }
    @Test void buscaFraudePreservaRelacionamentosOpcionais() {
        var registros=fraude.findAll();
        if(registros.isEmpty())return;
        var amostra=registros.stream().filter(a->a.getClienteDestino()==null||a.getClienteVitima()==null||a.getClienteSuspeito()==null)
            .findFirst().orElse(registros.getFirst());
        String documento=amostra.getClienteOrigem().getDocumentoFicticio();
        var encontrados=fraude.filtrar(null,null,null,null,null,documento);
        assertTrue(encontrados.stream().anyMatch(a->a.getIdAlertaFraude().equals(amostra.getIdAlertaFraude())));
    }
    @Test void historicoConsolidadoPodeSerConsultado() {
        var registros=clientes.findAll();
        if(registros.isEmpty())return;
        assertNotNull(historico.buscarHistorico(registros.getFirst().getIdCliente()).getCliente());
    }
}
