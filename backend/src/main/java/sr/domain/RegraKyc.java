
package sr.domain;

import java.util.Objects;

/**
 * Regras de negocio para analises KYC.
 *
 * Responsabilidades:
 *
 * 1. Validar as decisoes permitidas.
 * 2. Identificar analises ja encerradas.
 * 3. Controlar a responsabilidade do analista.
 * 4. Determinar quando inativar um cliente.
 * 5. Validar o parecer tecnico.
 *
 * COMPATIBILIDADE COM DADOS HISTORICOS:
 *
 * O sistema reconhece duas nomenclaturas:
 *
 * Modelo anterior:
 * - APROVADO
 * - REPROVADO
 *
 * Modelo atualizado:
 * - ACEITO
 * - NAO_ACEITO
 *
 * Todos esses status representam decisoes
 * definitivas e nao permitem nova decisao KYC.
 */
public final class RegraKyc {

    /**
     * Impede a criacao de instancias.
     *
     * Esta classe possui somente metodos
     * estaticos de regras de negocio.
     */
    private RegraKyc() {
    }

    /**
     * Verifica se a nova decisao e valida.
     *
     * O sistema atualizado permite apenas:
     * ACEITO e NAO_ACEITO.
     *
     * APROVADO e REPROVADO sao reconhecidos
     * somente como estados historicos.
     */
    public static boolean decisaoValida(String decisao) {

        return "ACEITO".equals(decisao)
                || "NAO_ACEITO".equals(decisao);
    }

    /**
     * Identifica se uma analise esta encerrada.
     *
     * CORRECAO:
     *
     * Reconhece os quatro status definitivos
     * existentes no banco de dados.
     *
     * Isso impede que analises antigas sejam
     * submetidas novamente a decisao.
     */
    public static boolean encerrada(String status) {

        return "ACEITO".equalsIgnoreCase(status)
                || "NAO_ACEITO".equalsIgnoreCase(status)
                || "APROVADO".equalsIgnoreCase(status)
                || "REPROVADO".equalsIgnoreCase(status);
    }

    /**
     * Verifica se o analista pode decidir.
     *
     * Condicoes:
     *
     * 1. A analise nao pode estar encerrada.
     * 2. A ocorrencia pode estar sem responsavel.
     * 3. Se houver responsavel, deve ser
     *    o proprio analista autenticado.
     *
     * Uma analise encerrada nao pode
     * receber outra decisao.
     */
    public static boolean podeDecidir(
            String status,
            Long responsavelAtual,
            long analista) {

        boolean analiseDisponivel = !encerrada(status);

        boolean analistaAutorizado =
                responsavelAtual == null
                || Objects.equals(
                        responsavelAtual,
                        analista
                );

        return analiseDisponivel && analistaAutorizado;
    }

    /**
     * Define quando o cliente deve ser inativado.
     *
     * Nas novas decisoes KYC:
     *
     * ACEITO:
     * Nao altera automaticamente o status
     * cadastral do cliente.
     *
     * NAO_ACEITO:
     * Exige a inativacao do cliente.
     *
     * Esta regra nao modifica automaticamente
     * registros historicos.
     */
    public static boolean deveInativar(String decisao) {

        return "NAO_ACEITO".equals(decisao);
    }

    /**
     * Valida o parecer tecnico.
     *
     * Requisitos:
     *
     * - Nao pode ser nulo.
     * - Nao pode estar vazio.
     * - Deve conter informacao relevante.
     * - Limite maximo de 5000 caracteres.
     */
    public static boolean parecerValido(String parecer) {

        return parecer != null
                && !parecer.isBlank()
                && parecer.trim().length() <= 5000;
    }
}
