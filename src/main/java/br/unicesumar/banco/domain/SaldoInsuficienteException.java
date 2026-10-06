package br.unicesumar.banco.domain;

import java.math.BigDecimal;

/**
 * Lançada quando alguém tenta sacar/transferir mais do que tem.
 * É checked de propósito: o compilador obriga quem chama a decidir o que fazer.
 */
public class SaldoInsuficienteException extends Exception {

    public SaldoInsuficienteException(long numeroConta, BigDecimal saldo, BigDecimal pedido) {
        super("Saldo insuficiente na conta " + numeroConta
                + ": saldo=" + saldo + ", pedido=" + pedido);
    }
}
