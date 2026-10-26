package sr.repository;

import sr.model.Chargeback;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ChargebackRepository extends JpaRepository<Chargeback, String> {

    List<Chargeback> findByCliente_IdCliente(String idCliente);
}