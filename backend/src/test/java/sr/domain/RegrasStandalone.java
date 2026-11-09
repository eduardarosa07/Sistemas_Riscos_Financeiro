package sr.domain;

/** Testes sem bibliotecas: executar com JAVAC + JAVA quando Maven estiver indisponível. */
public final class RegrasStandalone {
  private static int aprovados=0;
  private static void check(boolean ok,String caso) {if(!ok)throw new AssertionError(caso);aprovados++;}
  public static void main(String[] args) {
    check(RegraTratativa.podeAssumir("PENDENTE",null,1L),"Assumir pendente");
    check(!RegraTratativa.podeAssumir("PENDENTE",2L,1L),"Impedir apropriação");
    check(RegraTratativa.podeAssumir("AGUARDANDO_PARECER",1L,1L),"Retomar parecer próprio");
    check(!RegraTratativa.podeAssumir("AGUARDANDO_PARECER",2L,1L),"Proteger parecer de outro");
    check(!RegraTratativa.podeAssumir("CONCLUIDA",null,1L),"Encerramento definitivo");
    check(RegraTratativa.podeTratar("EM_ANALISE",1L,1L),"Tratar por responsável");
    check(!RegraTratativa.podeTratar("EM_ANALISE",2L,1L),"Bloquear responsável incorreto");
    check(RegraTratativa.decisaoValida("SEM_EVIDENCIAS_SUFICIENTES"),"Decisão alternativa");
    check(!RegraTratativa.decisaoValida(null),"Decisão ausente");
    check(RegraTratativa.justificativaValida("Parecer válido"),"Parecer não vazio");
    check(!RegraTratativa.justificativaValida(" "),"Parecer vazio");
    check(!RegraTratativa.justificativaValida("X".repeat(5001)),"Limite de caracteres");
    check(RegraKyc.decisaoValida("ACEITO"),"KYC aceito");
    check(RegraKyc.decisaoValida("NAO_ACEITO"),"KYC recusado");
    check(!RegraKyc.decisaoValida("PROCEDENTE"),"KYC apenas duas decisões");
    check(RegraKyc.deveInativar("NAO_ACEITO"),"Recusa inativa");
    check(!RegraKyc.deveInativar("ACEITO"),"Aceite não inativa");
    check(!RegraKyc.podeDecidir("ACEITO",null,1L),"Não redecidir KYC concluído");
    check(!RegraKyc.podeDecidir("PENDENTE",2L,1L),"Não decidir KYC de outro");
    check(RegraKyc.parecerValido("Evidências registradas"),"Parecer KYC");
    System.out.println("APROVADOS: "+aprovados+" testes de regras de domínio. Não são testes de integração.");
  }
}
