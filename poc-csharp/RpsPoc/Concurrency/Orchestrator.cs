using System.Collections.Concurrent;

using RpsPoc.Domain;

namespace RpsPoc.Concurrency;

/// <summary>
/// Loop proprio: tira duplas da fila e despacha. Nao joga nada.
/// <para>
/// O limite de T matches simultaneos vem do <see cref="SemaphoreSlim"/>, nao de
/// um pool de tamanho fixo: em .NET o pool e global, entao a forma de honrar o
/// RF02 e limitar quantas tasks concorrentes existem.
/// </para>
/// </summary>
public sealed class Orchestrator(
    PlayerQueue queue,
    EventBus bus,
    ConcurrentDictionary<int, Match> activeMatches,
    SemaphoreSlim slots,
    int roundDelayMs)
{
    private int _nextMatchId;

    public async Task RunAsync(CancellationToken cancellationToken)
    {
        try
        {
            while (!cancellationToken.IsCancellationRequested)
            {
                await slots.WaitAsync(cancellationToken).ConfigureAwait(false);

                // DequeuePair bloqueia (Monitor.Wait). Sai pro pool pra nao
                // prender a thread que carrega o orquestrador.
                var pair = await Task.Run(queue.DequeuePair, cancellationToken).ConfigureAwait(false);
                if (pair is null)
                {
                    slots.Release();
                    break; // RF09: sobrou 1, nenhum match ativo
                }

                var match = new Match(Interlocked.Increment(ref _nextMatchId), pair.Value.First, pair.Value.Second);
                activeMatches[match.Id] = match;
                bus.Publish(new QueueChanged(queue.Snapshot(), queue.Alive));

                _ = new MatchRunner(match, queue, bus, activeMatches, slots, roundDelayMs)
                    .RunAsync(cancellationToken);
            }
        }
        catch (OperationCanceledException)
        {
            // encerramento manual
        }
        finally
        {
            bus.Publish(new TournamentEnded(queue.Champion()));
        }
    }
}
