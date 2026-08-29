package rps.concurrency;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executor;
import java.util.function.Consumer;

/**
 * Observer entre a camada de concorrencia e a de apresentacao.
 *
 * <p>Threads de match publicam de qualquer thread; o dispatch e marshalado pro
 * {@code uiExecutor} (Platform::runLater no JavaFX), entao o handler sempre roda
 * na UI thread. E isso que mantem RNF01 de pe: a GUI nunca toca no estado
 * compartilhado sob lock, so recebe eventos ja prontos.
 */
public final class EventBus {

    private final Map<Class<?>, List<Consumer<Object>>> handlers = new ConcurrentHashMap<>();
    private final Executor uiExecutor;

    public EventBus(Executor uiExecutor) {
        this.uiExecutor = uiExecutor;
    }

    @SuppressWarnings("unchecked")
    public <E> void subscribe(Class<E> type, Consumer<E> handler) {
        handlers.computeIfAbsent(type, key -> new CopyOnWriteArrayList<>())
                .add((Consumer<Object>) handler);
    }

    public void publish(Object event) {
        List<Consumer<Object>> subscribers = handlers.get(event.getClass());
        if (subscribers == null || subscribers.isEmpty()) {
            return;
        }
        uiExecutor.execute(() -> {
            for (Consumer<Object> subscriber : subscribers) {
                subscriber.accept(event);
            }
        });
    }
}
