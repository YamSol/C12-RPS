package rps.concurrency;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;

import rps.domain.Match;
import rps.domain.Player;

/** Dono do estado compartilhado e do ciclo de vida das threads. */
public final class Tournament {

    private final TournamentConfig config;
    private final EventBus bus;
    private final PlayerQueue queue;
    private final List<Player> players;
    private final Set<Match> activeMatches = ConcurrentHashMap.newKeySet();

    private ExecutorService pool;
    private Thread orchestratorThread;

    public Tournament(TournamentConfig config, EventBus bus) {
        this.config = config;
        this.bus = bus;
        this.players = new ArrayList<>(config.players());
        for (int i = 1; i <= config.players(); i++) {
            players.add(new Player(i));
        }
        this.queue = new PlayerQueue(players);
    }

    /** UC01. Retorna imediatamente: tudo acontece fora da UI thread (RNF01). */
    public void start() {
        pool = Executors.newFixedThreadPool(config.threads(), runnable -> {
            Thread thread = new Thread(runnable, "match-worker");
            thread.setDaemon(true);
            return thread;
        });
        bus.publish(new Events.QueueChanged(queue.snapshot(), queue.alive()));

        // RF02: T vagas de partida simultanea. E o semaforo — nao o tamanho do
        // pool — que segura o orquestrador, mantendo na fila todo mundo que
        // ainda nao tem thread pra jogar. Em .NET o equivalente e o
        // SemaphoreSlim, entao as duas stacks passam a expressar T do mesmo jeito.
        Semaphore slots = new Semaphore(config.threads());

        // RF12: o relogio do torneio comeca na construcao do TournamentStats,
        // entao ele nasce aqui — o mais perto possivel da largada.
        TournamentStats stats = new TournamentStats();

        orchestratorThread = new Thread(
                new Orchestrator(queue, pool, bus, activeMatches, config.roundDelayMs(), slots, stats),
                "orchestrator");
        orchestratorThread.setDaemon(true);
        orchestratorThread.start();
    }

    public void stop() {
        if (orchestratorThread != null) {
            orchestratorThread.interrupt();
        }
        if (pool != null) {
            pool.shutdownNow();
        }
    }

    /** RF09. */
    public boolean isFinished() {
        return queue.alive() <= 1 && activeMatches.isEmpty();
    }

    public Player champion() {
        return queue.champion();
    }

    public PlayerQueue queue() {
        return queue;
    }

    public Set<Match> activeMatches() {
        return activeMatches;
    }

    public List<Player> players() {
        return players;
    }

    public TournamentConfig config() {
        return config;
    }
}
