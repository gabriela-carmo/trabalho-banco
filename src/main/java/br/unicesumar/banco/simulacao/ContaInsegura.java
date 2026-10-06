package br.unicesumar.banco.simulacao;

import java.math.BigDecimal;

/**
 * VERSÃO DIDÁTICA E PROPOSITALMENTE ERRADA (só pro relatório "antes e depois").
 *
 * Igualzinha à Conta, mas SEM synchronized. "saldo = saldo.add(v)" são 3 passos:
 *   1) ler saldo   2) somar   3) escrever saldo
 * Se duas threads lerem o mesmo valor antes de qualquer uma escrever, uma soma se perde.
 *
 * NÃO use isso em nenhum outro lugar do projeto.
 */
public class ContaInsegura {

    private BigDecimal saldo;

    public ContaInsegura(BigDecimal saldoInicial) {
        this.saldo = saldoInicial;
    }

    public void depositar(BigDecimal valor) { // <- SEM synchronized (aqui mora o bug)
        saldo = saldo.add(valor);
    }

    public BigDecimal getSaldo() {
        return saldo;
    }
}
