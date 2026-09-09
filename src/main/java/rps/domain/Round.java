package rps.domain;

/** Uma rodada dentro de um match: o par de jogadas. */
public record Round(Move p1, Move p2) {

    public boolean isDraw() {
        return p1.beats(p2) == 0;
    }
}
