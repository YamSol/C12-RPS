# Arquitetura

Projeto: simulação de um torneio de pedra-papel-tesoura com concorrência real —
cada partida roda em uma thread, uma fila única alimenta o torneio, e a GUI
observa tudo sem nunca tocar no estado compartilhado.

Referências: [requisitos.md](requisitos.md), [casos-de-uso.md](casos-de-uso.md),
[decisoes.md](decisoes.md).

## 1. Camadas

Três camadas, com dependência estritamente para baixo. Apresentação nunca é
chamada pelas camadas de baixo — recebe eventos.

```
+-----------------------------------------+
|  Apresentacao  (UI thread)              |
|  MainApp . QueuePanel . ArenaPanel      |
+---------------^-------------------------+
                | eventos (marshalados p/ UI thread)
+---------------+-------------------------+
|  Concorrencia  (Orchestrator + pool T)  |
|  Tournament . PlayerQueue . MatchRunner |
+---------------^-------------------------+
                | usa
+---------------+-------------------------+
|  Dominio  (puro, sem thread/GUI)        |
|  Player . Move . Round . MatchResult    |
+-----------------------------------------+
```

| Camada | Pacote | O que pode importar |
|---|---|---|
| Domínio | `rps.domain` | nada de `javafx.*`, nada de `java.util.concurrent.*` |
| Concorrência | `rps.concurrency` | domínio + `java.util.concurrent.*`; nada de `javafx.*` |
| Apresentação | `rps.ui` | tudo |

O único ponto onde a concorrência sabe que existe uma GUI é o `Executor`
passado ao `EventBus` no construtor. Em JavaFX ele é `Platform::runLater`.
Trocar por Swing significa passar `SwingUtilities::invokeLater` e reescrever
apenas `rps.ui` — nenhuma outra classe muda (RNF03 estendido à camada de
apresentação).

## 2. Domínio

```
enum Move { ROCK, PAPER, SCISSORS }
    beats(other) -> 1 vence | -1 perde | 0 empata
    random() -> jogada aleatoria uniforme        // ADR-007

class Player
    id: int
    score: int                                    // comeca em 0, +1 por vitoria
    win() -> int                                  // incrementa e devolve o score novo

record Round(p1: Move, p2: Move)
    isDraw() -> bool

record MatchResult(winner: Player, loser: Player, rounds: list<Round>)

class Match
    id: int
    p1, p2: Player
    state: PENDING | RUNNING | DONE
    lastRound: Round?                             // ultimo round jogado, pra GUI
    result: MatchResult?                          // preenchido ao final
```

`Match` é mutável e é lido pela UI thread enquanto uma thread de match escreve
nele. Os campos mutados (`state`, `lastRound`, `result`) são escritos uma vez
por transição e sempre publicados logo em seguida por um evento — a UI só lê
depois de receber o evento correspondente.

## 3. Concorrência

```
class PlayerQueue                       // thread-safe (RNF02)
    waiting: Deque<Player>
    alive: int                          // NAO eliminados, incluindo quem esta em match
    lock: ReentrantLock + Condition changed

    enqueue(p)                          // RF04: winner volta pro fim
    eliminate(p)                        // RF05: alive--
    dequeuePair() -> (Player, Player)?  // RF03: bloqueia ate 2 disponiveis;
                                        //       null = torneio acabou
    size() / alive() / snapshot() / champion()

class Orchestrator: Runnable            // thread propria, em loop
    loop:
        pair = queue.dequeuePair()
        if pair == null: break          // RF09
        match = new Match(id++, pair)
        activeMatches.add(match)
        pool.submit(new MatchRunner(match, ...))

class MatchRunner: Runnable             // uma das T threads do pool
    publish MatchStarted
    do { sleep(delay); round = random vs random; publish RoundPlayed }
    while round.isDraw()                // RF06 / ADR-004
    sleep(delay)                        // ADR-005: deixa o resultado visivel
    winner.win(); queue.eliminate(loser); queue.enqueue(winner)
    activeMatches.remove(match)
    publish MatchEnded, PlayerScored, PlayerEliminated, QueueChanged

class Tournament
    config: TournamentConfig(players, threads, roundDelayMs)
    queue, activeMatches (set thread-safe), pool = ExecutorService(T), bus
    start() / stop() / isFinished() / champion()
```

### 3.1 A armadilha de deadlock do desenho

**O que termina o torneio não é a fila estar vazia — é `alive == 1`.**

