package sr.repository;

import sr.model.Kyc;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface KycRepository extends JpaRepository<Kyc, String> {

    List<Kyc> findByCliente_IdCliente(String idCliente);
}