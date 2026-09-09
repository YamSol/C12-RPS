# PoC — Java 21 + JavaFX

Fatia vertical do RPS concorrente na stack A. Ver issue #2 para o escopo e os
critérios de comparação, e issue #1 para a arquitetura e os requisitos.

## Como rodar

```bash
cd poc-java
gradle run          # ou ./gradlew run, se você gerar o wrapper
```

Precisa de JDK 21+ e Gradle 8+. O plugin `org.openjfx.javafxplugin` baixa o
JavaFX 21.0.4 sozinho — não é preciso instalar o SDK à mão.

### Sem GUI (smoke test)

Prova que as camadas de domínio e concorrência terminam sozinhas, sem JavaFX
nem Gradle — só um JDK:

```bash
javac --release 21 -d out \
  $(find src/main/java/rps/domain src/main/java/rps/concurrency -name '*.java') \
  src/main/java/rps/HeadlessMain.java
java -cp out rps.HeadlessMain 60 4 0     # N T delayMs
```

Sai com código 1 se o torneio não terminar em 60 s (detector de deadlock).

## Mapa do código → camadas do planejamento

| Camada | Pacote | Classes |
|---|---|---|
| Domínio | `rps.domain` | `Move`, `Player`, `Round`, `MatchResult`, `Match` |
| Concorrência | `rps.concurrency` | `PlayerQueue`, `EventBus`, `Events`, `Orchestrator`, `MatchRunner`, `Tournament`, `TournamentConfig` |
| Apresentação | `rps.ui` | `MainApp`, `QueuePanel`, `ArenaPanel`, `MatchView`, `ColorMapper`, `Launcher` |

O domínio não importa nada de `javafx.*` nem de `java.util.concurrent.*`.
A concorrência não importa nada de `javafx.*` — o único acoplamento com a GUI é
o `Executor` passado ao `EventBus`, que em `MainApp` é `Platform::runLater`.
Trocar JavaFX por Swing = trocar esse `Executor` por `SwingUtilities::invokeLater`
e reescrever só o pacote `rps.ui`.

## Decisões de implementação (e por que)

- **`ReentrantLock` + `Condition` em vez de `BlockingQueue`.** A primitiva que o
  RF03 pede é "tire DOIS", que `BlockingQueue` não oferece atomicamente; e a
  espera precisa acordar quando o torneio acaba, não só quando chega elemento.
  Ficou em `PlayerQueue.dequeuePair()`, que devolve `null` como sinal de fim.
- **Contador `alive` dentro da fila.** O fim do torneio (RF09) é `alive == 1` —
  contando quem está dentro de um match, não só quem está na fila. É esse
  contador, e não `queue.isEmpty()`, que evita o orquestrador dormir para sempre.
- **`MatchEnded` significa "saiu da arena", não "foi decidido".** O `MatchRunner`
  publica o último `RoundPlayed` (já decisivo), segura o match por
  `roundDelayMs` para a GUI mostrar o vencedor, e só então pontua, elimina o
  perdedor e publica `MatchEnded`. Sem essa pausa o match some antes de ser
  visto (pergunta aberta #2 do planejamento).
- **`Launcher` separado de `MainApp`.** Rodar JavaFX não-modular pelo classpath
  com a `Application` como main-class falha com *"JavaFX runtime components are
  missing"*. Atrito de toolchain que conta para a comparação.

## Escopo entregue

Feito: RF01–RF07, RF09, RF10, RF11, RNF01–RNF03.
Fora (por decisão do escopo da PoC): RF08 (`pattern` de desempate visual),
UC07 (jogador humano), animação por rodada, persistência, testes automatizados.

## Estado da verificação

- Compila limpo com `javac --release 21 -Xlint:all` contra JavaFX 21.0.4 —
  as três camadas.
- `HeadlessMain` roda até o fim (1 campeão, `activeMatches == 0`) em
  N/T = 2/1, 3/1, 7/16, 60/4, 61/4, 200/8. Sem deadlock, inclusive com N ímpar
  e com T maior que o número de partidas possíveis.
- **A janela JavaFX não foi executada** — não havia Gradle instalado na máquina
  onde a PoC foi escrita. Rodar `gradle run` e conferir a arena é o primeiro
  passo de quem pegar esta branch.
