# NOTAS Projeto C12 - RPS

## Documentação

| Documento | Conteúdo |
|---|---|
| [docs/arquitetura.md](docs/arquitetura.md) | Camadas, contrato do EventBus, armadilha de deadlock do desenho. |
| [docs/requisitos.md](docs/requisitos.md) | RF01–RF11 e RNF01–RNF04 consolidados. |
| [docs/casos-de-uso.md](docs/casos-de-uso.md) | UC01–UC07. |
| [docs/decisoes.md](docs/decisoes.md) | ADRs, incluindo a escolha da stack e as 5 perguntas abertas do planejamento. |

Stack decidida: **Java 21 + JavaFX** ([ADR-003](docs/decisoes.md#adr-003--stack-java-21--javafx)).

## Planejamento

Ideação Inicial:
```
Aplicação: Simulação de Jogos de pedra papel tesoura com elementos de concorrência.
Aplicação de conceitos de Threads, para cada jogo entre dois jogadores.

<!-- Graficamente, cada jogo sera renderizado como um retangulo de determinada cor, e, mediante vitoria, ocupa tambem o espaco antes ocupado pelo adversario. -->

O sistema vai ter uma fila unica. cada jogador tem um pontuao (cor), que incrementa a cada vitoria. a perca resulta na eliminacao do jogador da fila. A fila é mostrada graficamente, indicanod os proximos a jogarem. o Orquestrador carrega os proximos jogadores em um jogo, que é direcionado à N-esina thread disponivel. O jogo termina com todos os jogos finalizados e apenas um jogador vivo.

Ideia: usar paleta com mapa de cores que indiquem "maior pontuacao" (semelhante ao mapa de temperatura, por exemplo).
```

