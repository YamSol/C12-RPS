using Avalonia;
using Avalonia.Controls;
using Avalonia.Layout;
using Avalonia.Media;

using RpsPoc.Domain;

namespace RpsPoc.Ui;

/// <summary>
/// Um match desenhado: dois retangulos coloridos pelo score de cada jogador,
/// com a jogada corrente no meio. So le estado, nao decide nada.
/// </summary>
public sealed class MatchView : Grid
{
    private readonly Border _left = new() { CornerRadius = new CornerRadius(6) };
    private readonly Border _right = new() { CornerRadius = new CornerRadius(6) };
    private readonly TextBlock _leftMove = new() { Text = "...", FontSize = 11 };
    private readonly TextBlock _rightMove = new() { Text = "...", FontSize = 11 };
    private readonly TextBlock _title = new() { FontSize = 12, Foreground = Brushes.Gainsboro };

    public MatchView(Match match, int maxScore)
    {
        Match = match;
        Margin = new Thickness(4);
        RowDefinitions = new RowDefinitions("Auto,*");

        _title.Text = $"match #{match.Id}";
        SetRow(_title, 0);
        Children.Add(_title);

        var arena = new Grid { ColumnDefinitions = new ColumnDefinitions("*,*") };
        Fill(_left, match.P1, _leftMove);
        Fill(_right, match.P2, _rightMove);
        SetColumn(_left, 0);
        SetColumn(_right, 1);
        arena.Children.Add(_left);
        arena.Children.Add(_right);

        SetRow(arena, 1);
        Children.Add(arena);

        Refresh(maxScore);
    }

    public Match Match { get; }

    /// <summary>Repinta a partir do estado atual do match. Sempre na UI thread.</summary>
    public void Refresh(int maxScore)
    {
        _left.Background = ColorMapper.ScoreToBrush(Match.P1.Score, maxScore);
        _right.Background = ColorMapper.ScoreToBrush(Match.P2.Score, maxScore);

        var round = Match.LastRound;
        _leftMove.Text = round is null ? "..." : round.P1.ToString();
        _rightMove.Text = round is null ? "..." : round.P2.ToString();

        if (round is null)
        {
            return;
        }

        if (round.IsDraw)
        {
            _title.Text = $"match #{Match.Id} — empate, replay";
            _left.Opacity = 1;
            _right.Opacity = 1;
            return;
        }

        // Rodada decisiva: apaga o perdedor. O runner ainda segura o match na
        // arena por um instante antes de publicar MatchEnded.
        var p1Won = round.P1.Beats(round.P2) > 0;
        _title.Text = $"match #{Match.Id} — vence {(p1Won ? Match.P1 : Match.P2)}";
        (p1Won ? _right : _left).Opacity = 0.25;
    }

    private static void Fill(Border border, Player player, TextBlock moveText)
    {
        border.Margin = new Thickness(3);
        border.Child = new StackPanel
        {
            HorizontalAlignment = HorizontalAlignment.Center,
            VerticalAlignment = VerticalAlignment.Center,
            Spacing = 2,
            Children =
            {
                new TextBlock
                {
                    Text = player.ToString(),
                    FontSize = 13,
                    Foreground = Brushes.Black,
                    HorizontalAlignment = HorizontalAlignment.Center,
                },
                moveText,
            },
        };
        moveText.Foreground = Brushes.Black;
        moveText.HorizontalAlignment = HorizontalAlignment.Center;
    }
}
