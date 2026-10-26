package sr.exception;

public class ClienteDuplicadoException extends RuntimeException {

    public ClienteDuplicadoException(String mensagem) {
        super(mensagem);
    }
}