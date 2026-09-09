package rps.concurrency;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

import rps.domain.Player;

/**
 * Fila única do torneio, thread-safe.
 *
 * waiting = jogadores esperando uma partida.
 * alivePlayers = todos os jogadores ainda vivos,
 * incluindo os que estão jogando.
 */
public final class PlayerQueue {

    private final Deque<Player> waiting = new ArrayDeque<>();

    // Guarda TODOS os jogadores ainda vivos
    private final Set<Player> alivePlayers = new LinkedHashSet<>();

    private final ReentrantLock lock = new ReentrantLock();
    private final Condition changed = lock.newCondition();

    private int alive;

    public PlayerQueue(Collection<Player> players) {

        waiting.addAll(players);
        alivePlayers.addAll(players);

        alive = players.size();
    }

    /** O vencedor volta para o fim da fila. */
    public void enqueue(Player player) {
        lock.lock();

        try {
            waiting.addLast(player);
            changed.signalAll();

        } finally {
            lock.unlock();
        }
    }

    /** O perdedor é eliminado do torneio. */
    public void eliminate(Player player) {
        lock.lock();

        try {

            // Remove da lista de jogadores vivos
            if (alivePlayers.remove(player)) {
                alive--;
            }

            changed.signalAll();

        } finally {
            lock.unlock();
        }
    }

    /**
     * Espera até existirem dois jogadores disponíveis
     * e remove os dois da fila.
     */
    public Player[] dequeuePair() throws InterruptedException {
        lock.lock();

        try {

            while (waiting.size() < 2) {

                if (alive <= 1) {
                    return null;
                }

                changed.await();
            }

            return new Player[]{
                    waiting.pollFirst(),
                    waiting.pollFirst()
            };

        } finally {
            lock.unlock();
        }
    }

    /** Quantos estão esperando uma partida. */
    public int size() {
        lock.lock();

        try {
            return waiting.size();

        } finally {
            lock.unlock();
        }
    }

    /** Quantos ainda estão vivos no torneio. */
    public int alive() {
        lock.lock();

        try {
            return alive;

        } finally {
            lock.unlock();
        }
    }

    /** Jogadores que estão esperando para jogar. */
    public List<Player> snapshot() {
        lock.lock();

        try {
            return new ArrayList<>(waiting);

        } finally {
            lock.unlock();
        }
    }

    /**
     * TODOS os jogadores ainda vivos,
     * inclusive os que estão jogando.
     */
    public List<Player> aliveSnapshot() {
        lock.lock();

        try {
            return new ArrayList<>(alivePlayers);

        } finally {
            lock.unlock();
        }
    }

    /** Retorna o último sobrevivente. */
    public Player champion() {
        lock.lock();

        try {

            if (alivePlayers.size() == 1) {
                return alivePlayers.iterator().next();
            }

            return null;

        } finally {
            lock.unlock();
        }
    }
}