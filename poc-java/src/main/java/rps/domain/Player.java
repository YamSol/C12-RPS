package rps.domain;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Jogador. O score e mutavel e tocado por threads de match diferentes ao longo
 * do torneio (nunca por duas ao mesmo tempo, mas a leitura vem da UI thread),
 * dai o AtomicInteger.
 */
public final class Player {

    private final int id;
    private final AtomicInteger score = new AtomicInteger();

    public Player(int id) {
        this.id = id;
    }

    public int id() {
        return id;
    }

    public int score() {
        return score.get();
    }

    /** RF04: +1 ponto por vitoria. */
    public int win() {
        return score.incrementAndGet();
    }

    @Override
    public String toString() {
        return "P" + id + " (" + score() + ")";
    }
}
