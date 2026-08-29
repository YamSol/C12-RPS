using Avalonia;
using Avalonia.Controls;
using Avalonia.Layout;
using Avalonia.Media;

using RpsPoc.Domain;

namespace RpsPoc.Ui;

/// <summary>RF10: a fila viva. Redesenhada a cada QueueChanged.</summary>
public sealed class QueuePanel : StackPanel
{
    private readonly TextBlock _header = new() { Text = "fila", FontSize = 14, Foreground = Brushes.Gainsboro };
    private readonly WrapPanel _chips = new();

    public QueuePanel()
    {
        Width = 250;
        Spacing = 6;
        Margin = new Thickness(10);
        Children.Add(_header);
        Children.Add(_chips);
    }

    public void Update(IReadOnlyList<Player> waiting, int alive, int maxScore)
    {
        _header.Text = $"fila: {waiting.Count}   vivos: {alive}";
        _chips.Children.Clear();
        foreach (var player in waiting)
        {
            _chips.Children.Add(Chip(player, maxScore));
        }
    }

    private static Border Chip(Player player, int maxScore) => new()
    {
        Width = 28,
        Height = 28,
        Margin = new Thickness(2),
        CornerRadius = new CornerRadius(14),
        Background = ColorMapper.ScoreToBrush(player.Score, maxScore),
        Child = new TextBlock
        {
            Text = player.Id.ToString(),
            FontSize = 11,
            Foreground = Brushes.Black,
            HorizontalAlignment = HorizontalAlignment.Center,
            VerticalAlignment = VerticalAlignment.Center,
        },
    };
}
