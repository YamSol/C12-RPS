# NOTAS Projeto C12 - RPS

## Planejamento

Ideação Inicial:
```
Aplicação: Simulação de Jogos de pedra papel tesoura com elementos de concorrência.
Aplicação de conceitos de Threads, para cada jogo entre dois jogadores.

<!-- Graficamente, cada jogo sera renderizado como um retangulo de determinada cor, e, mediante vitoria, ocupa tambem o espaco antes ocupado pelo adversario. -->

O sistema vai ter uma fila unica. cada jogador tem um pontuao (cor), que incrementa a cada vitoria. a perca resulta na eliminacao do jogador da fila. A fila é mostrada graficamente, indicanod os proximos a jogarem. o Orquestrador carrega os proximos jogadores em um jogo, que é direcionado à N-esina thread disponivel. O jogo termina com todos os jogos finalizados e apenas um jogador vivo.

Ideia: usar paleta com mapa de cores que indiquem "maior pontuacao" (semelhante ao mapa de temperatura, por exemplo).
```

