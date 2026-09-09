# C12 — RPS concorrente

Simulação de um torneio de pedra-papel-tesoura com concorrência real: uma fila
única de jogadores, um orquestrador em thread própria e T partidas rodando
simultaneamente em um pool de threads. Cada vitória vale +1 ponto e devolve o
jogador ao fim da fila; cada derrota elimina. O torneio acaba quando resta um.

A cor de cada jogador é função do seu score, em um gradiente do frio ao quente,
e a arena redivide o espaço sozinha conforme o número de partidas ativas muda.

**Java 21 + JavaFX.**

## Como rodar

Precisa de JDK 21+ e Gradle 8+. O plugin `org.openjfx.javafxplugin` baixa o
JavaFX 21.0.4 sozinho — não é preciso instalar o SDK à mão.

```bash
gradle run
```

### Sem GUI (smoke test)

Roda o torneio inteiro só com domínio e concorrência — nem JavaFX nem Gradle,
só um JDK. Sai com código 1 se não terminar em 60 s (detector de deadlock):

```bash
javac --release 21 -d out \
  $(find src/main/java/rps/domain src/main/java/rps/concurrency -name '*.java') \
  src/main/java/rps/HeadlessMain.java
java -cp out rps.HeadlessMain 60 4 0     # N T delayMs
```

## Configuração

Em `TournamentConfig` (`src/main/java/rps/concurrency/TournamentConfig.java`):

| Parâmetro | Default | O que é |
|---|---|---|
| `players` (N) | 60 | jogadores no início do torneio |
| `threads` (T) | 4 | partidas simultâneas — tamanho do pool |
| `roundDelayMs` | 350 | duração artificial de cada rodada; `0` roda a toda velocidade |

## Estrutura

Três camadas, com dependência estritamente para baixo. A GUI nunca toca no
estado compartilhado — recebe eventos já marshalados para a UI thread.

| Camada | Pacote | Classes |
|---|---|---|
| Domínio | `rps.domain` | `Move`, `Player`, `Round`, `MatchResult`, `Match` |
| Concorrência | `rps.concurrency` | `PlayerQueue`, `EventBus`, `Events`, `Orchestrator`, `MatchRunner`, `Tournament`, `TournamentConfig` |
| Apresentação | `rps.ui` | `MainApp`, `QueuePanel`, `ArenaPanel`, `MatchView`, `ColorMapper`, `Launcher` |

O domínio não importa `javafx.*` nem `java.util.concurrent.*`. A concorrência
não importa `javafx.*`: o único acoplamento com a GUI é o `Executor` passado ao
`EventBus`, que em `MainApp` é `Platform::runLater`. Trocar JavaFX por Swing =
trocar esse `Executor` por `SwingUtilities::invokeLater` e reescrever só
`rps.ui`.

## Documentação

| Documento | Conteúdo |
|---|---|
| [docs/arquitetura.md](docs/arquitetura.md) | Camadas, contrato do EventBus, a armadilha de deadlock do desenho. |
| [docs/requisitos.md](docs/requisitos.md) | RF01–RF11 e RNF01–RNF04. |
| [docs/casos-de-uso.md](docs/casos-de-uso.md) | UC01–UC07. |
| [docs/decisoes.md](docs/decisoes.md) | ADRs — escolha da stack, empate, duração da partida, fila ímpar, estratégia. |

## Estado

Implementado: RF01–RF07, RF09, RF10, RF11, RNF01–RNF03.
Fora: RF08 (`pattern` de desempate visual), UC07 (jogador humano), animação por
rodada, persistência, testes automatizados.

Compila limpo com `javac --release 21 -Xlint:all` contra JavaFX 21.0.4. O
headless termina com 1 campeão e nenhum match ativo em N/T = 2/1, 3/1, 7/16,
60/4, 61/4 e 200/8 — sem deadlock, inclusive com N ímpar e T maior que o número
de partidas possíveis.

**A janela JavaFX ainda não foi executada** — RF10 e RF11 estão implementados
mas não verificados em tela.
