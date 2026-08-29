using Avalonia.Media;

namespace RpsPoc.Ui;

/// <summary>
/// RF07: cor e funcao deterministica do score. Gradiente tipo mapa de calor —
/// azul (0) -> ciano -> verde -> amarelo -> vermelho (maxScore).
/// </summary>
public static class ColorMapper
{
    private const double HueCold = 240; // azul
    private const double HueHot = 0;    // vermelho

    public static Color ScoreToColor(int score, int maxScore)
    {
        var t = maxScore <= 0 ? 0 : Math.Min(1.0, (double)score / maxScore);
        var hue = HueCold + ((HueHot - HueCold) * t);
        return FromHsv(hue, 0.75, 0.95);
    }

    public static IBrush ScoreToBrush(int score, int maxScore) =>
        new SolidColorBrush(ScoreToColor(score, maxScore));

    private static Color FromHsv(double hue, double saturation, double value)
    {
        var c = value * saturation;
        var h = hue / 60.0;
        var x = c * (1 - Math.Abs((h % 2) - 1));
        var m = value - c;

        var (r, g, b) = (int)h switch
        {
            0 => (c, x, 0.0),
            1 => (x, c, 0.0),
            2 => (0.0, c, x),
            3 => (0.0, x, c),
            4 => (x, 0.0, c),
            _ => (c, 0.0, x),
        };

        return Color.FromRgb(Channel(r + m), Channel(g + m), Channel(b + m));
    }

    private static byte Channel(double v) => (byte)Math.Clamp(Math.Round(v * 255), 0, 255);
}
