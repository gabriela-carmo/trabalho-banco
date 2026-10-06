package br.unicesumar.banco.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Conta bancária. É o ÚNICO lugar do sistema onde o saldo pode mudar.
 *
 * Regra de ouro de concorrência: existe UM objeto Conta por número de conta na memória.
 * O "cadeado" (monitor do synchronized) é o próprio objeto, então se dois objetos
 * representassem a mesma conta, eles NÃO se protegeriam um do outro.
 *
 * Domínio puro: não conhece JDBC, nem arquivo, nem thread de simulação.
 */
public class Conta {

    private final long numero;
    private final Cliente titular;
    private final BigDecimal saldoInicial;

    // PRIVADO: ninguém de fora mexe direto. Só os métodos synchronized abaixo alteram.
    private BigDecimal saldo;

    // Extrato em memória. Só é lido/alterado com o cadeado da conta.
    private final List<Transacao> historico = new ArrayList<>();

    // Quantas transações do histórico já foram gravadas no banco (controle da persistência)
    private int qtdPersistida = 0;

    /** Cria uma conta nova. */
    public Conta(long numero, Cliente titular, BigDecimal saldoInicial) {
        if (titular == null) {
            throw new IllegalArgumentException("Titular é obrigatório");
        }
        BigDecimal inicial = normalizar(saldoInicial);
        if (inicial.signum() < 0) {
            throw new IllegalArgumentException("Saldo inicial não pode ser negativo");
        }
        this.numero = numero;
        this.titular = titular;
        this.saldoInicial = inicial;
        this.saldo = inicial;
    }

    /**
     * Reconstrói uma conta que veio do banco (usado pelo ContaRepository).
     * O histórico já está salvo, então marcamos tudo como persistido.
     */
    public static Conta restaurar(long numero, Cliente titular, BigDecimal saldoInicial,
                                  BigDecimal saldoAtual, List<Transacao> historico) {
        Conta conta = new Conta(numero, titular, saldoInicial);
        conta.saldo = normalizar(saldoAtual);
        conta.historico.addAll(historico);
        conta.qtdPersistida = conta.historico.size();
        return conta;
    }

    // ------------------------------------------------------------------
    // OPERAÇÕES (todas protegidas pelo cadeado da conta)
    // ------------------------------------------------------------------

    /**
     * synchronized no método = só UMA thread por vez executa isso NESTA conta.
     * Sem isso, "saldo = saldo + valor" (ler, somar, escrever) pode se intercalar entre
     * threads e uma atualização é perdida: a race condition clássica.
     */
    public synchronized void depositar(BigDecimal valor) {
        BigDecimal v = validarValor(valor);
        saldo = saldo.add(v);
        registrar(TipoTransacao.DEPOSITO, v, null);
    }

    /**
     * A checagem "tem saldo?" e o desconto precisam ser UMA operação indivisível,
     * senão duas threads passam na checagem juntas e o saldo fica negativo.
     */
    public synchronized void sacar(BigDecimal valor) throws SaldoInsuficienteException {
        BigDecimal v = validarValor(valor);
        if (saldo.compareTo(v) < 0) {
            throw new SaldoInsuficienteException(numero, saldo, v);
        }
        saldo = saldo.subtract(v);
        registrar(TipoTransacao.SAQUE, v, null);
    }

    /**
     * Transferência mexe em DUAS contas, então precisa dos DOIS cadeados.
     *
     * Perigo de deadlock: A->B pega o cadeado de A e espera o de B, enquanto
     * B->A pega o cadeado de B e espera o de A. Ninguém sai do lugar.
     *
     * Solução: TODA transferência pega os cadeados na MESMA ORDEM (menor número primeiro).
     * Assim os dois sentidos disputam primeiro a mesma conta e um espera o outro, sem ciclo.
     *
     * Obs.: aqui usamos blocos synchronized (e não "synchronized" no método) porque
     * precisamos escolher QUAL objeto bloquear primeiro.
     */
    public void transferirPara(Conta destino, BigDecimal valor) throws SaldoInsuficienteException {
        if (destino == null) {
            throw new IllegalArgumentException("Conta de destino é obrigatória");
        }
        if (destino == this || destino.numero == this.numero) {
            throw new IllegalArgumentException("Não dá pra transferir para a mesma conta");
        }
        BigDecimal v = validarValor(valor);

        // Define a ordem de bloqueio pelo número da conta (critério fixo e igual pra todo mundo)
        Conta primeira = (this.numero < destino.numero) ? this : destino;
        Conta segunda = (primeira == this) ? destino : this;

        synchronized (primeira) {
            synchronized (segunda) {
                // Dentro dos dois cadeados: verificar + debitar + creditar é atômico
                if (this.saldo.compareTo(v) < 0) {
                    throw new SaldoInsuficienteException(this.numero, this.saldo, v);
                }
                this.saldo = this.saldo.subtract(v);
                destino.saldo = destino.saldo.add(v);
                this.registrar(TipoTransacao.TRANSFERENCIA_ENVIADA, v, destino.numero);
                destino.registrar(TipoTransacao.TRANSFERENCIA_RECEBIDA, v, this.numero);
            }
        }
    }

    // ------------------------------------------------------------------
    // CONSULTAS (também sincronizadas, pra nunca ler um valor "no meio" de uma operação)
    // ------------------------------------------------------------------

    public synchronized BigDecimal getSaldo() {
        return saldo;
    }

    /** Cópia imutável do extrato: quem recebe não consegue mexer na lista interna. */
    public synchronized List<Transacao> getHistorico() {
        return List.copyOf(historico);
    }

    /**
     * Confere a invariante: saldo atual == saldo inicial + soma de todas as transações.
     * Se isso for falso, alguma atualização se perdeu (ou foi duplicada).
     */
    public synchronized boolean confereComHistorico() {
        BigDecimal esperado = saldoInicial;
        for (Transacao t : historico) {
            esperado = esperado.add(t.valorComSinal());
        }
        return esperado.compareTo(saldo) == 0;
    }

    /**
     * Devolve só as transações que ainda não foram pro banco e marca elas como persistidas.
     * Assim, rodar a persistência duas vezes não duplica linhas.
     */
    public synchronized List<Transacao> retirarNaoPersistidas() {
        List<Transacao> pendentes = new ArrayList<>(historico.subList(qtdPersistida, historico.size()));
        qtdPersistida = historico.size();
        return pendentes;
    }

    // Getters de dados que nunca mudam (final) não precisam de synchronized
    public long getNumero() { return numero; }
    public Cliente getTitular() { return titular; }
    public BigDecimal getSaldoInicial() { return saldoInicial; }

    // ------------------------------------------------------------------
    // INTERNOS
    // ------------------------------------------------------------------

    /** Só é chamado com o cadeado desta conta já em mãos. */
    private void registrar(TipoTransacao tipo, BigDecimal valor, Long relacionada) {
        historico.add(new Transacao(numero, tipo, valor, LocalDateTime.now(), relacionada));
    }

    private static BigDecimal validarValor(BigDecimal valor) {
        BigDecimal v = normalizar(valor);
        if (v.signum() <= 0) {
            throw new IllegalArgumentException("Valor deve ser positivo");
        }
        return v;
    }

    private static BigDecimal normalizar(BigDecimal valor) {
        if (valor == null) {
            throw new IllegalArgumentException("Valor é obrigatório");
        }
        return valor.setScale(2, RoundingMode.HALF_EVEN);
    }

    @Override
    public String toString() {
        return "Conta{" + numero + ", " + titular.nome() + ", saldo=" + getSaldo() + "}";
    }
}
