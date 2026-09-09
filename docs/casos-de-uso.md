# Casos de uso

Ator único: **usuário** (quem roda a simulação). Os demais participantes são
componentes do sistema — orquestrador, threads de match, GUI.

Referências: [requisitos.md](requisitos.md), [arquitetura.md](arquitetura.md).

---

## UC01 — Iniciar torneio

**Ator:** usuário
**Pré-condição:** aplicação aberta, nenhum torneio em andamento.

1. Usuário define N (jogadores) e T (partidas simultâneas), ou aceita os
   defaults (60 / 4).
2. Usuário aciona *start*.
3. Sistema cria N jogadores com score 0 e popula a fila.
4. Sistema cria o pool de tamanho T e o `EventBus`.
5. Sistema inicia a thread do orquestrador.
6. GUI recebe `QueueChanged` e desenha a fila inicial.

**Pós-condição:** orquestrador em loop, fila cheia, arena vazia.
**Requisitos:** RF01, RF02.

---

## UC02 — Executar partida

**Ator:** sistema (orquestrador + thread de match)
**Pré-condição:** ≥ 2 jogadores disponíveis na fila.

1. Orquestrador desenfileira uma dupla (`dequeuePair`).
2. Orquestrador cria o `Match`, registra em `activeMatches` e publica
   `QueueChanged`.
3. Orquestrador submete o `MatchRunner` ao pool e volta ao topo do loop.
4. Thread do pool publica `MatchStarted`; GUI acrescenta o match à arena (UC05).
5. Thread joga uma rodada, publica `RoundPlayed`; GUI mostra as jogadas.
6. **Alternativa 5a — empate:** rodada empatada, repete o passo 5 com a mesma
   dupla (RF06).
7. Rodada decidida: thread define `MatchResult`, marca o match como `DONE` e
   mantém o resultado visível por `roundDelayMs`.
8. Thread pontua o vencedor, elimina o perdedor, devolve o vencedor à fila,
   remove o match de `activeMatches` e publica `MatchEnded`, `PlayerScored`,
   `PlayerEliminated` e `QueueChanged`.

**Pós-condição:** um jogador a menos vivo, vencedor no fim da fila com +1 ponto.
**Requisitos:** RF03, RF04, RF05, RF06, RF10.

---

## UC03 — Progresso de jogador

**Ator:** sistema
**Disparo:** `PlayerScored`.

1. Score do vencedor sobe.
2. GUI recalcula `maxScore` entre os jogadores vivos.
3. `ColorMapper` reavalia a cor do jogador; toda a arena e a fila são repintadas
   com a nova escala.

**Pós-condição:** cor do jogador reflete o score atual.
**Requisitos:** RF04, RF07.

---

## UC04 — Eliminar jogador

**Ator:** sistema
**Disparo:** `PlayerEliminated`.

1. `alive` decrementa e a condição da fila é sinalizada — inclusive quando
   `waiting` não muda, o que é o que permite formar o último par
   ([arquitetura.md §3.1](arquitetura.md)).
2. GUI remove o jogador da fila desenhada e da arena.

**Pós-condição:** jogador não volta a aparecer em nenhuma partida.
**Requisitos:** RF05.

---

## UC05 — Redimensionar arena

**Ator:** sistema (GUI)
**Disparo:** `MatchStarted` ou `MatchEnded` — o número de matches ativos mudou.

1. `ArenaPanel` recalcula a grade: `cols = ceil(sqrt(n))`,
   `rows = ceil(n / cols)`.
2. Colunas e linhas recebem percentuais iguais; cada `MatchView` cresce para
   preencher sua célula.

**Pós-condição:** todas as partidas ativas visíveis, em células de mesmo
tamanho.
**Requisitos:** RF11.

---

## UC06 — Encerrar torneio

**Ator:** sistema
**Pré-condição:** `alive == 1` e nenhum match ativo.

1. `dequeuePair()` devolve `null`; o loop do orquestrador encerra.
2. Orquestrador desliga o pool, fecha as contas em `TournamentStats.snapshot()`
   e publica `TournamentEnded` com o campeão e as `TournamentMetrics`.
3. GUI troca a arena — vazia nesse ponto — pelo painel de resultado: campeão,
   total de partidas, tempo médio por partida, vazão e tempo total. Sem janela
   separada; os controles de `N` e `T` continuam acessíveis ao lado.
4. Um novo *start* devolve a arena ao centro (UC01), e o fim do torneio
   seguinte traz o painel de volta com os números novos.

**Pós-condição:** todas as threads encerradas, campeão e métricas em tela.
**Requisitos:** RF09, RF12.

---

## UC07 — Jogador humano *(stretch, não implementado)*

**Ator:** usuário
**Pré-condição:** um dos jogadores da partida é o humano.

1. `MatchRunner` publica `MatchStarted` e bloqueia esperando a jogada.
2. GUI apresenta as três opções; usuário escolhe.
3. GUI entrega a jogada à thread do match, que retoma o UC02 a partir do
   passo 5.

**Observação:** o desenho suporta o caso — a thread do match já é dona da sua
própria espera e a GUI já se comunica por eventos —, mas o caminho de volta
(GUI → thread) não existe na v1. Adicioná-lo significa um canal de entrada por
match, não uma mudança de arquitetura.
**Requisitos:** nenhum da v1.
