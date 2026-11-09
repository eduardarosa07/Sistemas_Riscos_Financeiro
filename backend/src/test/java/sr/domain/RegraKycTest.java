package sr.domain;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class RegraKycTest {
 @Test void rf35_aceitoERecusadoValidos(){assertTrue(RegraKyc.decisaoValida("ACEITO"));assertTrue(RegraKyc.decisaoValida("NAO_ACEITO"));}
 @Test void rf35_naoAceitaOutraDecisao(){assertFalse(RegraKyc.decisaoValida("PROCEDENTE"));assertFalse(RegraKyc.decisaoValida(null));}
 @Test void rf35_analistaPodeAssumirSemResponsavel(){assertTrue(RegraKyc.podeDecidir("PENDENTE", null, 1));}
 @Test void rf35_analistaAlheioNaoPodeDecidir(){assertFalse(RegraKyc.podeDecidir("PENDENTE", 2L, 1));}
 @Test void rf36_decisaoNaoPodeSerAlterada(){assertFalse(RegraKyc.podeDecidir("ACEITO", null, 1));assertFalse(RegraKyc.podeDecidir("NAO_ACEITO", 1L, 1));}
 @Test void rf35_recusaExigeInativacao(){assertTrue(RegraKyc.deveInativar("NAO_ACEITO"));assertFalse(RegraKyc.deveInativar("ACEITO"));}
 @Test void rf36_parecerObrigatorio(){assertFalse(RegraKyc.parecerValido(null));assertFalse(RegraKyc.parecerValido(" "));assertTrue(RegraKyc.parecerValido("Documentos conferidos"));}
 @Test void rf36_parecerLimitado(){assertTrue(RegraKyc.parecerValido("x".repeat(5000)));assertFalse(RegraKyc.parecerValido("x".repeat(5001)));}
}
