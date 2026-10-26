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

    public Conta salvar(Conta objeto) {
        return repository.save(objeto);
    }

    public Conta atualizar(String id, Conta objeto) {
        buscarPorId(id);
        objeto.setIdConta(id);
        return repository.save(objeto);
    }

    public void excluir(String id) {
        Conta existente = buscarPorId(id);
        repository.delete(existente);
    }
}
