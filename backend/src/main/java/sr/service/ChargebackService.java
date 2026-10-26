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

    public Chargeback salvar(Chargeback objeto) {
        return repository.save(objeto);
    }

    public Chargeback atualizar(String id, Chargeback objeto) {
        buscarPorId(id);
        objeto.setIdChargeback(id);
        return repository.save(objeto);
    }

    public void excluir(String id) {
        Chargeback existente = buscarPorId(id);
        repository.delete(existente);
    }
}
