package rps.concurrency;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

import rps.domain.Player;

/**
 * Fila unica do torneio, thread-safe (RNF02).
 *
 * <p>Alem da fila em si, guarda {@code alive} — quantos jogadores ainda nao
 * foram eliminados, contando os que estao dentro de um match. E esse contador
 * que decide o fim do torneio (RF09): {@code alive == 1}.
 *
 * <p>Escolha de projeto: ReentrantLock + Condition em vez de BlockingQueue,
 * porque a operacao primitiva aqui e "tire DOIS" (RF03) e nao "tire um" —
 * e porque a espera precisa ser interrompida quando o torneio acaba.
 */
public final class PlayerQueue {

    private final Deque<Player> waiting = new ArrayDeque<>();
    private final ReentrantLock lock = new ReentrantLock();
    private final Condition changed = lock.newCondition();

    private int alive;

    public PlayerQueue(Collection<Player> players) {
        waiting.addAll(players);
        alive = players.size();
    }

    /**
     * RF04 + RF05 numa transacao so: o perdedor sai do contador e o vencedor
     * volta pro fim da fila sem que o lock seja solto no meio.
     *
     * <p>Em dois passos separados havia uma janela em que {@code alive == 1}
     * com o vencedor ainda fora da fila. O orquestrador que acordasse ali via
     * {@code dequeuePair() == null}, encerrava o loop e publicava o fim do
     * torneio com {@link #champion()} nulo — o campeao sumia da tela. Raro
     * (~1 em 2000 com N=8, T=4), mas reproduzivel.
     */
    public void finishMatch(Player winner, Player loser) {
        lock.lock();
        try {
            alive--;                    // RF05: o perdedor sai do universo
            waiting.addLast(winner);    // RF04: o vencedor volta pro fim da fila
            changed.signalAll();
        } finally {
            lock.unlock();
        }
    }

    /**
     * RF03: bloqueia ate haver 2 jogadores disponiveis.
     *
     * @return o par, ou {@code null} quando o torneio acabou (RF09) — nesse caso
     *         o orchestrator deve encerrar seu loop.
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
            return new Player[] { waiting.pollFirst(), waiting.pollFirst() };
        } finally {
            lock.unlock();
        }
    }

    public int size() {
        lock.lock();
        try {
            return waiting.size();
        } finally {
            lock.unlock();
        }
    }

    public int alive() {
        lock.lock();
        try {
            return alive;
        } finally {
            lock.unlock();
        }
    }

    /** Copia defensiva pra GUI ler sem segurar o lock enquanto desenha (RNF01). */
    public List<Player> snapshot() {
        lock.lock();
        try {
            return new ArrayList<>(waiting);
        } finally {
            lock.unlock();
        }
    }

    /** O ultimo sobrevivente, quando houver um. */
    public Player champion() {
        lock.lock();
        try {
            return alive == 1 && waiting.size() == 1 ? waiting.peekFirst() : null;
        } finally {
            lock.unlock();
        }
    }
}
