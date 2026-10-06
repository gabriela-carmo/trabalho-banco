# Relatório — antes e depois da correção da race condition

> **Preencher com os SEUS prints/logs.** O log abaixo é de uma execução real do `DemoRaceCondition`;
> rode no seu computador, cole a sua saída e tire print do terminal.

## 1. O problema

Quando várias threads chamam `depositar()` na mesma conta sem sincronização, a operação
`saldo = saldo.add(valor)` (ler → somar → escrever) se intercala entre as threads.
Duas threads leem o mesmo saldo, ambas somam, ambas escrevem, e **um depósito se perde**.

## 2. Experimento

8 threads × 50.000 depósitos de R$ 1,00 na mesma conta. Saldo esperado: **400.000**.

## 3. ANTES (`ContaInsegura`, sem `synchronized`)

```
Rodada 1: saldo real = 330347 | perdido = 69653  | CORROMPIDO
Rodada 2: saldo real = 200000 | perdido = 200000 | CORROMPIDO
Rodada 3: saldo real = 54453  | perdido = 345547 | CORROMPIDO
Rodada 4: saldo real = 173724 | perdido = 226276 | CORROMPIDO
Rodada 5: saldo real = 50000  | perdido = 350000 | CORROMPIDO
```

O resultado **muda a cada execução** e fica sempre abaixo do esperado.

## 4. DEPOIS (`Conta`, com `synchronized`)

```
Rodada 1: saldo real = 400000.00 | perdido = 0.00 | OK
Rodada 2: saldo real = 400000.00 | perdido = 0.00 | OK
Rodada 3: saldo real = 400000.00 | perdido = 0.00 | OK
Rodada 4: saldo real = 400000.00 | perdido = 0.00 | OK
Rodada 5: saldo real = 400000.00 | perdido = 0.00 | OK
```

Mesmo código de simulação, mesmo número de threads: agora o saldo é **sempre exato**.

## 5. Transferências e deadlock

- Problema: A→B trava A e espera B; B→A trava B e espera A. Ninguém avança.
- Solução: `transferirPara` sempre trava primeiro a conta de **menor número**.
- Evidência: `ContaConcorrenciaTest.transferenciasEmSentidosOpostosNaoTravamEConservamODinheiro`
  executa 2 × 20.000 transferências em sentidos opostos com timeout; termina e o total (R$ 2.000,00) é conservado.

## 6. Conclusão

(escreva com suas palavras: o que aprendeu, o que foi difícil, o que faria diferente)

## 7. Referências

Ver `docs/referencias-github.md`.
