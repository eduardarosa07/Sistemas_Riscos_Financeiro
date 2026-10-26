package sr.repository;

import sr.model.Conta;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ContaRepository extends JpaRepository<Conta, String> {

    List<Conta> findByCliente_IdCliente(String idCliente);
}