package rps.concurrency;

import java.util.List;

import rps.domain.Match;
import rps.domain.MatchResult;
import rps.domain.Player;
import rps.domain.Round;

/** Os eventos que trafegam no EventBus. */
public final class Events {

    private Events() {
    }

    public record MatchStarted(Match match) { }

    public record RoundPlayed(Match match, Round round) { }

    public record MatchEnded(Match match, MatchResult result) { }

    public record PlayerScored(Player player, int score) { }

    public record PlayerEliminated(Player player) { }

    /*
     * waiting = quem está esperando partida.
     * alivePlayers = todos que ainda estão vivos.
     */
    public record QueueChanged(
            List<Player> waiting,
            List<Player> alivePlayers
    ) { }

    /**
     * Evento final do torneio.
     *
     * totalTimeNanos = duração total do torneio.
     * totalMatchTimeNanos = soma da duração de todas as partidas.
     * matchesPlayed = quantidade total de partidas.
     */
    public record TournamentEnded(
            Player champion,
            long totalTimeNanos,
            long totalMatchTimeNanos,
            int matchesPlayed
    ) { }
}