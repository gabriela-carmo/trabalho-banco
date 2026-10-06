package br.unicesumar.banco.app;

import br.unicesumar.banco.domain.Cliente;
import br.unicesumar.banco.domain.Conta;
import br.unicesumar.banco.persistence.ConexaoFactory;
import br.unicesumar.banco.persistence.ContaRepository;
import br.unicesumar.banco.persistence.TransacaoRepository;
import br.unicesumar.banco.simulacao.ResultadoSimulacao;
import br.unicesumar.banco.simulacao.SimuladorConcorrente;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;

/**
 * Fluxo completo:
 *  1) carrega as contas do banco (ou cria, se for a primeira vez)
 *  2) dispara N threads fazendo depósitos/saques/transferências
 *  3) confere se o saldo bate EXATAMENTE
 *  4) grava contas + transações no MySQL (numa transação JDBC: tudo ou nada)
 *  5) exporta o extrato de uma conta pra .txt
 *
 * Rodar (com o MySQL de pé e o schema.sql aplicado):  mvn compile exec:java
 */
public class Main {

    private static final int QTD_THREADS = 8;
    private static final int OPERACOES_POR_THREAD = 1_000;
    private static final long SEMENTE = 42L;

    public static void main(String[] args) throws Exception {
        try (Connection con = ConexaoFactory.abrir()) {
            // Desliga o commit automático: a gente decide quando "confirmar" tudo de uma vez
            con.setAutoCommit(false);

            TransacaoRepository transacaoRepo = new TransacaoRepository(con);
            ContaRepository contaRepo = new ContaRepository(con, transacaoRepo);

            // 1) Carrega (ou cria) 3 contas. UM objeto Conta por número, compartilhado pelas threads.
            List<Conta> contas = new ArrayList<>();
            contas.add(carregarOuCriar(contaRepo, 1001L, "Ana Souza", "111.111.111-11"));
            contas.add(carregarOuCriar(contaRepo, 1002L, "Bruno Lima", "222.222.222-22"));
            contas.add(carregarOuCriar(contaRepo, 1003L, "Carla Dias", "333.333.333-33"));

            System.out.println("Saldos antes: ");
            contas.forEach(c -> System.out.println("  " + c));

            // 2 e 3) Simulação concorrente + conferência
            SimuladorConcorrente simulador = new SimuladorConcorrente();
            ResultadoSimulacao r = simulador.executar(contas, QTD_THREADS, OPERACOES_POR_THREAD, SEMENTE);

            System.out.println("\nSaldos depois: ");
            contas.forEach(c -> System.out.println("  " + c));
            System.out.println("\nTotal antes:    " + r.saldoTotalAntes());
            System.out.println("Depositado:     " + r.totalDepositado());
            System.out.println("Sacado:         " + r.totalSacado());
            System.out.println("Esperado:       " + r.saldoTotalEsperado());
            System.out.println("Real:           " + r.saldoTotalDepois());
            System.out.println("Recusadas:      " + r.recusadas() + " (saldo insuficiente)");
            System.out.println("Tempo:          " + r.duracaoMs() + " ms");
            boolean historicoOk = contas.stream().allMatch(Conta::confereComHistorico);
            System.out.println("Saldo bate com o histórico de cada conta? " + historicoOk);
            System.out.println(r.bateExatamente() && historicoOk ? ">>> SALDO BATE EXATAMENTE" : ">>> ERRO: SALDO CORROMPIDO");

            // 4) Persistência: tudo numa transação JDBC
            try {
                for (Conta c : contas) {
                    contaRepo.salvar(c); // a conta precisa existir antes (chave estrangeira)
                }
                for (Conta c : contas) {
                    transacaoRepo.salvarTodas(c.retirarNaoPersistidas());
                }
                con.commit();   // deu tudo certo: confirma
                System.out.println("\nContas e transações gravadas no MySQL.");
            } catch (Exception e) {
                con.rollback(); // deu ruim: desfaz tudo
                throw e;
            }

            // 5) Extrato em arquivo
            Path arquivo = Path.of("extratos", "extrato_1001.txt");
            new ExtratoExportador().exportar(contas.get(0), arquivo);
            System.out.println("Extrato exportado em: " + arquivo.toAbsolutePath());
        }
    }

    private static Conta carregarOuCriar(ContaRepository repo, long numero, String nome, String cpf)
            throws Exception {
        return repo.buscarPorNumero(numero)
                .orElseGet(() -> new Conta(numero, new Cliente(nome, cpf), new BigDecimal("1000.00")));
    }
}
