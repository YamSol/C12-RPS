namespace RpsPoc.Domain;

/// <summary>
/// Jogador. O score e escrito por threads de match diferentes ao longo do
/// torneio e lido pela UI thread — dai Interlocked/Volatile.
/// </summary>
public sealed class Player(int id)
{
    private int _score;

    public int Id { get; } = id;

    public int Score => Volatile.Read(ref _score);

    /// <summary>RF04: +1 ponto por vitoria.</summary>
    public int Win() => Interlocked.Increment(ref _score);

    public override string ToString() => $"P{Id} ({Score})";
}
