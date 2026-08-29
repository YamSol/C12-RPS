namespace RpsPoc.Concurrency;

/// <summary>
/// RF01/RF02: N e T configuraveis. RNF03: mudar T nao toca em nenhuma outra classe.
/// </summary>
/// <param name="Players">N — quantos jogadores entram no torneio.</param>
/// <param name="Threads">T — quantos matches podem rodar ao mesmo tempo.</param>
/// <param name="RoundDelayMs">
/// Duracao artificial de cada rodada, pra GUI ter o que mostrar
/// (pergunta aberta #2 do planejamento).
/// </param>
public sealed record TournamentConfig(int Players, int Threads, int RoundDelayMs)
{
    public static TournamentConfig Defaults() => new(60, 4, 350);
}
