using RpsPoc.Domain;

namespace RpsPoc.Concurrency;

/// <summary>
/// Fila unica do torneio, thread-safe (RNF02).
/// <para>
/// Alem da fila, guarda <c>_alive</c> — quantos jogadores ainda nao foram
/// eliminados, contando os que estao dentro de um match. E esse contador que
/// decide o fim do torneio (RF09): <c>alive == 1</c>.
/// </para>
/// <para>
/// Escolha de projeto: <c>lock</c> + <c>Monitor.Wait/PulseAll</c> em vez de
/// <c>ConcurrentQueue</c> ou <c>Channel&lt;T&gt;</c>. A primitiva que o RF03 pede
/// e "tire DOIS" atomicamente, que nenhuma das duas oferece; e a espera precisa
/// acordar tambem quando o torneio acaba, nao so quando chega elemento.
/// </para>
/// </summary>
public sealed class PlayerQueue
{
    private readonly Queue<Player> _waiting = new();
    private readonly object _gate = new();

    private int _alive;
    private bool _stopped;

    public PlayerQueue(IEnumerable<Player> players)
    {
        foreach (var player in players)
        {
            _waiting.Enqueue(player);
        }

        _alive = _waiting.Count;
    }

    public int Count
    {
        get
        {
            lock (_gate)
            {
                return _waiting.Count;
            }
        }
    }

    public int Alive
    {
        get
        {
            lock (_gate)
            {
                return _alive;
            }
        }
    }

    /// <summary>RF04: winner volta pro fim da fila.</summary>
    public void Enqueue(Player player)
    {
        lock (_gate)
        {
            _waiting.Enqueue(player);
            Monitor.PulseAll(_gate);
        }
    }

    /// <summary>RF05: loser sai do universo — nao volta pra fila e some do contador.</summary>
    public void Eliminate(Player player)
    {
        _ = player;
        lock (_gate)
        {
            _alive--;
            Monitor.PulseAll(_gate);
        }
    }

    /// <summary>
    /// RF03: bloqueia ate haver 2 jogadores disponiveis.
    /// Devolve <c>null</c> quando o torneio acabou (RF09) ou foi parado —
    /// nesse caso o orquestrador encerra o loop.
    /// </summary>
    public (Player First, Player Second)? DequeuePair()
    {
        lock (_gate)
        {
            while (_waiting.Count < 2)
            {
                if (_stopped || _alive <= 1)
                {
                    return null;
                }

                Monitor.Wait(_gate);
            }

            return (_waiting.Dequeue(), _waiting.Dequeue());
        }
    }

    /// <summary>Acorda quem estiver esperando, para o encerramento manual.</summary>
    public void Shutdown()
    {
        lock (_gate)
        {
            _stopped = true;
            Monitor.PulseAll(_gate);
        }
    }

    /// <summary>Copia defensiva pra GUI ler sem segurar o lock enquanto desenha (RNF01).</summary>
    public IReadOnlyList<Player> Snapshot()
    {
        lock (_gate)
        {
            return _waiting.ToArray();
        }
    }

    /// <summary>O ultimo sobrevivente, quando houver um.</summary>
    public Player? Champion()
    {
        lock (_gate)
        {
            return _alive == 1 && _waiting.Count == 1 ? _waiting.Peek() : null;
        }
    }
}
