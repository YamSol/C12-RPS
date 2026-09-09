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

    public static final int MIN_PLAYERS = 2;
    public static final int MAX_PLAYERS = 500;
    public static final int MIN_THREADS = 1;
    public static final int MAX_THREADS = 64;

    /**
     * As faixas moram aqui, e nao no Spinner: a UI e so mais um cliente do
     * config. T > N/2 passa de proposito — o Semaphore(T) do Tournament apenas
     * deixa vagas ociosas, e ver o pool ocioso e parte do que a PoC quer medir.
     */
    public TournamentConfig {
        if (players < MIN_PLAYERS || players > MAX_PLAYERS) {
            throw new IllegalArgumentException(
                    "players fora de [" + MIN_PLAYERS + ", " + MAX_PLAYERS + "]: " + players);
        }
        if (threads < MIN_THREADS || threads > MAX_THREADS) {
            throw new IllegalArgumentException(
                    "threads fora de [" + MIN_THREADS + ", " + MAX_THREADS + "]: " + threads);
        }
        if (roundDelayMs < 0) {
            throw new IllegalArgumentException("roundDelayMs negativo: " + roundDelayMs);
        }
    }

    public static TournamentConfig defaults() {
        return new TournamentConfig(60, 4, 350);
    }
}
