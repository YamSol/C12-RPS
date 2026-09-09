package rps.ui;

import java.util.Locale;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TextFormatter;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.stage.Stage;

import rps.concurrency.EventBus;
import rps.concurrency.Events;
import rps.concurrency.Tournament;
import rps.concurrency.TournamentConfig;
import rps.concurrency.TournamentMetrics;
import rps.domain.Player;

/**
 * Camada de apresentacao. So subscreve eventos e desenha — nenhuma regra de
 * jogo, nenhum lock, nenhuma espera (RNF01).
 */
public final class MainApp extends Application {

    private final QueuePanel queuePanel = new QueuePanel();
    private final ArenaPanel arenaPanel = new ArenaPanel();
    private final ResultPanel resultPanel = new ResultPanel();
    private final ScaleLegend legend = new ScaleLegend();
    private final Label status = new Label("pronto");
    private final Spinner<Integer> playersSpinner = editableSpinner(
            TournamentConfig.MIN_PLAYERS, TournamentConfig.MAX_PLAYERS, 60);
    private final Spinner<Integer> threadsSpinner = editableSpinner(
            TournamentConfig.MIN_THREADS, TournamentConfig.MAX_THREADS, 4);
    private final Button startButton = new Button("start");

    /** Guardado porque o centro alterna entre arena e resultado (UC06). */
    private final BorderPane root = new BorderPane();

    private Tournament tournament;

    @Override
    public void start(Stage stage) {
        root.setTop(controls());
        root.setLeft(leftColumn());
        root.setCenter(arenaPanel);
        root.setBottom(statusBar());
        root.setBackground(new Background(new BackgroundFill(Color.web("#0e1116"), CornerRadii.EMPTY, Insets.EMPTY)));

        startButton.setOnAction(event -> startTournament());

        stage.setTitle("RPS concorrente — PoC Java + JavaFX");
        stage.setScene(new Scene(root, 1100, 700));
        stage.setOnCloseRequest(event -> {
            if (tournament != null) {
                tournament.stop();
            }
        });
        stage.show();
    }

    /** Fila em cima, legenda da escala (#15) embaixo — a fila cresce, a legenda nao. */
    private VBox leftColumn() {
        VBox box = new VBox(queuePanel, legend);
        VBox.setVgrow(queuePanel, Priority.ALWAYS);
        return box;
    }

    private HBox controls() {
        playersSpinner.setPrefWidth(90);
        threadsSpinner.setPrefWidth(70);
        HBox box = new HBox(8,
                label("N jogadores"), playersSpinner,
                label("T threads"), threadsSpinner,
                startButton);
        box.setAlignment(Pos.CENTER_LEFT);
        box.setPadding(new Insets(10));
        box.setBackground(new Background(new BackgroundFill(Color.web("#161a21"), CornerRadii.EMPTY, Insets.EMPTY)));
        return box;
    }

    private HBox statusBar() {
        status.setTextFill(Color.web("#cfd3dc"));
        status.setFont(Font.font(13));
        HBox box = new HBox(status);
        box.setPadding(new Insets(8, 10, 8, 10));
        box.setBackground(new Background(new BackgroundFill(Color.web("#161a21"), CornerRadii.EMPTY, Insets.EMPTY)));
        return box;
    }

    private Label label(String text) {
        Label label = new Label(text);
        label.setTextFill(Color.web("#cfd3dc"));
        return label;
    }

    /**
     * Spinner que aceita o valor digitado (#4). Tres peças, todas necessarias:
     * setEditable libera o teclado, o TextFormatter barra o que nao e digito
     * antes de virar texto, e o listener de foco comita — sozinho, o Spinner do
     * JavaFX so comita no Enter e descarta o resto em silencio.
     */
    private static Spinner<Integer> editableSpinner(int min, int max, int initial) {
        Spinner<Integer> spinner = new Spinner<>(min, max, initial);
        spinner.setEditable(true);

        int maxDigits = Integer.toString(max).length();
        spinner.getEditor().setTextFormatter(new TextFormatter<>(change ->
                change.getControlNewText().matches("\\d{0," + maxDigits + "}") ? change : null));

        spinner.getEditor().focusedProperty().addListener((observable, had, has) -> {
            if (!has) {
                commitEditor(spinner);
            }
        });
        return spinner;
    }

