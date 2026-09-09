package rps.concurrency;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import rps.domain.Match;
import rps.domain.MatchResult;
import rps.domain.Move;
import rps.domain.Player;
import rps.domain.Round;

/** Roda em uma das T threads do pool. Um match, do inicio ao fim. */
public final class MatchRunner implements Runnable {

    private final Match match;
    private final PlayerQueue queue;
    private final EventBus bus;
    private final Set<Match> activeMatches;
    private final long roundDelayMs;

    // Estatísticas compartilhadas do torneio
    private final AtomicLong totalMatchTimeNanos;
    private final AtomicInteger matchesPlayed;

    public MatchRunner(
            Match match,
            PlayerQueue queue,
            EventBus bus,
            Set<Match> activeMatches,
            long roundDelayMs,
            AtomicLong totalMatchTimeNanos,
            AtomicInteger matchesPlayed
    ) {
        this.match = match;
        this.queue = queue;
        this.bus = bus;
        this.activeMatches = activeMatches;
        this.roundDelayMs = roundDelayMs;
        this.totalMatchTimeNanos = totalMatchTimeNanos;
        this.matchesPlayed = matchesPlayed;
    }

    @Override
    public void run() {

        // Marca o início desta partida
        long matchStartTime = System.nanoTime();

        try {
            match.state(Match.State.RUNNING);
            bus.publish(new Events.MatchStarted(match));

            List<Round> rounds = new ArrayList<>();

            Round round;

            do {
                Thread.sleep(roundDelayMs);

                round = new Round(
                        Move.random(),
                        Move.random()
                );

                rounds.add(round);

                match.lastRound(round);

                bus.publish(
                        new Events.RoundPlayed(match, round)
                );

            } while (round.isDraw()); // empate -> replay da mesma dupla

            boolean p1Won =
                    round.p1().beats(round.p2()) > 0;

            Player winner =
                    p1Won ? match.p1() : match.p2();

            Player loser =
                    p1Won ? match.p2() : match.p1();

            MatchResult result =
                    new MatchResult(
                            winner,
                            loser,
                            rounds
                    );

            match.result(result);
            match.state(Match.State.DONE);

            // Deixa o resultado visível por um instante
            Thread.sleep(roundDelayMs);

            /*
             * Calcula a duração ANTES de eliminar o jogador.
             * Isso garante que a estatística esteja pronta
             * antes que o Orchestrator perceba que o torneio acabou.
             */
            long matchTime =
                    System.nanoTime() - matchStartTime;

            totalMatchTimeNanos.addAndGet(matchTime);
            matchesPlayed.incrementAndGet();

            int score = winner.win();

            queue.eliminate(loser);
            queue.enqueue(winner);

            activeMatches.remove(match);

            bus.publish(
                    new Events.MatchEnded(
                            match,
                            result
                    )
            );

            bus.publish(
                    new Events.PlayerScored(
                            winner,
                            score
                    )
            );

            bus.publish(
                    new Events.PlayerEliminated(
                            loser
                    )
            );

            bus.publish(
                    new Events.QueueChanged(
                            queue.snapshot(),
                            queue.aliveSnapshot()
                    )
            );

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}