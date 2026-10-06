# Sistema Bancário com Transações Concorrentes

Trabalho do 2º bimestre — Programação Orientada a Objetos (Java) · UniCesumar/UNIPA Curitiba · Opção C.

Contas, depósitos, saques e transferências rodando em várias threads ao mesmo tempo, com saldo que **sempre bate**,
persistência em MySQL via JDBC e extrato exportável para `.txt`.

**Stack:** Java 17 · Maven · MySQL 8 · JUnit 5

## Estrutura

```
banco-concorrente/
├── pom.xml
├── docker-compose.yml          # MySQL pronto (opcional)
├── sql/schema.sql              # script das tabelas (entregável)
├── src/main/java/br/unicesumar/banco/
│   ├── domain/                 # Conta, Cliente, Transacao, TipoTransacao, SaldoInsuficienteException
│   ├── persistence/            # ConexaoFactory, ContaRepository, TransacaoRepository (JDBC)
│   ├── simulacao/              # OperacaoBancariaThread, SimuladorConcorrente, ContaInsegura (didática)
│   └── app/                    # Main, DemoRaceCondition, ExtratoExportador
├── src/test/java/.../domain/   # ContaTest (10) + ContaConcorrenciaTest (3, com threads)
├── exemplos/                   # extrato .txt de exemplo + log do antes/depois
├── docs/                       # arquitetura, sugestões, referências, relatório, parte de IA
├── AGENTS.md                   # instruções para agentes de IA que mexem no projeto
└── .claude/agents/             # sub-agente revisor de concorrência
```

## Como rodar (passo a passo)

1. **Subir o MySQL** (o `schema.sql` roda sozinho na primeira vez):
   ```bash
   docker compose up -d
   ```
   Sem Docker? Instale o MySQL 8 e rode `mysql -u root -p < sql/schema.sql`.
2. **Credenciais** (opcional): por padrão usa `root` / `root` em `localhost:3306`.
   Para mudar, defina `DB_URL`, `DB_USER` e `DB_PASSWORD` como variáveis de ambiente.
3. **Rodar os testes:**
   ```bash
   mvn test
   ```
4. **Demo do "antes e depois" da race condition** (não precisa de banco):
   ```bash
   mvn compile exec:java -Dexec.mainClass=br.unicesumar.banco.app.DemoRaceCondition
   ```
5. **Simulação completa** (threads + MySQL + extrato):
   ```bash
   mvn compile exec:java
   ```
   O extrato sai em `extratos/extrato_1001.txt`.

## Rodar sem instalar nada (GitHub Actions)

Suba o projeto num repositório no GitHub. A cada `git push` (ou pelo botão **Actions → Testes e simulação → Run workflow**)
a nuvem roda os testes, o demo da race condition e a simulação completa com MySQL.
Na página da execução, baixe o artefato **evidencias** (logs, extrato e relatório do JUnit) e use nos prints do relatório.

## Como a concorrência foi resolvida

- `Conta.depositar()` e `Conta.sacar()` são `synchronized`: verificar saldo e alterar saldo viram uma operação indivisível.
- `Conta.transferirPara()` bloqueia as **duas** contas sempre na **mesma ordem** (menor número de conta primeiro),
  o que elimina o deadlock quando A→B e B→A acontecem juntas.
- O saldo é `BigDecimal` (nunca `double`) e só muda dentro da `Conta`.
- A persistência acontece **depois** da simulação, em lote, numa transação JDBC (commit/rollback), sem segurar locks.

Mais detalhes em [`docs/arquitetura.md`](docs/arquitetura.md).
