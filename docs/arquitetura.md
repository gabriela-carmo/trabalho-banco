# Arquitetura proposta

Arquitetura em **camadas** com regra de dependência para dentro: o domínio não conhece ninguém.

```mermaid
flowchart TB
    subgraph app["app"]
        Main
        DemoRaceCondition
        ExtratoExportador
    end
    subgraph simulacao["simulacao"]
        SimuladorConcorrente
        OperacaoBancariaThread
        ContaInsegura["ContaInsegura (didática)"]
    end
    subgraph persistence["persistence (JDBC)"]
        ContaRepository
        TransacaoRepository
        ConexaoFactory
    end
    subgraph domain["domain"]
        Conta
        Cliente
        Transacao
        TipoTransacao
    end
    DB[("MySQL")]
    TXT[/"extrato.txt"/]

    Main --> SimuladorConcorrente
    Main --> ContaRepository
    Main --> TransacaoRepository
    Main --> ExtratoExportador
    SimuladorConcorrente --> OperacaoBancariaThread
    OperacaoBancariaThread --> Conta
    ContaRepository --> Conta
    TransacaoRepository --> Transacao
    ContaRepository --> TransacaoRepository
    ContaRepository --> DB
    TransacaoRepository --> DB
    ExtratoExportador --> TXT
    Conta --> Transacao
    Conta --> Cliente
```

## Diagrama de classes do domínio

```mermaid
classDiagram
    class Conta {
        -long numero
        -Cliente titular
        -BigDecimal saldoInicial
        -BigDecimal saldo
        -List~Transacao~ historico
        +depositar(valor) synchronized
        +sacar(valor) synchronized
        +transferirPara(destino, valor)
        +getSaldo() BigDecimal
        +getHistorico() List
        +confereComHistorico() boolean
    }
    class Cliente {
        <<record>>
        nome
        cpf
    }
    class Transacao {
        <<record>>
        contaNumero
        tipo
        valor
        dataHora
        contaRelacionada
    }
    class TipoTransacao {
        <<enumeration>>
        DEPOSITO
        SAQUE
        TRANSFERENCIA_ENVIADA
        TRANSFERENCIA_RECEBIDA
    }
    Conta "1" --> "1" Cliente : titular
    Conta "1" o-- "*" Transacao : historico
    Transacao --> TipoTransacao
```

## Decisões de projeto

| Decisão | Motivo |
|---|---|
| Um objeto `Conta` por número de conta, compartilhado entre threads | O cadeado do `synchronized` é o próprio objeto; dois objetos para a mesma conta não se protegeriam. |
| Ordem de bloqueio pelo número da conta | Critério fixo e igual para todas as threads, elimina ciclo de espera (deadlock). |
| Persistência depois da simulação, em lote | Mantém I/O e banco fora dos locks. |
| `Conta.retirarNaoPersistidas()` | Rodar a gravação de novo não duplica transações. |
| `Conta.restaurar()` | Permite recuperar a conta e o extrato entre execuções sem o domínio conhecer JDBC. |
| Repositories sem commit | Quem orquestra (`Main`) decide o limite da transação JDBC. |

## Fluxo de uma transferência

1. Thread chama `origem.transferirPara(destino, valor)`.
2. Ordena as duas contas pelo número → `primeira`, `segunda`.
3. `synchronized (primeira) { synchronized (segunda) { ... } }`
4. Dentro: verifica saldo → debita → credita → registra 2 transações (tudo atômico).
5. Sai dos blocos; os locks são liberados automaticamente.
