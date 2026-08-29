namespace RpsPoc.Domain;

/// <summary>Jogada de RPS. Camada de dominio: sem thread, sem GUI.</summary>
public enum Move
{
    Rock,
    Paper,
    Scissors,
}

public static class Moves
{
    /// <summary>Jogada aleatoria uniforme (pergunta aberta #4 do planejamento).</summary>
    public static Move Random() => (Move)System.Random.Shared.Next(3);

    /// <returns>1 se <paramref name="self"/> vence, -1 se perde, 0 se empata.</returns>
    public static int Beats(this Move self, Move other)
    {
        if (self == other)
        {
            return 0;
        }

        return self switch
        {
            Move.Rock => other == Move.Scissors ? 1 : -1,
            Move.Paper => other == Move.Rock ? 1 : -1,
            Move.Scissors => other == Move.Paper ? 1 : -1,
            _ => 0,
        };
    }
}
