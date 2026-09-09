package rps.concurrency;

import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import rps.domain.Match;
import rps.domain.Player;

/**
 * Thread propria, em loop:
 * tira duplas da fila e entrega pro pool.
 * Nao joga nada — so parea e despacha.
 */
public final class Orchestrator implements Runnable {

    private final PlayerQueue queue;
    private final ExecutorService pool;
    private final EventBus bus;
    private final Set<Match> activeMatches;
    private final long roundDelayMs;

    // Estatísticas
    private final long tournamentStartTime;
    private final AtomicLong totalMatchTimeNanos;
    private final AtomicInteger matchesPlayed;

    private int nextMatchId = 1;

    public Orchestrator(
            PlayerQueue queue,
            ExecutorService pool,
            EventBus bus,
            Set<Match> activeMatches,
            long roundDelayMs,
            long tournamentStartTime,
            AtomicLong totalMatchTimeNanos,
            AtomicInteger matchesPlayed
    ) {
        this.queue = queue;
        this.pool = pool;
        this.bus = bus;
        this.activeMatches = activeMatches;
        this.roundDelayMs = roundDelayMs;

        this.tournamentStartTime = tournamentStartTime;
        this.totalMatchTimeNanos = totalMatchTimeNanos;
        this.matchesPlayed = matchesPlayed;
    }

    @Override
    public void run() {
        try {

            while (true) {

                Player[] pair = queue.dequeuePair();

                if (pair == null) {
                    break;
                }

                Match match = new Match(
                        nextMatchId++,
                        pair[0],
                        pair[1]
                );

                activeMatches.add(match);

                bus.publish(
                        new Events.QueueChanged(
                                queue.snapshot(),
                                queue.aliveSnapshot()
                        )
                );

                pool.submit(
                        new MatchRunner(
                                match,
                                queue,
                                bus,
                                activeMatches,
                                roundDelayMs,
                                totalMatchTimeNanos,
                                matchesPlayed
                        )
                );
            }

        } catch (InterruptedException e) {

            Thread.currentThread().interrupt();

        } finally {

            pool.shutdown();

            // Tempo total real do torneio
            long totalTournamentTime =
                    System.nanoTime() - tournamentStartTime;

            bus.publish(
                    new Events.TournamentEnded(
                            queue.champion(),
                            totalTournamentTime,
                            totalMatchTimeNanos.get(),
                            matchesPlayed.get()
                    )
            );
        }
    }
}