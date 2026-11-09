package sr.domain;

import java.util.Set;
import java.util.Objects;

/** Regras de domínio puras, sem Spring ou PostgreSQL, que podem ser exercitadas por TDD. */
public final class RegraTratativa {
    private static final Set<String> DECISOES = Set.of("PROCEDENTE","IMPROCEDENTE","SEM_EVIDENCIAS_SUFICIENTES");
    private RegraTratativa() {}

    public static boolean podeAssumir(String estado, Long responsavelAtual, long analista) {
        return ("PENDENTE".equals(estado) && responsavelAtual == null)
            || ("AGUARDANDO_PARECER".equals(estado) && Objects.equals(responsavelAtual, analista));
    }
    public static boolean podeTratar(String estado, Long responsavelAtual, long analista) {
        return "EM_ANALISE".equals(estado) && Objects.equals(responsavelAtual, analista);
    }
    public static boolean decisaoValida(String decisao) {
        return decisao != null && DECISOES.contains(decisao);
    }
    public static boolean justificativaValida(String nota) {
        return nota != null && !nota.isBlank() && nota.trim().length() <= 5000;
    }
    public static boolean encerrada(String estado) {return "CONCLUIDA".equals(estado);}
}
