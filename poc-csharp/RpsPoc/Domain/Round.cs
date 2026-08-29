namespace RpsPoc.Domain;

/// <summary>
/// Uma rodada dentro de um match. Record de referencia (nao struct) porque
/// <see cref="Match.LastRound"/> e publicado entre threads via Volatile.
/// </summary>
public sealed record Round(Move P1, Move P2)
{
    public bool IsDraw => P1.Beats(P2) == 0;
}
