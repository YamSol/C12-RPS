using Avalonia;
using Avalonia.Controls.ApplicationLifetimes;
using Avalonia.Themes.Fluent;

using RpsPoc.Ui;

namespace RpsPoc;

/// <summary>
/// App montado em C# puro, sem .axaml. Nao e o jeito canonico do Avalonia
/// (o template usa XAML), mas mantem a PoC comparavel linha a linha com a
/// versao JavaFX, que tambem monta a UI em codigo.
/// </summary>
public sealed class App : Application
{
    public override void Initialize()
    {
        Styles.Add(new FluentTheme());
    }

    public override void OnFrameworkInitializationCompleted()
    {
        if (ApplicationLifetime is IClassicDesktopStyleApplicationLifetime desktop)
        {
            desktop.MainWindow = new MainWindow();
        }

        base.OnFrameworkInitializationCompleted();
    }
}
