using System.Collections.Concurrent;

namespace RpsPoc.Concurrency;

/// <summary>
/// Observer entre a camada de concorrencia e a de apresentacao.
/// <para>
/// Tasks de match publicam de qualquer thread; o dispatch e marshalado pro
/// <c>uiDispatch</c> (<c>Dispatcher.UIThread.Post</c> no Avalonia), entao o
/// handler sempre roda na UI thread. E isso que mantem RNF01 de pe.
/// </para>
/// </summary>
public sealed class EventBus(Action<Action> uiDispatch)
{
    private readonly ConcurrentDictionary<Type, List<Delegate>> _handlers = new();

    public void Subscribe<TEvent>(Action<TEvent> handler)
    {
        var list = _handlers.GetOrAdd(typeof(TEvent), _ => []);
        lock (list)
        {
            list.Add(handler);
        }
    }

    public void Publish<TEvent>(TEvent evt)
        where TEvent : notnull
    {
        if (!_handlers.TryGetValue(typeof(TEvent), out var list))
        {
            return;
        }

        Delegate[] snapshot;
        lock (list)
        {
            snapshot = list.ToArray();
        }

        if (snapshot.Length == 0)
        {
            return;
        }

        uiDispatch(() =>
        {
            foreach (var handler in snapshot)
            {
                ((Action<TEvent>)handler)(evt);
            }
        });
    }
}
