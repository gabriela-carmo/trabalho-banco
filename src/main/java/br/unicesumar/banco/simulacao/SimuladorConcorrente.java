package br.unicesumar.banco.simulacao;

import br.unicesumar.banco.domain.Conta;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

/**
 * Dispara N threads de {@link OperacaoBancariaThread} sobre as mesmas contas e
 * confere se o dinheiro total bate no final.
 */
public class SimuladorConcorrente {

    public ResultadoSimulacao executar(List<Conta> contas, int qtdThreads, int operacoesPorThread, long semente)
            throws InterruptedException {

        BigDecimal antes = somarSaldos(contas);
        CountDownLatch largada = new CountDownLatch(1);

        List<OperacaoBancariaThread> tarefas = new ArrayList<>();
        List<Thread> threads = new ArrayList<>();
        for (int i = 0; i < qtdThreads; i++) {
            OperacaoBancariaThread tarefa = new OperacaoBancariaThread(
                    "cliente-" + (i + 1), contas, operacoesPorThread, semente + i, largada);
            tarefas.add(tarefa);
            threads.add(new Thread(tarefa, tarefa.getNome()));
        }

        long inicio = System.nanoTime();
        threads.forEach(Thread::start);  // todas prontas, esperando no latch
        largada.countDown();             // LARGADA! todas começam juntas
        for (Thread t : threads) {
            t.join();                    // espera todo mundo terminar
        }
        long duracaoMs = (System.nanoTime() - inicio) / 1_000_000;

        BigDecimal depositado = BigDecimal.ZERO;
        BigDecimal sacado = BigDecimal.ZERO;
        int recusadas = 0;
        for (OperacaoBancariaThread t : tarefas) {
            depositado = depositado.add(t.getTotalDepositado());
            sacado = sacado.add(t.getTotalSacado());
            recusadas += t.getOperacoesRecusadas();
        }

        return new ResultadoSimulacao(antes, depositado, sacado, somarSaldos(contas), recusadas, duracaoMs);
    }

    public static BigDecimal somarSaldos(List<Conta> contas) {
        BigDecimal soma = BigDecimal.ZERO;
        for (Conta c : contas) {
            soma = soma.add(c.getSaldo());
        }
        return soma;
    }
}
