package rps.concurrency;

import java.util.Set;
import java.util.concurrent.ExecutorService;

import rps.domain.Match;
import rps.domain.Player;

/**
 * Thread propria, em loop: tira duplas da fila e entrega pro pool.
 * Nao joga nada — so parea e despacha.
 */
public final class Orchestrator implements Runnable {

    private final PlayerQueue queue;
    private final ExecutorService pool;
    private final EventBus bus;
    private final Set<Match> activeMatches;
    private final long roundDelayMs;

    private int nextMatchId = 1;

    public Orchestrator(PlayerQueue queue,
                        ExecutorService pool,
                        EventBus bus,
                        Set<Match> activeMatches,
                        long roundDelayMs) {
        this.queue = queue;
        this.pool = pool;
        this.bus = bus;
        this.activeMatches = activeMatches;
        this.roundDelayMs = roundDelayMs;
    }

    @Override
    public void run() {
        try {
            while (true) {
                Player[] pair = queue.dequeuePair();
                if (pair == null) {
                    break; // RF09: sobrou 1, nenhum match ativo
                }
                Match match = new Match(nextMatchId++, pair[0], pair[1]);
                activeMatches.add(match);
                bus.publish(new Events.QueueChanged(queue.snapshot(), queue.alive()));
                pool.submit(new MatchRunner(match, queue, bus, activeMatches, roundDelayMs));
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            pool.shutdown();
            bus.publish(new Events.TournamentEnded(queue.champion()));
        }
    }
}
