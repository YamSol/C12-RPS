using RpsPoc.Concurrency;

namespace RpsPoc;

/// <summary>
/// Roda o torneio sem GUI, so pra provar que as camadas de dominio e
/// concorrencia terminam sozinhas (RF09) e nao travam:
/// <code>dotnet run -- --headless 60 4 0</code>
/// </summary>
internal static class Headless
{
    public static int Run(string[] args)
    {
        var players = args.Length > 0 ? int.Parse(args[0]) : 60;
        var threads = args.Length > 1 ? int.Parse(args[1]) : 4;
        var delay = args.Length > 2 ? int.Parse(args[2]) : 0;

        using var done = new ManualResetEventSlim(false);

        // Sem UI thread: o "dispatcher de UI" e o proprio chamador.
        var bus = new EventBus(action => action());

        bus.Subscribe<MatchEnded>(e =>
            Console.WriteLine($"match #{e.Match.Id} -> {e.Result.Winner} ({e.Result.Rounds.Count} rodada(s))"));
        bus.Subscribe<TournamentEnded>(e =>
        {
            Console.WriteLine($"campeao: {e.Champion?.ToString() ?? "(nenhum)"}");
            done.Set();
        });

        var tournament = new Tournament(new TournamentConfig(players, threads, delay), bus);
        _ = tournament.Start();

        if (!done.Wait(TimeSpan.FromSeconds(60)))
        {
            Console.Error.WriteLine("TIMEOUT: torneio nao terminou — provavel deadlock");
            tournament.Stop();
            return 1;
        }

        Console.WriteLine($"terminou com alive={tournament.Queue.Alive} activeMatches={tournament.ActiveCount}");
        return 0;
    }
}
