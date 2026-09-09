package rps.concurrency;

/**
 * RF12: os numeros fechados do torneio, prontos para a apresentacao apenas
 * formatar. Nada aqui e calculado na UI thread (RNF01) — quem mede e a thread
 * que jogou a partida, e quem fecha a conta e o orquestrador.
 *
 * @param totalMillis       do {@code Tournament.start()} ate o
 *                          {@code TournamentEnded}
 * @param matches           partidas concluidas; empate (RF06) e replay da mesma
 *                          dupla, entao a partida conta uma vez so
 * @param avgMatchMillis    duracao media de uma partida. Com T &gt; 1 as
 *                          partidas correm em paralelo, entao a soma das
 *                          duracoes passa de {@code totalMillis} — sao medidas
 *                          diferentes, nao uma inconsistencia
 * @param matchesPerSecond  vazao: e a metrica que reage a T
 */
public record TournamentMetrics(long totalMillis,
                                int matches,
                                double avgMatchMillis,
                                double matchesPerSecond) {
}
