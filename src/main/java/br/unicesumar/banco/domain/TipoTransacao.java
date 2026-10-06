package br.unicesumar.banco.domain;

/**
 * Tipos de operação que podem aparecer no extrato.
 *
 * A transferência gera DUAS transações: uma ENVIADA na conta de origem
 * e uma RECEBIDA na conta de destino. Assim cada extrato conta a história completa da conta.
 */
public enum TipoTransacao {
    DEPOSITO(true),                // entra dinheiro
    SAQUE(false),                  // sai dinheiro
    TRANSFERENCIA_ENVIADA(false),  // sai dinheiro
    TRANSFERENCIA_RECEBIDA(true);  // entra dinheiro

    private final boolean credito;

    TipoTransacao(boolean credito) {
        this.credito = credito;
    }

    /** true se a operação AUMENTA o saldo da conta. */
    public boolean isCredito() {
        return credito;
    }
}
