# Parte de IA do projeto

Registro de como a IA foi usada, para transparência com a professora.

## 1. Solicitação feita à IA

Requisitos do PDF (Opção C) mais os pedidos extras da professora:

1. 10 sugestões de implementação → `docs/sugestoes-implementacao.md`
2. Referências no GitHub com requisitos e códigos semelhantes → `docs/referencias-github.md`
3. Propor uma arquitetura → `docs/arquitetura.md`
4. Mostrar um diagrama → diagramas Mermaid em `docs/arquitetura.md`
5. Criar um agente para o projeto → `AGENTS.md` + `.claude/agents/revisor-concorrencia.md`
6. Pasta no Git com a estrutura solicitada à IA → este repositório

Decisões que a equipe tomou: banco **MySQL**, build **Maven + Java 17**.

## 2. O agente do projeto

- `AGENTS.md`: regras do projeto que qualquer agente de código lê antes de mexer (saldo privado e sincronizado, ordem de bloqueio, `BigDecimal`, SQL só em `persistence`...).
- `.claude/agents/revisor-concorrencia.md`: sub-agente que revisa o código atrás de race condition, deadlock e violações de arquitetura, e roda `mvn test`.

## 3. O que foi validado na prática

- Compilação com Java 17 (`-source 17`) do código principal.
- `DemoRaceCondition`: antes corrompido, depois sempre 400000.00.
- Simulação de 8 threads em 3 contas com transferências: saldo total bate e `saldo == inicial + histórico`.
- 200.000 transferências em sentidos opostos sem deadlock.
- **Ainda precisa ser feito por vocês:** rodar `mvn test` e a simulação com o MySQL de vocês, e tirar os prints do relatório.

## 4. Seus prompts (preencha)

| Data | Prompt usado | O que a IA devolveu | O que vocês alteraram |
|------|--------------|---------------------|-----------------------|
|      |              |                     |                       |
