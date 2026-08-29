using Avalonia;
using Avalonia.Controls;
using Avalonia.Layout;
using Avalonia.Media;
using Avalonia.Threading;

using RpsPoc.Concurrency;

namespace RpsPoc.Ui;

/// <summary>
/// Camada de apresentacao. So subscreve eventos e desenha — nenhuma regra de
/// jogo, nenhum lock, nenhuma espera (RNF01).
/// </summary>
public sealed class MainWindow : Window
{
    private readonly QueuePanel _queuePanel = new();
    private readonly ArenaPanel _arenaPanel = new();
    private readonly TextBlock _status = new() { Text = "pronto", Foreground = Brushes.Gainsboro };
    private readonly NumericUpDown _players = new() { Minimum = 2, Maximum = 500, Value = 60, Width = 110 };
    private readonly NumericUpDown _threads = new() { Minimum = 1, Maximum = 64, Value = 4, Width = 100 };
    private readonly Button _start = new() { Content = "start" };

    private Tournament? _tournament;
    private int _maxScore = 1;

    public MainWindow()
    {
        Title = "RPS concorrente — PoC C# + Avalonia";
        Width = 1100;
        Height = 700;
        Background = new SolidColorBrush(Color.FromRgb(0x0E, 0x11, 0x16));

        _start.Click += (_, _) => StartTournament();

        var controls = new StackPanel
        {
            Orientation = Orientation.Horizontal,
            Spacing = 8,
            Margin = new Thickness(10),
            Children =
            {
                Label("N jogadores"), _players,
                Label("T threads"), _threads,
                _start,
            },
        };

        var statusBar = new Border
        {
            Padding = new Thickness(10, 8),
            Child = _status,
        };

        var root = new DockPanel();
        DockPanel.SetDock(controls, Dock.Top);
        DockPanel.SetDock(statusBar, Dock.Bottom);
        DockPanel.SetDock(_queuePanel, Dock.Left);
        root.Children.Add(controls);
        root.Children.Add(statusBar);
        root.Children.Add(_queuePanel);
        root.Children.Add(_arenaPanel); // preenche o resto

        Content = root;
        Closing += (_, _) => _tournament?.Stop();
    }

    private static TextBlock Label(string text) => new()
    {
        Text = text,
        Foreground = Brushes.Gainsboro,
        VerticalAlignment = VerticalAlignment.Center,
    };

    /// <summary>UC01.</summary>
    private void StartTournament()
    {
        _tournament?.Stop();
        _maxScore = 1;

        // Dispatcher.UIThread.Post e o unico ponto de acoplamento com Avalonia
        // na camada de concorrencia — trocar de GUI troca so este delegate.
        var bus = new EventBus(action => Dispatcher.UIThread.Post(action));
        Wire(bus);

        var config = new TournamentConfig((int)(_players.Value ?? 60), (int)(_threads.Value ?? 4), 350);
        _tournament = new Tournament(config, bus);

        _start.IsEnabled = false;
        _status.Text = $"torneio rodando — N={config.Players} T={config.Threads}";
        _ = _tournament.Start();
    }

    private void Wire(EventBus bus)
    {
        bus.Subscribe<QueueChanged>(e =>
        {
            _queuePanel.Update(e.Waiting, e.Alive, _maxScore);
            _arenaPanel.RefreshAll(_maxScore);
        });
        bus.Subscribe<MatchStarted>(e => _arenaPanel.Add(e.Match, _maxScore));
        bus.Subscribe<RoundPlayed>(e => _arenaPanel.View(e.Match)?.Refresh(_maxScore));
        bus.Subscribe<MatchEnded>(e => _arenaPanel.Remove(e.Match, _maxScore));
        bus.Subscribe<PlayerScored>(e =>
        {
            if (e.Score > _maxScore)
            {
                _maxScore = e.Score; // renormaliza o gradiente (RF07)
            }
        });
        bus.Subscribe<TournamentEnded>(e => // UC06
        {
            _start.IsEnabled = true;
            _status.Text = e.Champion is null ? "torneio encerrado" : $"campeao: {e.Champion}";
        });
    }
}
