package br.unicesumar.banco.persistence;

import br.unicesumar.banco.domain.TipoTransacao;
import br.unicesumar.banco.domain.Transacao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

/**
 * Todo o SQL de transações fica AQUI. O resto do sistema não sabe que existe banco.
 * Não faz commit: quem controla a transação JDBC é quem chama (ver Main).
 */
public class TransacaoRepository {

    private final Connection conexao;

    public TransacaoRepository(Connection conexao) {
        this.conexao = conexao;
    }

    /** Insere várias transações de uma vez (batch = bem mais rápido que um INSERT por vez). */
    public void salvarTodas(List<Transacao> transacoes) throws SQLException {
        String sql = "INSERT INTO transacoes (conta_numero, tipo, valor, data_hora, conta_relacionada) "
                + "VALUES (?, ?, ?, ?, ?)";
        // try-with-resources fecha o PreparedStatement sozinho, mesmo se der erro
        try (PreparedStatement ps = conexao.prepareStatement(sql)) {
            for (Transacao t : transacoes) {
                ps.setLong(1, t.contaNumero());
                ps.setString(2, t.tipo().name());
                ps.setBigDecimal(3, t.valor());
                ps.setTimestamp(4, Timestamp.valueOf(t.dataHora()));
                if (t.contaRelacionada() == null) {
                    ps.setNull(5, Types.BIGINT);
                } else {
                    ps.setLong(5, t.contaRelacionada());
                }
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    /** Extrato completo de uma conta, na ordem em que as transações foram gravadas. */
    public List<Transacao> buscarPorConta(long contaNumero) throws SQLException {
        String sql = "SELECT conta_numero, tipo, valor, data_hora, conta_relacionada "
                + "FROM transacoes WHERE conta_numero = ? ORDER BY id";
        List<Transacao> resultado = new ArrayList<>();
        try (PreparedStatement ps = conexao.prepareStatement(sql)) {
            ps.setLong(1, contaNumero);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    long relacionada = rs.getLong("conta_relacionada");
                    Long contaRelacionada = rs.wasNull() ? null : relacionada;
                    resultado.add(new Transacao(
                            rs.getLong("conta_numero"),
                            TipoTransacao.valueOf(rs.getString("tipo")),
                            rs.getBigDecimal("valor"),
                            rs.getTimestamp("data_hora").toLocalDateTime(),
                            contaRelacionada));
                }
            }
        }
        return resultado;
    }
}
