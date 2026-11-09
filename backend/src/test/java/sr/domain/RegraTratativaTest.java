package sr.domain;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class RegraTratativaTest {
 @Test void rf37_pendenteSemResponsavelPodeSerAssumida(){assertTrue(RegraTratativa.podeAssumir("PENDENTE", null, 10));}
 @Test void rf37_pendenteAtribuidaNaoPodeSerRoubada(){assertFalse(RegraTratativa.podeAssumir("PENDENTE", 20L, 10));}
 @Test void rf37_aguardandoParecerSoMesmoResponsavel(){assertTrue(RegraTratativa.podeAssumir("AGUARDANDO_PARECER", 10L, 10));assertFalse(RegraTratativa.podeAssumir("AGUARDANDO_PARECER", 20L, 10));}
 @Test void rf38_analistaNaoResponsavelNaoTrata(){assertFalse(RegraTratativa.podeTratar("EM_ANALISE", 20L, 10));}
 @Test void rf38_responsavelSoTrataEmAnalise(){assertTrue(RegraTratativa.podeTratar("EM_ANALISE", 10L, 10));assertFalse(RegraTratativa.podeTratar("PENDENTE", 10L, 10));}
 @Test void rf24_ocorrenciaEncerradaNaoReabre(){assertTrue(RegraTratativa.encerrada("CONCLUIDA"));assertFalse(RegraTratativa.podeAssumir("CONCLUIDA", null, 10));}
 @Test void rf23_decisaoSemEvidenciasSuficientesPermitida(){assertTrue(RegraTratativa.decisaoValida("SEM_EVIDENCIAS_SUFICIENTES"));}
 @Test void rf23_decisaoNulaInvalida(){assertFalse(RegraTratativa.decisaoValida(null));}
 @Test void rf23_justificativaObrigatoria(){assertFalse(RegraTratativa.justificativaValida("  "));assertTrue(RegraTratativa.justificativaValida("Parecer fundamentado"));}
 @Test void rf23_justificativaLimite(){assertTrue(RegraTratativa.justificativaValida("x".repeat(5000)));assertFalse(RegraTratativa.justificativaValida("x".repeat(5001)));}
}
