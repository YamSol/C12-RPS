package rps.ui;

import javafx.scene.paint.Color;

/**
 * RF07: cor e funcao deterministica do score. Gradiente tipo mapa de calor —
 * azul (0) -> ciano -> verde -> amarelo -> vermelho (maxScore).
 */
public final class ColorMapper {

    private static final double HUE_COLD = 240; // azul
    private static final double HUE_HOT = 0;    // vermelho

    private ColorMapper() {
    }

    public static Color scoreToColor(int score, int maxScore) {
        double t = maxScore <= 0 ? 0 : Math.min(1.0, (double) score / maxScore);
        double hue = HUE_COLD + (HUE_HOT - HUE_COLD) * t;
        return Color.hsb(hue, 0.75, 0.95);
    }

    public static String toCss(Color color) {
        return String.format("#%02X%02X%02X",
                (int) Math.round(color.getRed() * 255),
                (int) Math.round(color.getGreen() * 255),
                (int) Math.round(color.getBlue() * 255));
    }
}
