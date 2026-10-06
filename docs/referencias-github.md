# Referências no GitHub (e afins) com requisitos parecidos

Nenhuma cobre **tudo** que o PDF pede (principalmente JDBC + extrato em arquivo + testes),
mas juntas cobrem a parte difícil: race condition e deadlock em contas bancárias.
Use para **comparar** com a sua solução e entender, não para copiar.

| Repositório / página | O que mostra | Parecido com o nosso |
|---|---|---|
| https://github.com/sebfisch/java-locks/wiki | Contas bancárias sincronizadas de várias formas (synchronized, locks explícitos), simulação concorrente em 10 contas e exercício de refatorar com `tryLock` com tempo limite. Pede Java 17+. | Mesma versão de Java; ótima fonte para a variação com `ReentrantLock`. |
| https://github.com/ya-dola/bankaccountexample | Exemplo multithread de conta: explica o cenário de saque simultâneo que deixa saldo negativo e o deadlock em duas contas, resolvido ordenando os locks pelo id da conta. | Mesma estratégia anti-deadlock do `transferirPara`. |
| https://github.com/aarczynski/threadssyncdemo | Demo com Spring Boot e Gatling com cenários: transferência sem lock, com lock global (lento), com deadlock e com blocos aninhados em ordem fixa. | Mostra "antes e depois" com carga, igual ao nosso relatório. |
| https://github.com/rushuat/moneytransfer | Transferência concorrente que trava as contas por id em ordem crescente. Projeto pequeno e fácil de ler. | Mesma regra de ordem de bloqueio, em escala pequena. |
| https://wiki.sei.cmu.edu/confluence/x/dmJGBQ | Regra do CERT (SEI/CMU) sobre deadlock em transferência entre contas e as soluções conformes (lock global e ordenação). | Referência "oficial" para citar no relatório. |
| https://codesignal.com/learn/courses/java-concurrency-in-practice/lessons/implementing-a-thread-safe-bank-account-transfer-system | Lição passo a passo: bloqueia a conta de menor id primeiro, depois a de maior. | Tutorial equivalente ao `transferirPara`. |

**Lacuna que o nosso projeto preenche:** nenhuma dessas referências persiste contas/transações via JDBC nem exporta extrato.
Isso é parte do diferencial do trabalho.

> Dica para o relatório: cite 2 ou 3 destas referências e diga o que você fez igual e o que fez diferente.
