package rps.concurrency;

/**
 * RF01/RF02: N e T configuraveis. RNF03: mudar T nao toca em nenhuma outra classe.
 *
 * @param players      N — quantos jogadores entram no torneio
 * @param threads      T — tamanho do pool de matches
 * @param roundDelayMs duracao artificial de cada rodada, pra GUI ter o que mostrar
 *                     (pergunta aberta #2 do planejamento)
 */
public record TournamentConfig(int players, int threads, long roundDelayMs) {

    public static TournamentConfig defaults() {
        return new TournamentConfig(60, 4, 350);
    }
}
