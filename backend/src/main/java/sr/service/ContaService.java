package sr.service;

import sr.exception.RecursoNaoEncontradoException;
import sr.model.Conta;
import sr.repository.ContaRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ContaService {

    private final ContaRepository repository;

    public ContaService(ContaRepository repository) {
        this.repository = repository;
    }

    public List<Conta> listarTodos() {
        return repository.findAll();
    }

    public Conta buscarPorId(String id) {
        return repository.findById(id).orElseThrow(() -> new RecursoNaoEncontradoException("Conta não encontrada: " + id));
    }

}
