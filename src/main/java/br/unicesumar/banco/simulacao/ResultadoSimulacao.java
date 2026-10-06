package br.unicesumar.banco.simulacao;

import java.math.BigDecimal;

/**
 * Resumo de uma rodada de simulação.
 *
 * @param saldoTotalAntes  soma dos saldos de todas as contas antes de começar
 * @param totalDepositado  soma de tudo que as threads depositaram com sucesso
 * @param totalSacado      soma de tudo que as threads sacaram com sucesso
 * @param saldoTotalDepois soma dos saldos REAIS depois da simulação
 * @param recusadas        operações negadas por saldo insuficiente
 * @param duracaoMs        tempo gasto
 */
public record ResultadoSimulacao(BigDecimal saldoTotalAntes,
                                 BigDecimal totalDepositado,
                                 BigDecimal totalSacado,
                                 BigDecimal saldoTotalDepois,
                                 int recusadas,
                                 long duracaoMs) {

    /** Transferências são neutras: o que sai de uma conta entra na outra. */
    public BigDecimal saldoTotalEsperado() {
        return saldoTotalAntes.add(totalDepositado).subtract(totalSacado);
    }

    /** O banco "bate" quando o saldo real é exatamente o esperado. */
    public boolean bateExatamente() {
        return saldoTotalEsperado().compareTo(saldoTotalDepois) == 0;
    }
}
