package rps.ui;

import javafx.scene.paint.Color;

/**
 * RF07: a cor de um jogador e funcao deterministica do score, numa escala
 * <b>discreta e fixada no inicio do torneio</b> (ADR-012).
 *
 * <p>Um nivel por ponto de score — score 0 usa o nivel 0, score 1 o nivel 1, e
 * assim por diante. Quem passa do topo satura no ultimo nivel, entao nunca
 * falta cor. Como o numero de niveis sai de N e nao do score corrente, a cor de
 * um score <b>nao muda</b> enquanto o torneio roda: e isso que faz a cor
 * significar "quao forte e este jogador" em vez de "quao forte ele e comparado
 * ao lider de agora".
 */
public final class ColorScale {

    /**
     * Margem sobre {@code ceil(log2(N))}. Medido em 40 execucoes por N: o score
     * maximo real estoura o teto teorico em 2-7% das vezes, sempre por +1 (a
     * fila nao e perfeitamente sincrona, entao alguem joga mais que o esperado).
     * +2 cobre isso com folga; acima disso o clamp resolve.
     */
    private static final int MARGIN = 2;

    private static final int MIN_LEVELS = 3;
    private static final int MAX_LEVELS = 12;

    /**
     * Ancoras do Viridis. Rampa sequencial monotonica em luminancia: funciona em
     * deuteranopia/protanopia, e o "mais escuro -> mais claro" sobrevive ate
     * numa impressao em preto e branco. Nao trocar por arco-iris (matizes de
     * luminancia desigual invertem a hierarquia visual).
     */
    private static final Color[] RAMP = {
            Color.web("#440154"),
            Color.web("#414487"),
            Color.web("#2A788E"),
            Color.web("#22A884"),
            Color.web("#7AD151"),
            Color.web("#FDE725")
    };

    private static final Color TEXT_ON_LIGHT = Color.web("#101318");
    private static final Color TEXT_ON_DARK = Color.web("#F2F4F8");

    private final Color[] levels;

    private ColorScale(Color[] levels) {
        this.levels = levels;
    }

    /** UC01: chamada uma vez no start, com o N configurado. */
    public static ColorScale forPlayers(int players) {
        int count = levelsFor(players);
        Color[] cores = new Color[count];
        for (int i = 0; i < count; i++) {
            cores[i] = sample(i / (double) (count - 1));
        }
        return new ColorScale(cores);
    }

    /**
     * {@code ceil(log2(players)) + MARGIN}, preso em [{@value #MIN_LEVELS},
     * {@value #MAX_LEVELS}].
     *
     * <p>O teto sai de aritmetica inteira, nao de {@code Math.log(n)/Math.log(2)}:
     * em potencia de 2 o ponto flutuante erra pra cima ou pra baixo por epsilon e
     * o {@code ceil} devolve um nivel a mais ou a menos.
     */
    static int levelsFor(int players) {
        int teto = players < 2 ? 1 : 32 - Integer.numberOfLeadingZeros(players - 1);
        return Math.max(MIN_LEVELS, Math.min(MAX_LEVELS, teto + MARGIN));
    }

    /** Quantos niveis esta escala tem. */
    public int levels() {
        return levels.length;
    }

    /** RF07. Score acima do topo satura no ultimo nivel. */
    public Color colorFor(int score) {
        return levels[clamp(score)];
    }

    /**
     * Cor de texto legivel sobre {@link #colorFor(int)}. A rampa vai de um roxo
     * quase preto a um amarelo quase branco, entao uma cor fixa seria ilegivel
     * em uma das pontas.
     */
    public Color textOn(int score) {
        Color fundo = colorFor(score);
        double luminancia = 0.2126 * fundo.getRed()
                + 0.7152 * fundo.getGreen()
                + 0.0722 * fundo.getBlue();
        return luminancia > 0.5 ? TEXT_ON_LIGHT : TEXT_ON_DARK;
    }

    /** Rotulo do nivel na legenda (#15): o ultimo e saturacao, nao score exato. */
    public String labelFor(int level) {
        int nivel = clamp(level);
        return nivel == levels.length - 1 ? nivel + "+" : String.valueOf(nivel);
    }

    private int clamp(int score) {
        return Math.max(0, Math.min(score, levels.length - 1));
    }

    /** Amostra a rampa em t entre 0 e 1, interpolando entre duas ancoras. */
    private static Color sample(double t) {
        double pos = Math.max(0, Math.min(1, t)) * (RAMP.length - 1);
        int i = (int) Math.floor(pos);
        if (i >= RAMP.length - 1) {
            return RAMP[RAMP.length - 1];
        }
        return RAMP[i].interpolate(RAMP[i + 1], pos - i);
    }
}
