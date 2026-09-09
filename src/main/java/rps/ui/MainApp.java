package rps.ui;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.stage.Stage;

import rps.concurrency.EventBus;
import rps.concurrency.Events;
import rps.concurrency.Tournament;
import rps.concurrency.TournamentConfig;

/**
 * Camada de apresentacao. So subscreve eventos e desenha — nenhuma regra de
 * jogo, nenhum lock, nenhuma espera (RNF01).
 */
public final class MainApp extends Application {

    private final QueuePanel queuePanel = new QueuePanel();
    private final ArenaPanel arenaPanel = new ArenaPanel();
    private final Label status = new Label("pronto");
    private final Spinner<Integer> playersSpinner = new Spinner<>(2, 500, 60);
    private final Spinner<Integer> threadsSpinner = new Spinner<>(1, 64, 4);
    private final Button startButton = new Button("start");

    private Tournament tournament;
    private int maxScore = 1;

    @Override
    public void start(Stage stage) {
        BorderPane root = new BorderPane();
        root.setTop(controls());
        root.setLeft(queuePanel);
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

    /** UC01. */
    private void startTournament() {
        if (tournament != null) {
            tournament.stop();
        }
        maxScore = 1;

        // Platform::runLater e o unico ponto de acoplamento com JavaFX na
        // camada de concorrencia — trocar de GUI troca so este Executor.
        EventBus bus = new EventBus(Platform::runLater);
        wire(bus);

        TournamentConfig config = new TournamentConfig(
                playersSpinner.getValue(), threadsSpinner.getValue(), 350);
        tournament = new Tournament(config, bus);

        startButton.setDisable(true);
        status.setText("torneio rodando — N=" + config.players() + " T=" + config.threads());
        tournament.start();
    }

    private void wire(EventBus bus) {
        bus.subscribe(Events.QueueChanged.class, event -> {
            queuePanel.update(event.waiting(), event.alive(), maxScore);
            arenaPanel.refreshAll(maxScore);
        });
        bus.subscribe(Events.MatchStarted.class, event -> arenaPanel.add(event.match(), maxScore));
        bus.subscribe(Events.RoundPlayed.class, event -> {
            MatchView view = arenaPanel.view(event.match());
            if (view != null) {
                view.refresh(maxScore);
            }
        });
        bus.subscribe(Events.MatchEnded.class, event -> arenaPanel.remove(event.match(), maxScore));
        bus.subscribe(Events.PlayerScored.class, event -> {
            if (event.score() > maxScore) {
                maxScore = event.score(); // renormaliza o gradiente (RF07)
            }
        });
        bus.subscribe(Events.TournamentEnded.class, event -> { // UC06
            startButton.setDisable(false);
            status.setText(event.champion() == null
                    ? "torneio encerrado"
                    : "campeao: " + event.champion());
        });
    }
}
