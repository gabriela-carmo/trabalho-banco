package br.unicesumar.banco.app;

import br.unicesumar.banco.domain.Conta;
import br.unicesumar.banco.domain.Transacao;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

/**
 * Parte de Input/Output: grava o extrato de uma conta num arquivo .txt.
 */
public class ExtratoExportador {

    private static final Locale PT_BR = Locale.forLanguageTag("pt-BR");
    private static final DateTimeFormatter DATA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    /** O formatador do Java usa espaço "não separável" depois do R$; trocamos por espaço normal. */
    private static String fmt(NumberFormat moeda, java.math.BigDecimal valor) {
        return moeda.format(valor).replace('\u00A0', ' ');
    }

    public void exportar(Conta conta, Path destino) throws IOException {
        // Cria a pasta (ex.: extratos/) se ainda não existir
        if (destino.getParent() != null) {
            Files.createDirectories(destino.getParent());
        }

        NumberFormat moeda = NumberFormat.getCurrencyInstance(PT_BR);
        // Pega uma cópia do extrato e o saldo; o try-with-resources fecha o arquivo no final
        List<Transacao> historico = conta.getHistorico();

        try (BufferedWriter out = Files.newBufferedWriter(destino, StandardCharsets.UTF_8)) {
            out.write("==================== EXTRATO ====================");
            out.newLine();
            out.write("Conta:   " + conta.getNumero());
            out.newLine();
            out.write("Titular: " + conta.getTitular().nome());
            out.newLine();
            out.write("Saldo inicial: " + fmt(moeda, conta.getSaldoInicial()));
            out.newLine();
            out.write("-------------------------------------------------");
            out.newLine();
            out.write(String.format("%-20s %-24s %14s  %s", "DATA/HORA", "TIPO", "VALOR", "CONTA REL."));
            out.newLine();

            for (Transacao t : historico) {
                String relacionada = (t.contaRelacionada() == null) ? "-" : t.contaRelacionada().toString();
                out.write(String.format("%-20s %-24s %14s  %s",
                        t.dataHora().format(DATA_HORA),
                        t.tipo(),
                        fmt(moeda, t.valorComSinal()), // com sinal: + entrou, - saiu
                        relacionada));
                out.newLine();
            }

            out.write("-------------------------------------------------");
            out.newLine();
            out.write("Total de transações: " + historico.size());
            out.newLine();
            out.write("Saldo final: " + fmt(moeda, conta.getSaldo()));
            out.newLine();
        }
    }
}
