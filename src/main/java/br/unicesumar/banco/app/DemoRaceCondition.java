package br.unicesumar.banco.app;

import br.unicesumar.banco.domain.Cliente;
import br.unicesumar.banco.domain.Conta;
import br.unicesumar.banco.simulacao.ContaInsegura;
import br.unicesumar.banco.simulacao.SimuladorDepositos;

import java.math.BigDecimal;

/**
 * Gera o "ANTES E DEPOIS" do relatório. NÃO precisa de banco de dados.
 *
 * Roda a mesma simulação 5 vezes com a conta SEM sincronização (resultado muda a cada
 * rodada e fica abaixo do esperado) e 5 vezes com a Conta sincronizada (sempre exato).
 *
 * Rodar: mvn compile exec:java -Dexec.mainClass=br.unicesumar.banco.app.DemoRaceCondition
 */
public class DemoRaceCondition {

    private static final int THREADS = 8;
    private static final int DEPOSITOS_POR_THREAD = 50_000;
    private static final int RODADAS = 5;

    public static void main(String[] args) throws InterruptedException {
        BigDecimal esperado = BigDecimal.valueOf((long) THREADS * DEPOSITOS_POR_THREAD);
        System.out.printf("%d threads x %d depósitos de R$ 1,00 -> saldo esperado: %s%n%n",
                THREADS, DEPOSITOS_POR_THREAD, esperado);

        System.out.println("=== ANTES: ContaInsegura (SEM synchronized) ===");
        for (int i = 1; i <= RODADAS; i++) {
            ContaInsegura conta = new ContaInsegura(BigDecimal.ZERO);
            SimuladorDepositos.rodar(THREADS, DEPOSITOS_POR_THREAD, conta::depositar);
            imprimir(i, conta.getSaldo(), esperado);
        }

        System.out.println();
        System.out.println("=== DEPOIS: Conta (COM synchronized) ===");
        for (int i = 1; i <= RODADAS; i++) {
            Conta conta = new Conta(1L, new Cliente("Teste", "000.000.000-00"), BigDecimal.ZERO);
            SimuladorDepositos.rodar(THREADS, DEPOSITOS_POR_THREAD, conta::depositar);
            imprimir(i, conta.getSaldo(), esperado);
        }
    }

    private static void imprimir(int rodada, BigDecimal real, BigDecimal esperado) {
        BigDecimal perdido = esperado.subtract(real);
        System.out.printf("Rodada %d: saldo real = %s | perdido = %s | %s%n",
                rodada, real, perdido, perdido.signum() == 0 ? "OK" : "CORROMPIDO");
    }
}
