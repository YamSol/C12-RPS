# Requisitos

Versão 1.0 — consolidada a partir da issue #1 e revisada contra a PoC Java
(issue #2). Onde a PoC mudou o entendimento de um requisito, há uma nota.

## Requisitos funcionais

| ID | Requisito | Status na PoC |
|----|-----------|---------------|
| RF01 | O torneio começa com N jogadores, configurável (default 60). | feito |
| RF02 | O número de partidas simultâneas T é configurável (default 4). | feito |
| RF03 | O orquestrador só desenfileira quando há ≥ 2 jogadores disponíveis na fila. | feito |
| RF04 | O vencedor recebe +1 ponto e volta ao fim da fila. | feito |
| RF05 | O perdedor é eliminado permanentemente. | feito |
| RF06 | Empate em uma rodada → nova rodada da mesma dupla, até haver vencedor. | feito |
| RF07 | A cor de cada jogador é função determinística do seu score atual. | feito |
| RF08 | Dois jogadores de mesma cor em matches ativos simultâneos são diferenciados por `pattern` (listrado, pontilhado…). | fora do escopo da PoC |
| RF09 | O torneio termina quando resta 1 jogador e não há matches ativos. | feito |
| RF10 | A GUI reflete em tempo quase-real: entrada/saída da fila, matches iniciando e terminando. | feito (não executado — ver [decisoes.md](decisoes.md#adr-003)) |
| RF11 | O layout da arena redivide o espaço automaticamente conforme o nº de matches ativos varia (1 → tela cheia; 2 → 50/50; N → grade de células iguais). | feito (não executado) |
| RF12 | Ao final do torneio o sistema apresenta automaticamente o campeão e as métricas da execução: total de partidas, tempo total, tempo médio por partida e vazão (partidas/s). | feito |

### Notas

**RF12 — a média por partida e o tempo total medem coisas diferentes.** Com
`T > 1` as partidas correm em paralelo, então a soma das durações individuais
passa do tempo de parede do torneio. A média serve para caracterizar uma
partida (não deve mudar com `T`); a **vazão** é a métrica que reage a `T` e a
que compara stacks. Medido em `N=32`, `roundDelayMs=50`: a média fica em ~86 ms
de `T=1` a `T=16`, enquanto a vazão vai de 6,6 a 31,8 partidas/s (saturando em
`T=16`, quando a fila deixa de alimentar 16 partidas simultâneas).

**RF12 — o resultado é um painel, não uma janela.** O `ResultPanel` entra no
lugar da `ArenaPanel`, no centro da própria janela, quando o torneio acaba, e
sai quando o próximo começa. A arena está vazia nesse momento, então a troca
não esconde nada; e sem diálogo para fechar, variar `N` e `T` em sequência —
o uso principal da tela — não passa por um clique extra a cada rodada.

As amostras são tiradas na origem — `MatchRunner` para a duração da partida,
`Orchestrator` para o fechamento — nunca no assinante do `EventBus`, cujo
`Platform::runLater` entraria na medida (RNF01). A pausa cosmética que mantém o
resultado visível na arena fica **fora** da duração da partida.

Fecha as issues [#5](https://github.com/YamSol/C12-RPS/issues/5) e
[#10](https://github.com/YamSol/C12-RPS/issues/10). Ficou fora, por não pagar o
custo na PoC: tempo de espera na fila com mediana e p95, que exigiria um mapa
`Player -> instante` mantido sob o lock da `PlayerQueue`.

**RF04 + RF05 são uma transação só.** `PlayerQueue.finishMatch()` elimina o
perdedor e devolve o vencedor à fila sem soltar o lock no meio. Em dois passos
separados (`eliminate()` + `enqueue()`) havia uma janela com `alive == 1` e o
vencedor ainda fora da fila: o orquestrador que acordasse ali encerrava o
torneio e `champion()` devolvia `null`. Reproduzido em ~1 de 2000 execuções com
`N=8, T=4`; com a transação, 0 em 7000.

**RF02 — "T threads" é específico de stack.** Em Java, T é literalmente o
tamanho do `ExecutorService`, e a formulação original ("número de threads de
match") vale ao pé da letra. Em .NET o thread pool é global e o mesmo requisito
teria de ser reescrito como "T tasks concorrentes", limitadas por um
`SemaphoreSlim`. O texto acima foi generalizado para **partidas simultâneas**,
que é o que o requisito realmente quer dizer nas duas stacks.

Na primeira versão o limite era *só* o tamanho do pool — e por isso o requisito
não era cumprido de fato: `pool.submit()` não bloqueia, então o orquestrador
pareava a fila inteira de uma vez e as partidas excedentes esperavam escondidas
dentro do executor. Quem limita hoje é um `Semaphore(T)` segurando o
pareamento; o pool ficou só como provedor de threads — o que aproxima a stack
Java da formulação que o .NET exigiria. Ver [ADR-011](decisoes.md#adr-011).

**RF07 — determinística *e* estável.** "Função determinística do score" era
verdade na primeira versão só dentro de um instante: a escala se renormalizava a
cada `PlayerScored`, então o mesmo score mudava de cor ao longo do torneio.
Hoje a escala é discreta e fixada no start, com `ceil(log2(N)) + 2` níveis — a
cor de um score não muda enquanto o torneio roda. Ver
[ADR-012](decisoes.md#adr-012); a legenda que torna a escala legível é a
[#15](https://github.com/YamSol/C12-RPS/issues/15).

**RF06 — empate é por rodada, não por partida.** Uma partida é uma sequência de
rodadas que só termina quando uma delas é decidida; o histórico completo fica em
`MatchResult.rounds`. Ver [ADR-004](decisoes.md#adr-004).

**RF09 — o critério é `alive == 1`, não "fila vazia".** `alive` conta os
jogadores não eliminados incluindo os que estão dentro de uma partida. Usar
`queue.isEmpty()` como critério trava o orquestrador. Ver
[arquitetura.md §3.1](arquitetura.md).

## Requisitos não-funcionais

| ID | Requisito | Status na PoC |
|----|-----------|---------------|
| RNF01 | A UI thread nunca executa lógica de jogo nem espera por lock de longa duração. | feito |
| RNF02 | A fila e a coleção de matches ativos são thread-safe. | feito |
| RNF03 | Alterar T não exige mudança em nenhuma classe além de `Tournament` / `TournamentConfig`. | feito |
| RNF04 | Multiplataforma (Windows e Linux). | feito por construção; ver ADR-003 |

### Notas

**RNF01** é sustentado pelo `EventBus`: a GUI só recebe eventos imutáveis já
marshalados para a UI thread, e as leituras de estado compartilhado saem por
cópia defensiva (`PlayerQueue.snapshot()`).

**RNF04** eliminou WPF (Windows-only) da lista de candidatas ainda no
planejamento, e é atendido tanto por JavaFX quanto por Avalonia.

## Parâmetros de configuração

| Parâmetro | Default | Onde |
|---|---|---|
| `players` (N) | 60 | `TournamentConfig` |
| `threads` (T) | 4 | `TournamentConfig` |
| `roundDelayMs` | 350 | `TournamentConfig` |

`roundDelayMs` é a duração artificial de cada rodada e também da pausa que
mantém o resultado visível antes do match sair da arena. Com `0` o torneio roda
o mais rápido possível — é o modo usado no smoke test headless. Ver
[ADR-005](decisoes.md#adr-005).

## Fora de escopo (v1)

- Persistência de resultados.
- Jogador humano (UC07) — previsto na arquitetura, não implementado.
- Animação rodada a rodada além da troca de jogadas exibida.
- `pattern` de desempate visual (RF08).
