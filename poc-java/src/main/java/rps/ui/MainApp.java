package rps.ui;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
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
 * Camada de apresentacao.
 * So subscreve eventos e desenha.
 */
public final class MainApp extends Application {

    private final QueuePanel queuePanel =
            new QueuePanel();

    private final ArenaPanel arenaPanel =
            new ArenaPanel();

    private final Label status =
            new Label("pronto");

    private final Spinner<Integer> playersSpinner =
            new Spinner<>(2, 1000000, 100);

    private final Spinner<Integer> threadsSpinner =
            new Spinner<>(1, 64, 1);

    private final Button startButton =
            new Button("start");

    private Tournament tournament;

    private int maxScore = 1;

    @Override
    public void start(Stage stage) {

        BorderPane root =
                new BorderPane();

        root.setTop(controls());
        root.setLeft(queuePanel);
        root.setCenter(arenaPanel);
        root.setBottom(statusBar());

        root.setBackground(
                new Background(
                        new BackgroundFill(
                                Color.web("#0e1116"),
                                CornerRadii.EMPTY,
                                Insets.EMPTY
                        )
                )
        );

        startButton.setOnAction(
                event -> startTournament()
        );

        stage.setTitle(
                "RPS concorrente — PoC Java + JavaFX"
        );

        stage.setScene(
                new Scene(
                        root,
                        1100,
                        700
                )
        );

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

        HBox box =
                new HBox(
                        8,
                        label("N jogadores"),
                        playersSpinner,
                        label("T threads"),
                        threadsSpinner,
                        startButton
                );

        box.setAlignment(
                Pos.CENTER_LEFT
        );

        box.setPadding(
                new Insets(10)
        );

        box.setBackground(
                new Background(
                        new BackgroundFill(
                                Color.web("#161a21"),
                                CornerRadii.EMPTY,
                                Insets.EMPTY
                        )
                )
        );

        return box;
    }

    private HBox statusBar() {

        status.setTextFill(
                Color.web("#cfd3dc")
        );

        status.setFont(
                Font.font(13)
        );

        HBox box =
                new HBox(status);

        box.setPadding(
                new Insets(
                        8,
                        10,
                        8,
                        10
                )
        );

        box.setBackground(
                new Background(
                        new BackgroundFill(
                                Color.web("#161a21"),
                                CornerRadii.EMPTY,
                                Insets.EMPTY
                        )
                )
        );

        return box;
    }

    private Label label(String text) {

        Label label =
                new Label(text);

        label.setTextFill(
                Color.web("#cfd3dc")
        );

        return label;
    }

    /** UC01. */
    private void startTournament() {

        if (tournament != null) {
            tournament.stop();
        }

        maxScore = 1;

        EventBus bus =
                new EventBus(
                        Platform::runLater
                );

        wire(bus);

        TournamentConfig config =
                new TournamentConfig(
                        playersSpinner.getValue(),
                        threadsSpinner.getValue(),
                        500
                );

        tournament =
                new Tournament(
                        config,
                        bus
                );

        startButton.setDisable(true);

        status.setText(
                "torneio rodando — N="
                        + config.players()
                        + " T="
                        + config.threads()
        );

        tournament.start();
    }

    private void wire(EventBus bus) {

        bus.subscribe(
                Events.QueueChanged.class,
                event -> {

                    queuePanel.update(
                            event.alivePlayers(),
                            maxScore
                    );

                    arenaPanel.refreshAll(
                            maxScore
                    );
                }
        );

        bus.subscribe(
                Events.MatchStarted.class,
                event ->
                        arenaPanel.add(
                                event.match(),
                                maxScore
                        )
        );

        bus.subscribe(
                Events.RoundPlayed.class,
                event -> {

                    MatchView view =
                            arenaPanel.view(
                                    event.match()
                            );

                    if (view != null) {
                        view.refresh(
                                maxScore
                        );
                    }
                }
        );

        bus.subscribe(
                Events.MatchEnded.class,
                event ->
                        arenaPanel.remove(
                                event.match(),
                                maxScore
                        )
        );

        bus.subscribe(
                Events.PlayerScored.class,
                event -> {

                    if (event.score() > maxScore) {
                        maxScore =
                                event.score();
                    }
                }
        );

        /*
         * UC06:
         * recebe o campeão e as estatísticas
         * calculadas pelas threads.
         */
        bus.subscribe(
                Events.TournamentEnded.class,
                event -> {

                    startButton.setDisable(false);

                    String championText;

                    if (event.champion() == null) {

                        championText =
                                "Nenhum campeão";

                    } else {

                        championText =
                                "Jogador "
                                        + event.champion().id();
                    }

                    /*
                     * Conversão de nanossegundos
                     * para segundos.
                     */
                    double totalSeconds =
                            event.totalTimeNanos()
                                    / 1_000_000_000.0;

                    double averageSeconds = 0;

                    if (event.matchesPlayed() > 0) {

                        averageSeconds =
                                (event.totalMatchTimeNanos()
                                        / 1_000_000_000.0)
                                        / event.matchesPlayed();
                    }

                    status.setText(
                            "campeão: "
                                    + championText
                                    + " | tempo: "
                                    + String.format(
                                            "%.2f s",
                                            totalSeconds
                                    )
                    );

                    showTournamentResult(
                            championText,
                            event.matchesPlayed(),
                            averageSeconds,
                            totalSeconds
                    );
                }
        );
    }

    /**
     * Mostra uma nova janela com o
     * resultado final do torneio.
     */
    private void showTournamentResult(
            String champion,
            int matches,
            double averageMatchTime,
            double totalTime
    ) {

        Alert result =
                new Alert(
                        Alert.AlertType.INFORMATION
                );

        result.setTitle(
                "Resultado do torneio"
        );

        result.setHeaderText(
                "Torneio encerrado!"
        );

        result.setContentText(
                "Campeão: "
                        + champion
                        + "\n\n"
                        + "Partidas realizadas: "
                        + matches
                        + "\n"
                        + "Tempo médio por partida: "
                        + String.format(
                                "%.2f segundos",
                                averageMatchTime
                        )
                        + "\n"
                        + "Tempo total do torneio: "
                        + String.format(
                                "%.2f segundos",
                                totalTime
                        )
        );

        result.showAndWait();
    }
}