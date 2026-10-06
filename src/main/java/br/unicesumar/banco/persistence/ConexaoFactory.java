package br.unicesumar.banco.persistence;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Abre conexões JDBC com o MySQL.
 * Os valores padrão batem com o docker-compose.yml; pra mudar, use variáveis de ambiente:
 * DB_URL, DB_USER, DB_PASSWORD (assim a senha nunca vai pro Git).
 */
public final class ConexaoFactory {

    private static final String URL_PADRAO =
            "jdbc:mysql://localhost:3306/banco_concorrente"
                    + "?useSSL=false&allowPublicKeyRetrieval=true"
                    + "&rewriteBatchedStatements=true"; // acelera INSERT em lote

    private ConexaoFactory() {
    }

    public static Connection abrir() throws SQLException {
        String url = System.getenv().getOrDefault("DB_URL", URL_PADRAO);
        String usuario = System.getenv().getOrDefault("DB_USER", "root");
        String senha = System.getenv().getOrDefault("DB_PASSWORD", "root");
        // O driver mysql-connector-j se registra sozinho (JDBC 4+), não precisa de Class.forName
        return DriverManager.getConnection(url, usuario, senha);
    }
}