    /**
     * Le o texto do editor e devolve o valor efetivo, clampado na faixa. Vazio
     * (o filtro ja garante que o resto e numerico) volta pro ultimo valor valido.
     * Nunca lanca: o TournamentConfig rejeita valor fora da faixa, entao e aqui
     * que a entrada tem que ficar boa.
     */
    private static void commitEditor(Spinner<Integer> spinner) {
        SpinnerValueFactory.IntegerSpinnerValueFactory factory =
                (SpinnerValueFactory.IntegerSpinnerValueFactory) spinner.getValueFactory();
        String text = spinner.getEditor().getText().trim();

        int value = text.isEmpty() ? factory.getValue() : Integer.parseInt(text);
        int clamped = Math.max(factory.getMin(), Math.min(factory.getMax(), value));

        factory.setValue(clamped);
        spinner.getEditor().setText(Integer.toString(clamped));
        flagCorrection(spinner, clamped != value || text.isEmpty());
    }

    /** Feedback visual: borda vermelha quando o que foi digitado nao virou o valor. */
    private static void flagCorrection(Spinner<Integer> spinner, boolean corrected) {
        spinner.getEditor().setStyle(corrected
                ? "-fx-border-color: #e5534b; -fx-border-width: 1.5;"
                : "");
    }

    /** Enquanto roda, mexer nos campos nao muda nada — TournamentConfig e lido uma vez. */
    private void setInputsDisabled(boolean disabled) {
        playersSpinner.setDisable(disabled);
        threadsSpinner.setDisable(disabled);
        startButton.setDisable(disabled);
    }

    /** UC01. */
    private void startTournament() {
        // Clicar em start nao tira o foco do editor, entao o valor digitado
        // ainda nao esta comitado — comitar aqui e o que faz o torneio comecar
        // com o que esta na tela, e nao com o valor anterior.
        commitEditor(playersSpinner);
        commitEditor(threadsSpinner);

        if (tournament != null) {
            tournament.stop();
        }

        // Platform::runLater e o unico ponto de acoplamento com JavaFX na
        // camada de concorrencia — trocar de GUI troca so este Executor.
        EventBus bus = new EventBus(Platform::runLater);
        wire(bus);

        TournamentConfig config = new TournamentConfig(
                playersSpinner.getValue(), threadsSpinner.getValue(), 350);

        // RF07 / ADR-012: a escala sai de N e vale do start ao fim. Fixar aqui,
        // antes de tournament.start(), porque o start ja publica QueueChanged.
        ColorScale scale = ColorScale.forPlayers(config.players());
        queuePanel.scale(scale);
        arenaPanel.scale(scale);
        resultPanel.scale(scale);
        legend.show(scale);

        // Se o centro ainda mostra o resultado do torneio anterior, a arena
        // volta aqui: cada start reabre a tela dos jogos.
        root.setCenter(arenaPanel);

        tournament = new Tournament(config, bus);

        setInputsDisabled(true);
        status.setText("torneio rodando — N=" + config.players() + " T=" + config.threads());
        tournament.start();
    }

    private void wire(EventBus bus) {
        bus.subscribe(Events.QueueChanged.class, event -> {
            queuePanel.update(event.waiting(), event.alive());
            arenaPanel.refreshAll();
        });
        bus.subscribe(Events.MatchStarted.class, event -> arenaPanel.add(event.match()));
        bus.subscribe(Events.RoundPlayed.class, event -> {
            MatchView view = arenaPanel.view(event.match());
            if (view != null) {
                view.refresh();
            }
        });
        bus.subscribe(Events.MatchEnded.class, event -> arenaPanel.remove(event.match()));
        // PlayerScored continua sem assinante na UI: a escala e fixa, entao nao
        // ha o que renormalizar quando alguem pontua, e as metricas do RF12 sao
        // medidas na origem (MatchRunner), nao contadas aqui.
        bus.subscribe(Events.TournamentEnded.class, event -> { // UC06
            // Reabilita os campos junto com o botao (#4) e troca a arena — ja
            // vazia — pelo resultado (RF12). Sem janela: os controles de N e T
            // continuam a um clique, entao a proxima rodada nao passa por
            // fechar nada.
            setInputsDisabled(false);
            status.setText(summary(event.champion(), event.metrics()));
            resultPanel.show(event.champion(), event.metrics());
            root.setCenter(resultPanel);
        });
    }

    /** Linha compacta na barra de baixo; o painel do centro tem o detalhe. */
    private String summary(Player champion, TournamentMetrics metrics) {
        return String.format(Locale.ROOT,
                "%s — %d partidas em %.2f s (%.1f partidas/s)",
                champion == null ? "torneio encerrado" : "campeao: " + champion,
                metrics.matches(),
                metrics.totalMillis() / 1000.0,
                metrics.matchesPerSecond());
    }
}
