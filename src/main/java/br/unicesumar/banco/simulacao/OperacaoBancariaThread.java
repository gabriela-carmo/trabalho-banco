package br.unicesumar.banco.simulacao;

import br.unicesumar.banco.domain.Conta;
import br.unicesumar.banco.domain.SaldoInsuficienteException;

import java.math.BigDecimal;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CountDownLatch;

/**
 * Simula UM cliente fazendo várias operações aleatórias (depósito, saque ou transferência)
 * sobre as mesmas contas que as outras threads estão usando.
 *
 * Cada instância guarda o que ELA conseguiu depositar/sacar. No final, somando todas as
 * threads, a gente sabe exatamente quanto dinheiro deveria existir no banco.
 *
 * Os contadores são campos normais (sem synchronized) porque cada thread só mexe nos seus
 * próprios; o main só lê DEPOIS do join(), que garante a visibilidade dos valores.
 */
public class OperacaoBancariaThread implements Runnable {

    private final String nome;
    private final List<Conta> contas;
    private final int qtdOperacoes;
    private final Random random;
    private final CountDownLatch largada; // pra todas as threads começarem juntas (mais race!)

    private BigDecimal totalDepositado = BigDecimal.ZERO;
    private BigDecimal totalSacado = BigDecimal.ZERO;
    private int operacoesRecusadas = 0; // saldo insuficiente

    public OperacaoBancariaThread(String nome, List<Conta> contas, int qtdOperacoes,
                                  long semente, CountDownLatch largada) {
        this.nome = nome;
        this.contas = contas;
        this.qtdOperacoes = qtdOperacoes;
        this.random = new Random(semente); // semente fixa = simulação reproduzível
        this.largada = largada;
    }

    @Override
    public void run() {
        try {
            if (largada != null) {
                largada.await(); // espera o "tiro de largada"
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return;
        }

        for (int i = 0; i < qtdOperacoes; i++) {
            Conta origem = contas.get(random.nextInt(contas.size()));
            BigDecimal valor = BigDecimal.valueOf(100 + random.nextInt(4900), 2); // 1,00 a 49,99
            int tipo = random.nextInt(3);

            try {
                if (tipo == 0 || contas.size() < 2) {
                    origem.depositar(valor);
                    totalDepositado = totalDepositado.add(valor);
                } else if (tipo == 1) {
                    origem.sacar(valor);
                    totalSacado = totalSacado.add(valor);
                } else {
                    Conta destino = sortearOutra(origem);
                    origem.transferirPara(destino, valor); // não cria nem destrói dinheiro
                }
            } catch (SaldoInsuficienteException e) {
                operacoesRecusadas++; // faz parte do jogo: saldo acabou, operação negada
            }
        }
    }

    /** Sorteia uma conta diferente da origem. */
    private Conta sortearOutra(Conta origem) {
        Conta candidata;
        do {
            candidata = contas.get(random.nextInt(contas.size()));
        } while (candidata == origem);
        return candidata;
    }

    public String getNome() { return nome; }
    public BigDecimal getTotalDepositado() { return totalDepositado; }
    public BigDecimal getTotalSacado() { return totalSacado; }
    public int getOperacoesRecusadas() { return operacoesRecusadas; }
}
