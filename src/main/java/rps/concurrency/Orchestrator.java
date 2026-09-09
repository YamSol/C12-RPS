package rps.concurrency;

import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.Semaphore;

import rps.domain.Match;
import rps.domain.Player;

/**
 * Thread propria, em loop: tira duplas da fila e entrega pro pool.
 * Nao joga nada — so parea e despacha.
 *
 * <p>RF02: quem limita as partidas simultaneas e o semaforo {@code slots}, nao
 * o tamanho do pool. {@code pool.submit()} num {@code FixedThreadPool} nunca
 * bloqueia — a fila interna do executor e ilimitada — entao, sem o semaforo,
 * este loop drenaria a fila inteira de uma vez e os pares excedentes ficariam
 * esperando dentro do executor: fora da fila e fora da arena, invisiveis.
 */
public final class Orchestrator implements Runnable {

    private final PlayerQueue queue;
    private final ExecutorService pool;
    private final EventBus bus;
    private final Set<Match> activeMatches;
    private final long roundDelayMs;
    private final Semaphore slots;

    private int nextMatchId = 1;

    public Orchestrator(PlayerQueue queue,
                        ExecutorService pool,
                        EventBus bus,
                        Set<Match> activeMatches,
                        long roundDelayMs,
                        Semaphore slots) {
        this.queue = queue;
        this.pool = pool;
        this.bus = bus;
        this.activeMatches = activeMatches;
        this.roundDelayMs = roundDelayMs;
        this.slots = slots;
    }

    @Override
    public void run() {
        try {
            while (true) {
                // Espera uma vaga ANTES de desenfileirar: enquanto as T partidas
                // estiverem ocupadas, os jogadores continuam visiveis na fila.
                slots.acquire();

                Player[] pair = queue.dequeuePair();
                if (pair == null) {
                    slots.release();
                    break; // RF09: sobrou 1, nenhum match ativo
                }
                Match match = new Match(nextMatchId++, pair[0], pair[1]);
                activeMatches.add(match);
                bus.publish(new Events.QueueChanged(queue.snapshot(), queue.alive()));
                try {
                    // A vaga passa a ser do MatchRunner, que a devolve ao terminar.
                    pool.submit(new MatchRunner(match, queue, bus, activeMatches, roundDelayMs, slots));
                } catch (RejectedExecutionException e) {
                    slots.release(); // pool ja em shutdown (stop()): ninguem vai devolver por nos
                    break;
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            pool.shutdown();
            bus.publish(new Events.TournamentEnded(queue.champion()));
        }
    }
}
