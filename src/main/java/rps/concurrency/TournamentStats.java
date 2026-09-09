package rps.concurrency;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * RF12: coleta as amostras do torneio. Escrito pelas T threads de partida
 * (dai os contadores atomicos) e lido uma unica vez, no fim, pela thread do
 * orquestrador.
 *
 * <p>As amostras sao tiradas na origem, nunca no assinante do {@link EventBus}:
 * o {@code Platform::runLater} adiciona latencia de entrega que entraria na
 * medida (RNF01).
 *
 * <p>O relogio comeca na construcao, entao o objeto e criado em
 * {@link Tournament#start()} — nao no construtor do torneio.
 */
public final class TournamentStats {

    private final long startedAtNanos = System.nanoTime();
    private final AtomicLong matchTimeNanos = new AtomicLong();
    private final AtomicInteger matches = new AtomicInteger();

    /** Uma partida concluida, com quanto ela levou do inicio ate ser decidida. */
    public void recordMatch(long durationNanos) {
        matchTimeNanos.addAndGet(durationNanos);
        matches.incrementAndGet();
    }

    /** Fecha as contas para o {@code TournamentEnded}. */
    public TournamentMetrics snapshot() {
        long elapsedNanos = System.nanoTime() - startedAtNanos;
        int played = matches.get();

        double avgMatchMillis = played == 0
                ? 0
                : matchTimeNanos.get() / 1_000_000.0 / played;
        double matchesPerSecond = elapsedNanos == 0
                ? 0
                : played * 1_000_000_000.0 / elapsedNanos;

        return new TournamentMetrics(
                elapsedNanos / 1_000_000,
                played,
                avgMatchMillis,
                matchesPerSecond);
    }
}
