package rps.concurrency;

import java.util.List;

import rps.domain.Match;
import rps.domain.MatchResult;
import rps.domain.Player;
import rps.domain.Round;

/** Os eventos que trafegam no {@link EventBus}. */
public final class Events {

    private Events() {
    }

    public record MatchStarted(Match match) { }

    public record RoundPlayed(Match match, Round round) { }

    public record MatchEnded(Match match, MatchResult result) { }

    public record PlayerScored(Player player, int score) { }

    public record PlayerEliminated(Player player) { }

    public record QueueChanged(List<Player> waiting, int alive) { }

    /** RF12: o campeao vem com os numeros do torneio ja fechados. */
    public record TournamentEnded(Player champion, TournamentMetrics metrics) { }
}
