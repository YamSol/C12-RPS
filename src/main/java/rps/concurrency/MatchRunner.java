package rps.concurrency;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Semaphore;

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
    private final Semaphore slots;
    private final TournamentStats stats;

    public MatchRunner(Match match,
                       PlayerQueue queue,
                       EventBus bus,
                       Set<Match> activeMatches,
                       long roundDelayMs,
                       Semaphore slots,
                       TournamentStats stats) {
        this.match = match;
        this.queue = queue;
        this.bus = bus;
        this.activeMatches = activeMatches;
        this.roundDelayMs = roundDelayMs;
        this.slots = slots;
        this.stats = stats;
    }

    @Override
    public void run() {
        // RF12: o relogio da partida corre aqui, na thread que joga — a amostra
        // nao pode sair do assinante do EventBus (RNF01).
        long startedAtNanos = System.nanoTime();
        try {
            match.state(Match.State.RUNNING);
            bus.publish(new Events.MatchStarted(match));

            List<Round> rounds = new ArrayList<>();
            Round round;
            do {
                Thread.sleep(roundDelayMs);
                round = new Round(Move.random(), Move.random());
                rounds.add(round);
                match.lastRound(round);
                bus.publish(new Events.RoundPlayed(match, round));
            } while (round.isDraw()); // RF06: empate -> replay da mesma dupla

            boolean p1Won = round.p1().beats(round.p2()) > 0;
            Player winner = p1Won ? match.p1() : match.p2();
            Player loser = p1Won ? match.p2() : match.p1();

            MatchResult result = new MatchResult(winner, loser, rounds);
            match.result(result);
            match.state(Match.State.DONE);

            // A partida termina aqui. A pausa logo abaixo e so cosmetica, entao
            // fica fora da amostra pra nao inflar a media (RF12). Registrar
            // antes do finishMatch tambem garante que a contagem ja esteja
            // fechada quando o orquestrador perceber o fim do torneio.
            stats.recordMatch(System.nanoTime() - startedAtNanos);

            // Deixa o resultado visivel um instante antes de tirar o match da arena.
            Thread.sleep(roundDelayMs);

            int score = winner.win();               // RF04
            queue.finishMatch(winner, loser);       // RF04 + RF05, atomico
            activeMatches.remove(match);

            bus.publish(new Events.MatchEnded(match, result));
            bus.publish(new Events.PlayerScored(winner, score));
            bus.publish(new Events.PlayerEliminated(loser));
            bus.publish(new Events.QueueChanged(queue.snapshot(), queue.alive()));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            // Devolve a vaga so aqui, depois de o vencedor ja estar de volta na
            // fila: quando o orquestrador acordar, a fila que ele le e a real.
            // No finally para que um match interrompido nao vaze a permissao e
            // encolha as partidas simultaneas pro resto do torneio.
            slots.release();
        }
    }
}
