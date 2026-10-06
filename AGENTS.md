# AGENTS.md — instruções para agentes de IA neste projeto

Projeto acadêmico de POO (Java 17 + Maven + MySQL): sistema bancário com transações concorrentes.
Se você é um agente de código (Claude Code, Copilot, Codex etc.), siga estas regras.

## Contexto
- O objetivo pedagógico é a **race condition** em saldo e sua correção com sincronização, sem deadlock.
- Pacotes obrigatórios: `domain`, `persistence`, `simulacao`, `app` (em `br.unicesumar.banco`).

## Regras invioláveis
1. O saldo (`Conta.saldo`) é **privado** e só muda dentro de métodos/blocos `synchronized` de `Conta`.
2. `transferirPara()` bloqueia as duas contas **sempre pela ordem do número da conta**. Nunca inverta isso.
3. Dinheiro é `BigDecimal` com 2 casas. Proibido `double`/`float` para valores monetários.
4. `domain` não importa nada de `java.sql`, `persistence` nem `simulacao`.
5. Todo SQL fica em `persistence/` e usa `PreparedStatement` + try-with-resources. Nunca concatene valores em SQL.
6. Nenhuma chamada de banco ou I/O dentro de bloco `synchronized`.
7. `Transacao` é imutável (record). Não adicione setters.
8. Credenciais do banco vêm de variáveis de ambiente (`DB_URL`, `DB_USER`, `DB_PASSWORD`). Nunca commite senhas.

## Antes de dar um trabalho por terminado
- `mvn test` passa (inclui testes de concorrência e de deadlock).
- Se mexeu em `Conta`, rode `DemoRaceCondition` e confirme que o "DEPOIS" fecha sempre em 400000.00.
- Código novo com comentários em português explicando o porquê, no tom do restante do projeto.

## Comandos
- Testes: `mvn test`
- Demo antes/depois: `mvn compile exec:java -Dexec.mainClass=br.unicesumar.banco.app.DemoRaceCondition`
- Simulação completa: `mvn compile exec:java`
