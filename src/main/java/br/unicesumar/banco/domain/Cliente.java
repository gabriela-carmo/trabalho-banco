package br.unicesumar.banco.domain;

/**
 * Titular de uma conta. É um record: imutável, com equals/hashCode/toString prontos.
 */
public record Cliente(String nome, String cpf) {

    // Construtor compacto: roda antes de atribuir os campos, ótimo pra validar
    public Cliente {
        if (nome == null || nome.isBlank()) {
            throw new IllegalArgumentException("Nome do cliente é obrigatório");
        }
        if (cpf == null || cpf.isBlank()) {
            throw new IllegalArgumentException("CPF do cliente é obrigatório");
        }
    }
}
