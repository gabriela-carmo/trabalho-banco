package br.unicesumar.banco.domain;

import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Testes COM threads. Se alguém tirar o synchronized da Conta, estes testes quebram.
 */
class ContaConcorrenciaTest {

    private static final int THREADS = 16;
    private static final int OPERACOES = 2_000;

    /** Roda a mesma tarefa em várias threads que largam todas ao mesmo tempo. */
    private static void rodarEmParalelo(int qtdThreads, Runnable tarefa) throws InterruptedException {
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
                tarefa.run();
            });
            threads.add(t);
            t.start();
        }
        largada.countDown();
        for (Thread t : threads) {
            t.join();
        }
    }

    // O teste de concorrência exigido: saldo final tem que ser EXATO
    @RepeatedTest(5) // repete porque race condition nem sempre aparece de primeira
    void depositosConcorrentesNaoPerdemNenhumValor() throws Exception {
        Conta conta = new Conta(1L, new Cliente("Ana", "111"), BigDecimal.ZERO);

        rodarEmParalelo(THREADS, () -> {
            for (int i = 0; i < OPERACOES; i++) {
                conta.depositar(BigDecimal.ONE);
            }
        });

        BigDecimal esperado = BigDecimal.valueOf((long) THREADS * OPERACOES).setScale(2);
        assertEquals(esperado, conta.getSaldo());
        assertEquals(THREADS * OPERACOES, conta.getHistorico().size());
        assertTrue(conta.confereComHistorico());
    }

    @Test
    void saquesConcorrentesNuncaDeixamSaldoNegativo() throws Exception {
        // Saldo para exatamente 1000 saques de R$ 1,00; 16 threads tentam 2000 cada
        Conta conta = new Conta(1L, new Cliente("Ana", "111"), new BigDecimal("1000.00"));
        java.util.concurrent.atomic.AtomicInteger sucessos = new java.util.concurrent.atomic.AtomicInteger();

        rodarEmParalelo(THREADS, () -> {
            for (int i = 0; i < OPERACOES; i++) {
                try {
                    conta.sacar(BigDecimal.ONE);
                    sucessos.incrementAndGet();
                } catch (SaldoInsuficienteException ignorada) {
                    // esperado quando o saldo acaba
                }
            }
        });

        assertEquals(1000, sucessos.get(), "só 1000 saques podem dar certo");
        assertEquals(new BigDecimal("0.00"), conta.getSaldo());
    }

    @Test
    void transferenciasEmSentidosOpostosNaoTravamEConservamODinheiro() {
        Conta a = new Conta(1L, new Cliente("Ana", "111"), new BigDecimal("1000.00"));
        Conta b = new Conta(2L, new Cliente("Bruno", "222"), new BigDecimal("1000.00"));

        // Se houver deadlock, o teste estoura o tempo e FALHA (em vez de ficar travado pra sempre)
        assertTimeoutPreemptively(Duration.ofSeconds(20), () -> {
            Thread aParaB = new Thread(() -> repetirTransferencia(a, b));
            Thread bParaA = new Thread(() -> repetirTransferencia(b, a));
            aParaB.start();
            bParaA.start();
            aParaB.join();
            bParaA.join();
        });

        BigDecimal total = a.getSaldo().add(b.getSaldo());
        assertEquals(new BigDecimal("2000.00"), total, "transferência não cria nem destrói dinheiro");
        assertTrue(a.confereComHistorico());
        assertTrue(b.confereComHistorico());
    }

    private static void repetirTransferencia(Conta de, Conta para) {
        for (int i = 0; i < 20_000; i++) {
            try {
                de.transferirPara(para, BigDecimal.ONE);
            } catch (SaldoInsuficienteException ignorada) {
                // normal: às vezes a origem fica sem saldo por um instante
            }
        }
    }
}
