package sr.service;

import sr.exception.RecursoNaoEncontradoException;
import sr.model.Chargeback;
import sr.repository.ChargebackRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ChargebackService {

    private final ChargebackRepository repository;

    public ChargebackService(ChargebackRepository repository) {
        this.repository = repository;
    }

    public List<Chargeback> listarTodos() {
        return repository.findAll();
    }

    public Chargeback buscarPorId(String id) {
        return repository.findById(id).orElseThrow(() -> new RecursoNaoEncontradoException("Chargeback não encontrado: " + id));
    }

}
