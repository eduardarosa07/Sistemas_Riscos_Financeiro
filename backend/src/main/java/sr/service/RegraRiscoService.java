package sr.service;

import sr.exception.RecursoNaoEncontradoException;
import sr.model.RegraRisco;
import sr.repository.RegraRiscoRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class RegraRiscoService {

    private final RegraRiscoRepository repository;

    public RegraRiscoService(RegraRiscoRepository repository) {
        this.repository = repository;
    }

    public List<RegraRisco> listarTodos() {
        return repository.findAll();
    }

    public RegraRisco buscarPorId(String id) {
        return repository.findById(id).orElseThrow(() -> new RecursoNaoEncontradoException("RegraRisco não encontrado: " + id));
    }

}
