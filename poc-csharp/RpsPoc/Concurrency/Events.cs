using RpsPoc.Domain;

namespace RpsPoc.Concurrency;

/// <summary>Os eventos que trafegam no <see cref="EventBus"/>.</summary>
public sealed record MatchStarted(Match Match);

public sealed record RoundPlayed(Match Match, Round Round);

public sealed record MatchEnded(Match Match, MatchResult Result);

public sealed record PlayerScored(Player Player, int Score);

public sealed record PlayerEliminated(Player Player);

public sealed record QueueChanged(IReadOnlyList<Player> Waiting, int Alive);

public sealed record TournamentEnded(Player? Champion);
