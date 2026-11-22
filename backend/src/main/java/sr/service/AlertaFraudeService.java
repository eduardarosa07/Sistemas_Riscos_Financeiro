package sr.service;

import sr.exception.RecursoNaoEncontradoException;
import sr.model.AlertaFraude;
import sr.repository.AlertaFraudeRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.time.LocalDateTime;

@Service
public class AlertaFraudeService {

    private final AlertaFraudeRepository repository;

    public AlertaFraudeService(AlertaFraudeRepository repository) {
        this.repository = repository;
    }

    public List<AlertaFraude> filtrar(
        String status,
        String severidade,
        String tipoFraude,
        LocalDateTime dataInicial,
        LocalDateTime dataFinal,
        String busca
    ) {
    return repository.filtrar(
            status,
            severidade,
            tipoFraude,
            dataInicial,
            dataFinal,
            busca
    );
    }

    public List<AlertaFraude> listarTodos() {
        return repository.findAll();
    }

    public AlertaFraude buscarPorId(String id) {
        return repository.findById(id).orElseThrow(() -> new RecursoNaoEncontradoException("AlertaFraude não encontrado: " + id));
    }

}
