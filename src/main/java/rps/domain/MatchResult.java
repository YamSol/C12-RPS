package rps.domain;

import java.util.List;

/** Resultado fechado de um match. {@code rounds} guarda o historico (util pra animar). */
public record MatchResult(Player winner, Player loser, List<Round> rounds) {

    public MatchResult {
        rounds = List.copyOf(rounds);
    }
}
