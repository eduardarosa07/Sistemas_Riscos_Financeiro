package sr.repository;

import sr.model.Kyc;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface KycRepository extends JpaRepository<Kyc, String> {

    List<Kyc> findByCliente_IdCliente(String idCliente);
    @Query("select k from Kyc k where upper(trim(k.statusKyc)) in ('ACEITO','NAO_ACEITO','APROVADO','REPROVADO')")
    List<Kyc> buscarConcluidos();
    @Query("select k from Kyc k where upper(trim(coalesce(k.statusKyc,''))) not in ('ACEITO','NAO_ACEITO','APROVADO','REPROVADO')")
    List<Kyc> buscarPendentes();
}
