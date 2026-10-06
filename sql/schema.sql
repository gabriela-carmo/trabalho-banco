-- =====================================================================
-- Script de criação das tabelas (entregável da professora)
-- Banco: MySQL 8+
-- Uso:   mysql -u root -p < sql/schema.sql
-- =====================================================================
CREATE DATABASE IF NOT EXISTS banco_concorrente
    CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE banco_concorrente;

-- Contas: o número da conta é escolhido pela aplicação (é ele que define
-- a ORDEM DE BLOQUEIO nas transferências, então precisa existir antes do INSERT).
CREATE TABLE IF NOT EXISTS contas (
    numero        BIGINT        NOT NULL PRIMARY KEY,
    titular_nome  VARCHAR(120)  NOT NULL,
    titular_cpf   VARCHAR(14)   NOT NULL,
    saldo_inicial DECIMAL(15,2) NOT NULL,   -- usado pra conferir saldo = inicial + histórico
    saldo         DECIMAL(15,2) NOT NULL,
    CONSTRAINT ck_saldo_nao_negativo CHECK (saldo >= 0)
) ENGINE=InnoDB;

-- Transações: registro imutável de cada operação (só INSERT e SELECT, nunca UPDATE)
CREATE TABLE IF NOT EXISTS transacoes (
    id                BIGINT        NOT NULL AUTO_INCREMENT PRIMARY KEY,
    conta_numero      BIGINT        NOT NULL,
    tipo              VARCHAR(30)   NOT NULL,  -- DEPOSITO, SAQUE, TRANSFERENCIA_ENVIADA, TRANSFERENCIA_RECEBIDA
    valor             DECIMAL(15,2) NOT NULL,
    data_hora         DATETIME(6)   NOT NULL,
    conta_relacionada BIGINT        NULL,      -- a outra ponta da transferência
    CONSTRAINT fk_transacao_conta FOREIGN KEY (conta_numero) REFERENCES contas (numero),
    INDEX idx_transacao_conta (conta_numero, id)
) ENGINE=InnoDB;
