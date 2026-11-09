package sr.service;

import sr.exception.RecursoNaoEncontradoException;
import sr.model.Kyc;
import sr.repository.KycRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class KycService {

    private final KycRepository repository;

    public KycService(KycRepository repository) {
        this.repository = repository;
    }

    public List<Kyc> listarTodos() {
        return repository.findAll();
    }

    public List<Kyc> listarPendentes() { return repository.buscarPendentes(); }
    public List<Kyc> listarConcluidos() { return repository.buscarConcluidos(); }

    public Kyc buscarPorId(String id) {
        return repository.findById(id).orElseThrow(() -> new RecursoNaoEncontradoException("Kyc não encontrado: " + id));
    }

    public Kyc salvar(Kyc objeto) {
        return repository.save(objeto);
    }

    public Kyc atualizar(String id, Kyc objeto) {
        buscarPorId(id);
        objeto.setIdKyc(id);
        return repository.save(objeto);
    }

    public void excluir(String id) {
        Kyc existente = buscarPorId(id);
        repository.delete(existente);
    }
}
