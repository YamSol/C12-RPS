namespace RpsPoc.Domain;

/// <summary>Resultado fechado de um match. Rounds guarda o historico (util pra animar).</summary>
public sealed record MatchResult(Player Winner, Player Loser, IReadOnlyList<Round> Rounds);
