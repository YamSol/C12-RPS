package rps.domain;

/**
 * Partida entre dois jogadores. Escrito pela thread do MatchRunner, lido pela
 * UI thread -> campos mutaveis sao volatile.
 */
public final class Match {

    public enum State { PENDING, RUNNING, DONE }

    private final int id;
    private final Player p1;
    private final Player p2;

    private volatile State state = State.PENDING;
    private volatile MatchResult result;
    private volatile Round lastRound;

    public Match(int id, Player p1, Player p2) {
        this.id = id;
        this.p1 = p1;
        this.p2 = p2;
    }

    public int id() {
        return id;
    }

    public Player p1() {
        return p1;
    }

    public Player p2() {
        return p2;
    }

    public State state() {
        return state;
    }

    public void state(State state) {
        this.state = state;
    }

    public MatchResult result() {
        return result;
    }

    public void result(MatchResult result) {
        this.result = result;
    }

    public Round lastRound() {
        return lastRound;
    }

    public void lastRound(Round round) {
        this.lastRound = round;
    }
}
