# 10 sugestões de implementação

Cada sugestão diz **o quê**, **por quê** e **onde** ela aparece (ou pode aparecer) neste projeto.

| # | Sugestão | Por quê | Onde |
|---|----------|---------|------|
| 1 | **`synchronized` nos métodos de `Conta` que alteram saldo** | O requisito base. Torna "ler → calcular → escrever" indivisível por conta. | `Conta.depositar/sacar` |
| 2 | **Ordem fixa de bloqueio na transferência (menor número primeiro)** | Evita deadlock em A→B e B→A simultâneos: todos disputam o mesmo cadeado primeiro. | `Conta.transferirPara` |
| 3 | **`BigDecimal` com 2 casas, nunca `double`** | `double` acumula erro de arredondamento e o "saldo bate exatamente" deixaria de ser verdade. | todo o domínio |
| 4 | **`Transacao` como `record` imutável** | Registro de algo que já aconteceu não pode mudar; imutável é thread-safe de graça. | `domain/Transacao` |
| 5 | **Largada sincronizada com `CountDownLatch`** | Sem isso as threads começam escalonadas e a race condition aparece menos. Com o latch, todas colidem. | `SimuladorConcorrente` |
| 6 | **Conferir invariantes, não só um total** | Além de "soma total bate", checar `saldo == saldoInicial + Σ histórico` por conta pega perdas que se compensam. | `Conta.confereComHistorico` |
| 7 | **Persistir fora do lock, em lote** | Banco/I/O dentro de `synchronized` derruba o desempenho. Simula primeiro, grava depois com `executeBatch`. | `Main` + `TransacaoRepository` |
| 8 | **Transação JDBC (`setAutoCommit(false)` + commit/rollback)** | Conta e transações são gravadas juntas ou nenhuma: o banco nunca fica inconsistente. | `Main` |
| 9 | **Testes repetidos com timeout** | Race condition é probabilística: `@RepeatedTest` aumenta a chance de pegar; `assertTimeoutPreemptively` transforma deadlock em falha de teste em vez de travar. | `ContaConcorrenciaTest` |
| 10 | **`ContaInsegura` como "antes" didático + demo que imprime o log** | Prova a race condition com números reais pro relatório, usando o MESMO código de simulação nas duas versões. | `simulacao/ContaInsegura`, `app/DemoRaceCondition` |

## Variações para ir além (se sobrar tempo)

- **`ReentrantLock` com `tryLock(timeout)`** no lugar de `synchronized`: se não conseguir os dois locks, desiste e tenta de novo (outra estratégia anti-deadlock).
- **`ExecutorService` + `Future`** no lugar de `Thread` crua, para controlar o pool de threads.
- **Tabela `clientes` separada** (normalização) com `ClienteRepository`.
- **Testes de repositório** com banco em memória (H2) ou Testcontainers.
- **`Semaphore`/`ReadWriteLock`** para consultas de saldo mais concorrentes.
