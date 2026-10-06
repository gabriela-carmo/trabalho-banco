package br.unicesumar.banco.simulacao;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.function.Consumer;

/**
 * Simulação simples só de depósitos, pra mostrar a race condition.
 * Recebe "o que fazer num depósito" como Consumer, então serve tanto pra
 * ContaInsegura (antes) quanto pra Conta (depois), com o MESMO código de teste.
 */
public class SimuladorDepositos {

    /** Cada thread faz {@code depositosPorThread} depósitos de R$ 1,00. */
    public static void rodar(int qtdThreads, int depositosPorThread, Consumer<BigDecimal> deposito)
            throws InterruptedException {

        CountDownLatch largada = new CountDownLatch(1);
        List<Thread> threads = new ArrayList<>();
        for (int i = 0; i < qtdThreads; i++) {
            Thread t = new Thread(() -> {
                try {
                    largada.await();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
                for (int j = 0; j < depositosPorThread; j++) {
                    deposito.accept(BigDecimal.ONE);
                }
            });
            threads.add(t);
            t.start();
        }
        largada.countDown();
        for (Thread t : threads) {
            t.join();
        }
    }
}
