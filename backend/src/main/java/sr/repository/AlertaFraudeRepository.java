package sr.repository;

import sr.model.AlertaFraude;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public interface AlertaFraudeRepository extends JpaRepository<AlertaFraude, String>, JpaSpecificationExecutor<AlertaFraude> {
    List<AlertaFraude> findByClienteOrigem_IdClienteOrClienteDestino_IdClienteOrClienteVitima_IdClienteOrClienteSuspeito_IdCliente(
        String idClienteOrigem, String idClienteDestino, String idClienteVitima, String idClienteSuspeito);

    /** Adiciona apenas filtros informados, evitando parametros nulos sem tipo no PostgreSQL. */
    default List<AlertaFraude> filtrar(String status, String severidade, String tipoFraude,
            LocalDateTime dataInicial, LocalDateTime dataFinal, String busca) {
        return findAll((root, query, cb) -> {
            List<Predicate> filtros = new ArrayList<>();
            if (status != null) filtros.add(cb.equal(root.get("statusAnalise"), status));
            if (severidade != null) filtros.add(cb.equal(root.get("severidade"), severidade));
            if (tipoFraude != null) filtros.add(cb.equal(root.get("tipoFraude"), tipoFraude));
            if (dataInicial != null) filtros.add(cb.greaterThanOrEqualTo(root.get("dataAlerta"), dataInicial));
            if (dataFinal != null) filtros.add(cb.lessThanOrEqualTo(root.get("dataAlerta"), dataFinal));
            if (busca != null && !busca.isBlank()) {
                String termo = "%" + busca.trim().toLowerCase(Locale.ROOT)
                    .replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_") + "%";
                List<Predicate> clientes = new ArrayList<>();
                for (String papel : List.of("clienteOrigem", "clienteDestino", "clienteVitima", "clienteSuspeito")) {
                    var cliente = root.join(papel, JoinType.LEFT);
                    clientes.add(cb.like(cb.lower(cliente.get("nomeRazaoSocial")), termo, '\\'));
                    clientes.add(cb.like(cb.lower(cliente.get("documentoFicticio")), termo, '\\'));
                }
                filtros.add(cb.or(clientes.toArray(Predicate[]::new)));
            }
            if (query != null) query.orderBy(cb.desc(root.get("dataAlerta")));
            return cb.and(filtros.toArray(Predicate[]::new));
        });
    }
}