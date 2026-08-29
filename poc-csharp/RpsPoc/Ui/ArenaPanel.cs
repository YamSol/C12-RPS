using Avalonia;
using Avalonia.Controls;

using RpsPoc.Domain;

namespace RpsPoc.Ui;

/// <summary>
/// RF11 / UC05: o espaco se redivide sozinho conforme o numero de matches ativos.
/// 1 -> tela cheia; 2 -> 50/50; N -> grade quadrada mais proxima, celulas iguais.
/// </summary>
public sealed class ArenaPanel : Grid
{
    private readonly Dictionary<int, MatchView> _views = new();

    public ArenaPanel()
    {
        Margin = new Thickness(10);
    }

    public void Add(Match match, int maxScore)
    {
        _views[match.Id] = new MatchView(match, maxScore);
        Relayout(maxScore);
    }

    public void Remove(Match match, int maxScore)
    {
        _views.Remove(match.Id);
        Relayout(maxScore);
    }

    /// <summary>Repinta todos (o score de alguem pode ter mudado).</summary>
    public void RefreshAll(int maxScore)
    {
        foreach (var view in _views.Values)
        {
            view.Refresh(maxScore);
        }
    }

    public MatchView? View(Match match) => _views.GetValueOrDefault(match.Id);

    private void Relayout(int maxScore)
    {
        Children.Clear();
        ColumnDefinitions.Clear();
        RowDefinitions.Clear();

        var n = _views.Count;
        if (n == 0)
        {
            return;
        }

        var cols = (int)Math.Ceiling(Math.Sqrt(n));
        var rows = (int)Math.Ceiling((double)n / cols);

        for (var c = 0; c < cols; c++)
        {
            ColumnDefinitions.Add(new ColumnDefinition(new GridLength(1, GridUnitType.Star)));
        }

        for (var r = 0; r < rows; r++)
        {
            RowDefinitions.Add(new RowDefinition(new GridLength(1, GridUnitType.Star)));
        }

        var i = 0;
        foreach (var view in _views.Values)
        {
            view.Refresh(maxScore);
            SetColumn(view, i % cols);
            SetRow(view, i / cols);
            Children.Add(view);
            i++;
        }
    }
}
