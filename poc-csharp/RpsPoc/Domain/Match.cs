namespace RpsPoc.Domain;

public enum MatchState
{
    Pending,
    Running,
    Done,
}

/// <summary>
/// Partida entre dois jogadores. Escrita pela task do MatchRunner, lida pela
/// UI thread — os campos mutaveis sao publicados com volatile/Volatile.
/// </summary>
public sealed class Match(int id, Player p1, Player p2)
{
    private volatile MatchState _state = MatchState.Pending;
    private MatchResult? _result;
    private Round? _lastRound;

    public int Id { get; } = id;

    public Player P1 { get; } = p1;

    public Player P2 { get; } = p2;

    public MatchState State
    {
        get => _state;
        set => _state = value;
    }

    public MatchResult? Result
    {
        get => Volatile.Read(ref _result);
        set => Volatile.Write(ref _result, value);
    }

    public Round? LastRound
    {
        get => Volatile.Read(ref _lastRound);
        set => Volatile.Write(ref _lastRound, value);
    }
}
