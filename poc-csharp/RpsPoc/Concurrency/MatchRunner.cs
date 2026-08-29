using System.Collections.Concurrent;

using RpsPoc.Domain;

namespace RpsPoc.Concurrency;

/// <summary>Um match, do inicio ao fim, ocupando um dos T slots de concorrencia.</summary>
public sealed class MatchRunner(
    Match match,
    PlayerQueue queue,
    EventBus bus,
    ConcurrentDictionary<int, Match> activeMatches,
    SemaphoreSlim slots,
    int roundDelayMs)
{
    public async Task RunAsync(CancellationToken cancellationToken)
    {
        try
        {
            match.State = MatchState.Running;
            bus.Publish(new MatchStarted(match));

            var rounds = new List<Round>();
            Round round;
            do
            {
                await Task.Delay(roundDelayMs, cancellationToken).ConfigureAwait(false);
                round = new Round(Moves.Random(), Moves.Random());
                rounds.Add(round);
                match.LastRound = round;
                bus.Publish(new RoundPlayed(match, round));
            }
            while (round.IsDraw); // RF06: empate -> replay da mesma dupla

            var p1Won = round.P1.Beats(round.P2) > 0;
            var winner = p1Won ? match.P1 : match.P2;
            var loser = p1Won ? match.P2 : match.P1;

            var result = new MatchResult(winner, loser, rounds);
            match.Result = result;
            match.State = MatchState.Done;

            // Deixa o resultado visivel um instante antes de tirar o match da arena.
            await Task.Delay(roundDelayMs, cancellationToken).ConfigureAwait(false);

            var score = winner.Win();          // RF04
            queue.Eliminate(loser);            // RF05
            queue.Enqueue(winner);             // RF04
            activeMatches.TryRemove(match.Id, out _);

            bus.Publish(new MatchEnded(match, result));
            bus.Publish(new PlayerScored(winner, score));
            bus.Publish(new PlayerEliminated(loser));
            bus.Publish(new QueueChanged(queue.Snapshot(), queue.Alive));
        }
        catch (OperationCanceledException)
        {
            // encerramento manual
        }
        finally
        {
            slots.Release();
        }
    }
}
