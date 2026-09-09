# Decisões de arquitetura (ADRs)

Registro das decisões que fecham o planejamento (#1) e a comparação de stacks
(#2). Cada ADR diz o contexto, a decisão e o que ela custa — não só o que ela
resolve.

Status possíveis: **aceita**, **substituída**, **revogada**.

---

## ADR-001 — Sem renderização em terminal

**Data:** 2026-08-29 · **Status:** aceita

**Contexto.** A ideia inicial previa desenhar a arena no terminal com blocos
coloridos.

**Decisão.** Usar biblioteca gráfica; abandonar o terminal.

**Por quê.** Renderizar no terminal amarra o projeto em código de renderização
condicional — ANSI vs Windows Terminal vs curses vs largura de caractere — e
gasta o tempo do trabalho em layout, não no problema real, que é **threads e
orquestração**. O RF11 (arena que se redivide) seria, no terminal, a maior parte
do esforço.

**Custo.** Dependência de toolchain gráfica e um passo a mais para quem for
rodar o projeto.

---

## ADR-002 — Três camadas com EventBus entre concorrência e GUI

**Data:** 2026-08-29 · **Status:** aceita

**Contexto.** A GUI precisa refletir estado que muda em T threads
simultaneamente (RF10), sem virar fonte de condição de corrida (RNF01).

**Decisão.** Separar Domínio / Concorrência / Apresentação, com um `EventBus`
(observer) no meio. As threads de jogo publicam de qualquer thread; o dispatch é
marshalado para a UI thread.

**Por quê.** É o que mantém a GUI fora dos locks e o domínio testável sem
JavaFX. Também torna a camada de apresentação substituível: trocar JavaFX por
Swing é trocar o `Executor` do bus e reescrever `rps.ui`.

**Custo.** Indireção — a GUI nunca lê estado "ao vivo", só o que chegou por
evento; e a ordem entre eventos de matches diferentes não é garantida.

**Detalhes:** [arquitetura.md §4](arquitetura.md).

---

## ADR-003 — Stack: Java 21 + JavaFX

**Data:** 2026-09-09 · **Status:** aceita · **Fecha:** #2

**Contexto.** Duas candidatas, ambas multiplataforma (RNF04): Java 21 + JavaFX e
.NET 8 + Avalonia. WPF já estava fora por ser Windows-only. Foi escrita a mesma
fatia vertical nas duas branches (`poc/java-javafx`, `poc/csharp-avalonia`).

**Decisão.** O projeto segue em **Java 21 + JavaFX**. A branch
`poc/csharp-avalonia` fica arquivada como registro da comparação.

**Por quê.**

- **Ergonomia de concorrência.** O RF02 cai direto em `ExecutorService(T)`: T *é*
  o tamanho do pool. Em .NET o thread pool é global, então "T partidas
  simultâneas" vira `SemaphoreSlim(T)` — o requisito muda de forma junto com a
  stack, o que é um sinal ruim para um trabalho cujo tema é justamente threads.
- **Orquestrador como thread dedicada.** `dequeuePair()` bloqueia por desenho. Em
  Java o orquestrador já é uma thread própria e bloquear ali é natural. Na versão
  C# ele precisa de um `Task.Run` só para não prender a thread chamadora —
  atrito que não existe do lado Java.
- **Familiaridade da equipe**, que pesa de verdade no prazo do trabalho.
- **Empate real no RF03:** nenhuma das duas oferece "tire DOIS" atômico
  (`BlockingQueue`, `ConcurrentQueue` e `Channel<T>` caem todos em lock +
  condição). Esse critério não decidiu nada — vale registrar que foi verificado.

**Estado da evidência — o que esta decisão *não* tem.** A comparação não foi
completa, e isso fica registrado em vez de maquiado:

- Java: compila limpo (`javac --release 21 -Xlint:all`, JavaFX 21.0.4) e o
  `HeadlessMain` termina com 1 campeão e `activeMatches == 0` em N/T = 2/1, 3/1,
  7/16, 60/4, 61/4 e 200/8 — incluindo N ímpar e T maior que o número de partidas
  possíveis. **A janela JavaFX nunca foi executada** (não havia Gradle na
  máquina): RF10 e RF11 estão implementados mas não verificados em tela.
- C#: **nunca compilou** (não havia .NET SDK na máquina). O código é tradução
  direta da versão Java, escrito às cegas contra a API do Avalonia 11.2.

Ou seja: os critérios "setup e build", "distribuição" e "layout dinâmico em
runtime" não foram medidos nas duas pontas. A decisão se apoia nos critérios de
ergonomia de concorrência e familiaridade, que são os que a PoC efetivamente
exercitou, e no risco de continuar com a stack cuja PoC nunca rodou.

**Custo / risco aceito.** Primeira tarefa de quem pegar o projeto: rodar
`gradle run` e conferir a arena redividindo (RF11). Se a GUI se mostrar
inviável, JavaFX pode ser trocado por Swing sem tocar em nenhuma outra camada
(ADR-002) — o que limita o tamanho do erro possível.

---

## ADR-004 — Empate: replay da mesma dupla

**Data:** 2026-09-09 · **Status:** aceita · **Pergunta aberta #1**

**Decisão.** Rodada empatada gera nova rodada entre os mesmos dois jogadores,
até haver vencedor. Nada de moeda ao ar.

**Por quê.** É como pedra-papel-tesoura funciona; a alternativa introduziria
aleatoriedade que não é do jogo. O alongamento é limitado — cada rodada tem 1/3
de chance de empatar, então a partida termina em 1,5 rodada em média.

**Consequência.** Uma partida é uma sequência de rodadas, não uma rodada só. O
histórico completo fica em `MatchResult.rounds`, o que também serve para animar.

---

## ADR-005 — Partida tem duração artificial; `MatchEnded` = "saiu da arena"

**Data:** 2026-09-09 · **Status:** aceita · **Pergunta aberta #2**

**Contexto.** Se a partida for computada e devolvida instantaneamente, ela some
da arena antes de qualquer pessoa ver.

**Decisão.** Cada rodada dorme `roundDelayMs` (default 350 ms) antes de ser
jogada, e há uma segunda pausa de `roundDelayMs` depois da rodada decisiva. Só
então o vencedor pontua, o perdedor é eliminado e `MatchEnded` é publicado.

**Por quê.** `MatchEnded` passa a significar **"a partida saiu da arena"**, não
"a partida foi decidida". É essa distinção que dá à GUI uma janela para mostrar
o resultado. A alternativa — publicar o fim assim que decidido e deixar a GUI
segurar o desenho — colocaria tempo de exibição dentro da camada de
apresentação, e a arena passaria a mostrar matches que o modelo já considera
encerrados.

**Consequência.** `roundDelayMs = 0` roda o torneio o mais rápido possível; é o
modo do smoke test headless. O tempo de simulação não representa nada — é
puramente para o olho humano.

---

## ADR-006 — Fila ímpar: quem sobra espera, sem "bye"

**Data:** 2026-09-09 · **Status:** aceita · **Pergunta aberta #3**

**Decisão.** Se sobrar um jogador sem par, ele fica na fila até alguém voltar da
arena. Não existe vitória automática.

**Por quê.** Um "bye" daria ponto sem partida, quebrando a equivalência entre
score e número de vitórias (RF07 depende disso para a cor significar algo). A
espera é curta na prática: há sempre partidas em andamento devolvendo
vencedores, exceto no fim do torneio, quando `alive == 1` encerra tudo.

**Consequência.** `dequeuePair()` bloqueia enquanto `waiting.size() < 2`. É
correto e não trava — verificado com N ímpar (3 e 61) no smoke test.

---

## ADR-007 — Jogadores jogam aleatório uniforme

**Data:** 2026-09-09 · **Status:** aceita · **Pergunta aberta #4**

**Decisão.** Toda jogada é sorteada uniformemente entre as três opções
(`ThreadLocalRandom`). Sem personalidade, sem viés, sem memória.

**Por quê.** O objeto do trabalho é concorrência, não estratégia. Viés por
jogador tornaria o score uma medida de sorte na distribuição inicial e não
mudaria em nada a parte difícil. Fica como extensão possível, isolada em
`Move.random()` / uma estratégia por `Player`.

**Consequência.** O campeão é fruto de sorte pura — o que é o esperado e deve
ficar claro na apresentação do trabalho.

---

## ADR-008 — Vencedor volta ao fim da fila no momento da vitória

**Data:** 2026-09-09 · **Status:** aceita · **Pergunta aberta #5**

**Decisão.** `enqueue` insere no fim da fila **como ela está naquele instante**
(`addLast`), não em uma posição reservada por ordem de rodada.

**Por quê.** A fila é FIFO pura e não tem noção de "rodada do torneio"; não
existe outro fim para inserir. Explicitar isso evita que alguém no futuro tente
introduzir ordenação por rodada e mude o comportamento sem perceber.

**Consequência.** Partidas rápidas devolvem seus vencedores antes de partidas
lentas iniciadas junto, então a ordem da fila depende do escalonamento das
threads. É não-determinístico por desenho, e sem impacto sobre o resultado do
torneio.

---

## ADR-009 — `ReentrantLock` + `Condition` em vez de `BlockingQueue`

**Data:** 2026-09-09 · **Status:** aceita

**Contexto.** O RF03 exige retirar **dois** jogadores atomicamente.

**Decisão.** `PlayerQueue` usa `ArrayDeque` protegido por `ReentrantLock` com uma
`Condition`, em vez de uma fila concorrente pronta.

**Por quê.** Duas chamadas de `take()` em um `BlockingQueue` não são atômicas
entre si — bastaria uma segunda thread de orquestração para cruzar duplas. E a
espera precisa acordar em dois eventos diferentes: chegou jogador na fila
**ou** alguém foi eliminado (fim do torneio). Nenhuma fila pronta oferece as
duas coisas.

**Consequência.** `eliminate()` também sinaliza a condição, mesmo sem alterar
`waiting`; e `dequeuePair()` devolve `null` como sinal de encerramento. Sem esse
sinal, o último par nunca se forma e o orquestrador dorme para sempre — a única
armadilha de deadlock do desenho ([arquitetura.md §3.1](arquitetura.md)).

---

## ADR-010 — `Launcher` separado da classe `Application`

**Data:** 2026-09-09 · **Status:** aceita

**Contexto.** Rodar JavaFX não-modular pelo classpath com a própria
`Application` como main-class falha com *"JavaFX runtime components are
missing"*.

**Decisão.** Uma classe `Launcher` sem herança serve de main-class e chama
`Application.launch(MainApp.class, args)`.

**Consequência.** Duas classes onde deveria haver uma. É atrito de toolchain do
JavaFX, não escolha de desenho — registrado aqui para ninguém "simplificar" e
quebrar a execução.

---

## ADR-011 — `Semaphore(T)` limita as partidas simultâneas, não o tamanho do pool

**Data:** 2026-09-09 · **Status:** aceita

**Contexto.** O orquestrador entregava cada par com `pool.submit()` e confiava
no `newFixedThreadPool(T)` para limitar as partidas simultâneas. Não limita:
`submit()` num pool fixo **nunca bloqueia**, porque a fila interna do executor é
ilimitada. O loop drenava a fila inteira num piscar de olhos e os pares
excedentes ficavam parados dentro do executor.

Medido com N=60, T=4: 30 partidas em voo em vez de 4, e a fila caindo de 60 para
0 em menos de 25 ms — média de 1,4 jogador visível durante todo o torneio. Os 52
jogadores restantes não estavam nem na fila nem na arena: estavam num terceiro
estado invisível, dentro da fila do `ExecutorService`.

**Decisão.** Um `Semaphore` com T permissões, criado em `Tournament`. O
orquestrador faz `acquire()` **antes** de `dequeuePair()`; o `MatchRunner`
devolve a permissão num `finally`, depois de o vencedor já estar de volta na
fila.

**Por quê.** O RF02 fala em *partidas simultâneas*, e o único jeito de o número
ser verdade é segurar o pareamento, não o despacho. Adquirir antes de
desenfileirar é o que preserva o invariante que a GUI depende: **todo jogador
está ou na fila visível, ou numa das T partidas ativas** — nunca num limbo. Como
efeito colateral bem-vindo, o semáforo é exatamente o que a nota do RF02 já
previa para .NET (`SemaphoreSlim`), então as duas stacks passam a expressar T do
mesmo jeito e o pool volta a ser só um provedor de threads.

**Alternativas descartadas.** Pool com fila limitada + `CallerRunsPolicy` faria
o orquestrador *jogar* a partida rejeitada, misturando os papéis de quem pareia
e quem joga. `SynchronousQueue` com política de rejeição bloqueante resolve o
número, mas esconde a intenção num detalhe de configuração do executor.

**Consequência.** `MatchRunner` e `Orchestrator` ganham um parâmetro. Na
inanição da fila o orquestrador segura uma vaga enquanto espera o par, então o
paralelismo efetivo é T-1 nesse instante — sem perda real, porque não há dois
jogadores disponíveis para preencher a vaga de qualquer forma. Verificado: T
respeitado exatamente para T=1, 2, 4 e 8, e terminação sem deadlock em N/T
extremos (N=2 T=8, N=3 T=1, N=500 T=64, N ímpar) e em 25 execuções repetidas.
