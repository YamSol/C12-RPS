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

### Notas

**RF02 — "T threads" é específico de stack.** Em Java, T é literalmente o
tamanho do `ExecutorService`, e a formulação original ("número de threads de
match") vale ao pé da letra. Em .NET o thread pool é global e o mesmo requisito
teria de ser reescrito como "T tasks concorrentes", limitadas por um
`SemaphoreSlim`. O texto acima foi generalizado para **partidas simultâneas**,
que é o que o requisito realmente quer dizer nas duas stacks. Como a stack
escolhida foi Java ([ADR-003](decisoes.md#adr-003)), a implementação é o tamanho
do pool.

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
