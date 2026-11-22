package sr.service;

import sr.exception.RecursoNaoEncontradoException;
import sr.model.Transacao;
import sr.repository.TransacaoRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TransacaoService {

    private final TransacaoRepository repository;

    public TransacaoService(TransacaoRepository repository) {
        this.repository = repository;
    }

    public List<Transacao> listarTodos() {
        return repository.findAll();
    }

    public Transacao buscarPorId(String id) {
        return repository.findById(id).orElseThrow(() -> new RecursoNaoEncontradoException("Transacao não encontrado: " + id));
    }

}
