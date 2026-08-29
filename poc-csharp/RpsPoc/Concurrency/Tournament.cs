using System.Collections.Concurrent;

using RpsPoc.Domain;

namespace RpsPoc.Concurrency;

/// <summary>Dono do estado compartilhado e do ciclo de vida das tasks.</summary>
public sealed class Tournament
{
    private readonly TournamentConfig _config;
    private readonly EventBus _bus;
    private readonly ConcurrentDictionary<int, Match> _activeMatches = new();
    private readonly SemaphoreSlim _slots;

    private CancellationTokenSource? _cts;

    public Tournament(TournamentConfig config, EventBus bus)
    {
        _config = config;
        _bus = bus;
        _slots = new SemaphoreSlim(config.Threads, config.Threads);

        var players = new List<Player>(config.Players);
        for (var i = 1; i <= config.Players; i++)
        {
            players.Add(new Player(i));
        }

        Players = players;
        Queue = new PlayerQueue(players);
    }

    public PlayerQueue Queue { get; }

    public IReadOnlyList<Player> Players { get; }

    public IEnumerable<Match> ActiveMatches => _activeMatches.Values;

    public int ActiveCount => _activeMatches.Count;

    /// <summary>RF09.</summary>
    public bool IsFinished => Queue.Alive <= 1 && _activeMatches.IsEmpty;

    public Player? Champion => Queue.Champion();

    /// <summary>UC01. Retorna na hora: tudo acontece fora da UI thread (RNF01).</summary>
    public Task Start()
    {
        _cts = new CancellationTokenSource();
        _bus.Publish(new QueueChanged(Queue.Snapshot(), Queue.Alive));

        var orchestrator = new Orchestrator(Queue, _bus, _activeMatches, _slots, _config.RoundDelayMs);
        return Task.Run(() => orchestrator.RunAsync(_cts.Token), CancellationToken.None);
    }

    public void Stop()
    {
        Queue.Shutdown();
        _cts?.Cancel();
    }
}
