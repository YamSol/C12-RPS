# PoC — C# / .NET 8 + Avalonia

Fatia vertical do RPS concorrente na stack B. Ver issue #2 para o escopo e os
critérios de comparação, e issue #1 para a arquitetura e os requisitos.

É a mesma PoC da branch `poc/java-javafx`, classe por classe, para a comparação
ser de stack e não de desenho.

## Como rodar

```bash
cd poc-csharp
dotnet run --project RpsPoc
```

Precisa do .NET SDK 8. O `dotnet restore` baixa o Avalonia 11.2.3.

### Sem GUI (smoke test)

```bash
dotnet run --project RpsPoc -- --headless 60 4 0     # N T delayMs
```

Sai com código 1 se o torneio não terminar em 60 s (detector de deadlock).
É por isso que o `OutputType` é `Exe` e não `WinExe`: sem console não há saída.

## Mapa do código → camadas do planejamento

| Camada | Pasta | Classes |
|---|---|---|
| Domínio | `Domain/` | `Move`/`Moves`, `Player`, `Round`, `MatchResult`, `Match` |
| Concorrência | `Concurrency/` | `PlayerQueue`, `EventBus`, `Events`, `Orchestrator`, `MatchRunner`, `Tournament`, `TournamentConfig` |
| Apresentação | `Ui/` + `App.cs` | `MainWindow`, `QueuePanel`, `ArenaPanel`, `MatchView`, `ColorMapper` |

O domínio não referencia `Avalonia.*`. A concorrência também não — o único
acoplamento com a GUI é o `Action<Action>` passado ao `EventBus`, que em
`MainWindow` é `Dispatcher.UIThread.Post`.

## Decisões de implementação (e por que)

- **`lock` + `Monitor.Wait/PulseAll`, não `ConcurrentQueue` nem `Channel<T>`.**
  O RF03 pede "tire DOIS" atomicamente, que nenhuma das duas oferece; e a espera
  precisa acordar quando o torneio acaba, não só quando chega elemento.
  Ponto de atrito real da stack: a API idiomática do .NET é assíncrona, mas a
  operação que o problema pede é um rendez-vous bloqueante, então o
  `Orchestrator` chama `Task.Run(queue.DequeuePair)` só para não prender a
  própria thread.
- **`SemaphoreSlim(T)` no lugar de um pool de tamanho fixo.** O thread pool do
  .NET é global e não se dimensiona por aplicação, então honrar o RF02 significa
  limitar quantas *tasks* concorrem, não quantas threads existem. É a diferença
  conceitual mais visível em relação ao `ExecutorService` do Java.
- **Contador `alive` dentro da fila.** O fim do torneio (RF09) é `alive == 1` —
  contando quem está dentro de um match. É ele, e não `queue.IsEmpty`, que evita
  o orquestrador dormir para sempre.
- **`MatchEnded` significa "saiu da arena", não "foi decidido".** O runner segura
  o match por `RoundDelayMs` depois da rodada decisiva para a GUI mostrar o
  vencedor (pergunta aberta #2 do planejamento).
- **UI montada em C#, sem `.axaml`.** Não é o caminho canônico do Avalonia (o
  template oficial usa XAML + MVVM), mas mantém a PoC comparável linha a linha
  com a versão JavaFX. Um projeto de verdade nesta stack provavelmente usaria
  XAML e binding — o que muda a conta da comparação e precisa ser dito na
  decisão final.

## Escopo entregue

Feito: RF01–RF07, RF09, RF10, RF11, RNF01–RNF03.
Fora (por decisão do escopo da PoC): RF08 (`pattern` de desempate visual),
UC07 (jogador humano), animação por rodada, persistência, testes automatizados.

## Estado da verificação

**Nada aqui foi compilado nem executado** — não havia .NET SDK na máquina onde a
PoC foi escrita (`dotnet --version` → *No .NET SDKs were found*). O código é a
tradução direta da versão Java, que essa sim compila e roda até o fim.

Primeiro passo de quem pegar esta branch, nesta ordem:

1. `dotnet build` — esperar erros de compilação e corrigi-los; foram escritos às
   cegas contra a API do Avalonia 11.2.
2. `dotnet run --project RpsPoc -- --headless 60 4 0` — tem que terminar com
   `alive=1 activeMatches=0`. Repetir com `2 1`, `3 1`, `7 16`, `61 4`, `200 8`
   (é a bateria que a versão Java passou, incluindo N ímpar e T > partidas).
3. `dotnet run --project RpsPoc` — conferir a arena redividindo.

Só depois disso a tabela de comparação da issue #2 pode ser preenchida com
honestidade.