`alive` conta quem ainda não foi eliminado, *incluindo quem está dentro de uma
partida*. Se o critério de parada olhasse só `waiting.isEmpty()`, o orquestrador
dormiria para sempre no caso normal em que todos os jogadores restantes estão em
matches ativos e a fila está momentaneamente vazia.

`dequeuePair()` acorda em duas condições distintas: chegou gente na fila
(`enqueue`) **ou** alguém foi eliminado (`eliminate`) — por isso `eliminate`
também faz `signalAll()`, mesmo sem mexer em `waiting`. Sem esse sinal, o último
par nunca se forma.

Esta é a única fonte real de deadlock da arquitetura. Qualquer reimplementação
precisa reproduzir os dois pontos: o contador `alive` e o sinal na eliminação.

### 3.2 Por que não `BlockingQueue`

A primitiva que o RF03 pede é "tire DOIS atomicamente", que nenhuma fila
concorrente pronta oferece — nem `BlockingQueue` (Java) nem `ConcurrentQueue` /
`Channel<T>` (.NET). Duas chamadas de `take()` seguidas não são atômicas: duas
threads de orquestração pegariam jogadores cruzados. Além disso a espera precisa
ser interrompível pelo fim do torneio, não só pela chegada de elemento.

Daí `ReentrantLock` + `Condition`. Ver [ADR-009](decisoes.md#adr-009).

## 4. Contrato do EventBus

O `EventBus` é o observer entre concorrência e apresentação. Publicação vem de
qualquer thread; **o dispatch é sempre marshalado para a UI thread** pelo
`Executor` recebido no construtor. É isso que sustenta o RNF01: o handler da GUI
roda na UI thread, com um evento já pronto na mão, sem tocar em lock nenhum.

```
class EventBus
    EventBus(uiExecutor: Executor)
    subscribe<E>(type: Class<E>, handler: Consumer<E>)
    publish(event)   // handlers rodam no uiExecutor, nunca na thread que publicou
```

Regras do contrato:

- **Assinatura por tipo exato.** `publish` despacha para os handlers registrados
  na classe concreta do evento; não há hierarquia nem wildcard.
- **Sem valor de retorno e sem confirmação.** Publicar é fire-and-forget; a
  thread que publica não espera a GUI.
- **Ordem preservada por thread publicadora**, não globalmente. Dois matches
  concorrentes podem ter seus eventos intercalados em qualquer ordem.
- **Eventos são imutáveis** (records) e carregam cópias defensivas quando
  carregam coleção (`QueueChanged` recebe um `snapshot()`).
- **Handler não bloqueia.** Ele roda na UI thread; qualquer espera ali viola
  RNF01.

### Eventos

| Evento | Payload | Publicado por | Quando |
|---|---|---|---|
| `MatchStarted` | `match` | `MatchRunner` | match entra na arena |
| `RoundPlayed` | `match`, `round` | `MatchRunner` | cada rodada, inclusive as empatadas |
| `MatchEnded` | `match`, `result` | `MatchRunner` | match **sai da arena** (não quando é decidido — ver ADR-005) |
| `PlayerScored` | `player`, `score` | `MatchRunner` | após `winner.win()` |
| `PlayerEliminated` | `player` | `MatchRunner` | após `queue.eliminate(loser)` |
| `QueueChanged` | `waiting` (snapshot), `alive` | `Orchestrator`, `MatchRunner` | par retirado da fila; fim de match |
| `TournamentEnded` | `champion` | `Orchestrator` | loop encerrou (RF09) |

## 5. Apresentação

```
Launcher      // main-class separada; ver ADR-010
MainApp       // monta a janela, assina o bus, cria o Tournament
QueuePanel    // fila viva; redesenha em QueueChanged
ArenaPanel    // grade de MatchView; redivide o espaco (RF11)
MatchView     // um match: dois retangulos coloridos + jogadas do ultimo round
ColorMapper   // score -> cor, gradiente de matiz frio->quente
```

`ArenaPanel` implementa o RF11 escolhendo a grade quadrada mais próxima:
`cols = ceil(sqrt(n))`, `rows = ceil(n / cols)`, com `percentWidth` e
`percentHeight` iguais em todas as colunas e linhas. 1 match ocupa a tela
inteira, 2 ficam 50/50, 4 viram 2×2. O relayout roda a cada `MatchStarted` e
`MatchEnded` (UC05).

`ColorMapper` mapeia `score / maxScore` em matiz, do frio (0) ao quente
(`maxScore`) — o mapa de calor previsto no RF07. `maxScore` é o maior score vivo
no momento, então a escala se reajusta conforme o torneio avança e todos os
matches são repintados junto.
