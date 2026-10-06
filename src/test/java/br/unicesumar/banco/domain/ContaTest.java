package br.unicesumar.banco.domain;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

/** Testes das regras de negócio (sequenciais, sem threads). */
class ContaTest {

    private Conta ana;
    private Conta bruno;

    @BeforeEach
    void preparar() {
        ana = new Conta(1L, new Cliente("Ana", "111"), new BigDecimal("100.00"));
        bruno = new Conta(2L, new Cliente("Bruno", "222"), new BigDecimal("50.00"));
    }

    // 1
    @Test
    void depositarAumentaSaldoERegistraTransacao() {
        ana.depositar(new BigDecimal("25.50"));

        assertEquals(new BigDecimal("125.50"), ana.getSaldo());
        assertEquals(1, ana.getHistorico().size());
        assertEquals(TipoTransacao.DEPOSITO, ana.getHistorico().get(0).tipo());
    }

    // 2
    @Test
    void sacarDiminuiSaldo() throws Exception {
        ana.sacar(new BigDecimal("40.00"));

        assertEquals(new BigDecimal("60.00"), ana.getSaldo());
    }

    // 3
    @Test
    void sacarMaisQueOSaldoLancaExcecaoESaldoNaoMuda() {
        assertThrows(SaldoInsuficienteException.class, () -> ana.sacar(new BigDecimal("100.01")));

        assertEquals(new BigDecimal("100.00"), ana.getSaldo());
        assertTrue(ana.getHistorico().isEmpty(), "operação negada não pode entrar no extrato");
    }

    // 4
    @Test
    void valorNegativoOuZeroEhRejeitado() {
        assertThrows(IllegalArgumentException.class, () -> ana.depositar(new BigDecimal("-1.00")));
        assertThrows(IllegalArgumentException.class, () -> ana.depositar(BigDecimal.ZERO));
    }

    // 5
    @Test
    void transferirMoveDinheiroERegistraNasDuasContas() throws Exception {
        ana.transferirPara(bruno, new BigDecimal("30.00"));

        assertEquals(new BigDecimal("70.00"), ana.getSaldo());
        assertEquals(new BigDecimal("80.00"), bruno.getSaldo());
        assertEquals(TipoTransacao.TRANSFERENCIA_ENVIADA, ana.getHistorico().get(0).tipo());
        assertEquals(TipoTransacao.TRANSFERENCIA_RECEBIDA, bruno.getHistorico().get(0).tipo());
    }

    // 6
    @Test
    void transferirSemSaldoNaoAlteraNenhumaConta() {
        assertThrows(SaldoInsuficienteException.class, () -> ana.transferirPara(bruno, new BigDecimal("999.00")));

        assertEquals(new BigDecimal("100.00"), ana.getSaldo());
        assertEquals(new BigDecimal("50.00"), bruno.getSaldo());
    }

    // 7
    @Test
    void transferirParaAMesmaContaEhRejeitado() {
        assertThrows(IllegalArgumentException.class, () -> ana.transferirPara(ana, BigDecimal.TEN));
    }

    // 8
    @Test
    void saldoSempreBateComSaldoInicialMaisHistorico() throws Exception {
        ana.depositar(new BigDecimal("10.00"));
        ana.sacar(new BigDecimal("5.00"));
        ana.transferirPara(bruno, new BigDecimal("20.00"));

        assertTrue(ana.confereComHistorico());
        assertTrue(bruno.confereComHistorico());
    }

    // 9
    @Test
    void transacaoEhImutavelEValidaValor() {
        assertThrows(IllegalArgumentException.class, () ->
                new Transacao(1L, TipoTransacao.DEPOSITO, new BigDecimal("-5"), java.time.LocalDateTime.now(), null));
    }

    // 10
    @Test
    void retirarNaoPersistidasNaoDevolveAMesmaTransacaoDuasVezes() {
        ana.depositar(BigDecimal.ONE);
        assertEquals(1, ana.retirarNaoPersistidas().size());
        assertEquals(0, ana.retirarNaoPersistidas().size());
    }
}
