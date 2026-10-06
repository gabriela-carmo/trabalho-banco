package br.unicesumar.banco.persistence;

import br.unicesumar.banco.domain.Cliente;
import br.unicesumar.banco.domain.Conta;
import br.unicesumar.banco.domain.Transacao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

/**
 * Todo o SQL de contas fica AQUI. Também não faz commit.
 */
public class ContaRepository {

    private final Connection conexao;
    private final TransacaoRepository transacaoRepository;

    public ContaRepository(Connection conexao, TransacaoRepository transacaoRepository) {
        this.conexao = conexao;
        this.transacaoRepository = transacaoRepository;
    }

    /**
     * Cria a conta se não existir; se já existir, atualiza só o saldo.
     * (Não grava as transações: isso é com o TransacaoRepository.)
     */
    public void salvar(Conta conta) throws SQLException {
        if (existe(conta.getNumero())) {
            String sql = "UPDATE contas SET saldo = ? WHERE numero = ?";
            try (PreparedStatement ps = conexao.prepareStatement(sql)) {
                ps.setBigDecimal(1, conta.getSaldo());
                ps.setLong(2, conta.getNumero());
                ps.executeUpdate();
            }
        } else {
            String sql = "INSERT INTO contas (numero, titular_nome, titular_cpf, saldo_inicial, saldo) "
                    + "VALUES (?, ?, ?, ?, ?)";
            try (PreparedStatement ps = conexao.prepareStatement(sql)) {
                ps.setLong(1, conta.getNumero());
                ps.setString(2, conta.getTitular().nome());
                ps.setString(3, conta.getTitular().cpf());
                ps.setBigDecimal(4, conta.getSaldoInicial());
                ps.setBigDecimal(5, conta.getSaldo());
                ps.executeUpdate();
            }
        }
    }

    /** Recarrega a conta COM o extrato, pra continuar de onde a última execução parou. */
    public Optional<Conta> buscarPorNumero(long numero) throws SQLException {
        String sql = "SELECT numero, titular_nome, titular_cpf, saldo_inicial, saldo "
                + "FROM contas WHERE numero = ?";
        try (PreparedStatement ps = conexao.prepareStatement(sql)) {
            ps.setLong(1, numero);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return Optional.empty();
                }
                List<Transacao> historico = transacaoRepository.buscarPorConta(numero);
                Conta conta = Conta.restaurar(
                        rs.getLong("numero"),
                        new Cliente(rs.getString("titular_nome"), rs.getString("titular_cpf")),
                        rs.getBigDecimal("saldo_inicial"),
                        rs.getBigDecimal("saldo"),
                        historico);
                return Optional.of(conta);
            }
        }
    }

    private boolean existe(long numero) throws SQLException {
        try (PreparedStatement ps = conexao.prepareStatement("SELECT 1 FROM contas WHERE numero = ?")) {
            ps.setLong(1, numero);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }
}
