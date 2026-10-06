package br.unicesumar.banco.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

/**
 * Registro IMUTÁVEL de uma operação que já aconteceu (por isso é record:
 * não tem setter, não dá pra alterar depois de criado).
 *
 * @param contaNumero      conta à qual esta linha do extrato pertence
 * @param tipo             depósito, saque, transferência enviada/recebida
 * @param valor            sempre positivo; o sinal vem do {@link TipoTransacao}
 * @param dataHora         momento em que a operação foi aplicada
 * @param contaRelacionada a outra conta numa transferência (null em depósito/saque)
 */
public record Transacao(long contaNumero,
                        TipoTransacao tipo,
                        BigDecimal valor,
                        LocalDateTime dataHora,
                        Long contaRelacionada) {

    public Transacao {
        if (tipo == null || valor == null || dataHora == null) {
            throw new IllegalArgumentException("Tipo, valor e data/hora são obrigatórios");
        }
        // Dinheiro sempre com 2 casas decimais (centavos)
        valor = valor.setScale(2, RoundingMode.HALF_EVEN);
        if (valor.signum() <= 0) {
            throw new IllegalArgumentException("Valor da transação deve ser positivo");
        }
    }

    /** Valor com sinal: positivo se entrou dinheiro, negativo se saiu. */
    public BigDecimal valorComSinal() {
        return tipo.isCredito() ? valor : valor.negate();
    }
}
