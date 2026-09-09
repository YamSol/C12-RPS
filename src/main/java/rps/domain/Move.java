package rps.domain;

import java.util.concurrent.ThreadLocalRandom;

/** Jogada de RPS. Camada de dominio: sem thread, sem GUI. */
public enum Move {
    ROCK,
    PAPER,
    SCISSORS;

    private static final Move[] VALUES = values();

    /** Jogada aleatoria uniforme (pergunta aberta #4 do planejamento). */
    public static Move random() {
        return VALUES[ThreadLocalRandom.current().nextInt(VALUES.length)];
    }

    /**
     * @return 1 se esta jogada vence {@code other}, -1 se perde, 0 se empata.
     */
    public int beats(Move other) {
        if (this == other) {
            return 0;
        }
        return switch (this) {
            case ROCK -> other == SCISSORS ? 1 : -1;
            case PAPER -> other == ROCK ? 1 : -1;
            case SCISSORS -> other == PAPER ? 1 : -1;
        };
    }
}
