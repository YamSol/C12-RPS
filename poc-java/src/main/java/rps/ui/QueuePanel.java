package rps.ui;

import java.util.List;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;

import rps.domain.Player;

/** Painel que mostra todos os jogadores ainda vivos. */
public final class QueuePanel extends VBox {

    private final Label header =
            new Label("jogadores vivos");

    private final FlowPane chips =
            new FlowPane(4, 4);

    public QueuePanel() {

        setSpacing(6);
        setPadding(new Insets(10));
        setPrefWidth(240);

        setBackground(
                new Background(
                        new BackgroundFill(
                                Color.web("#161a21"),
                                CornerRadii.EMPTY,
                                Insets.EMPTY
                        )
                )
        );

        header.setFont(Font.font(14));
        header.setTextFill(
                Color.web("#cfd3dc")
        );

        getChildren().addAll(
                header,
                chips
        );
    }

    public void update(
            List<Player> alivePlayers,
            int maxScore
    ) {

        header.setText(
                "jogadores vivos: "
                        + alivePlayers.size()
        );

        chips.getChildren().clear();

        for (Player player : alivePlayers) {

            chips.getChildren().add(
                    chip(player, maxScore)
            );
        }
    }

    private Label chip(
            Player player,
            int maxScore
    ) {

        Label label =
                new Label(
                        String.valueOf(player.id())
                );

        label.setPrefSize(28, 28);
        label.setAlignment(Pos.CENTER);
        label.setFont(Font.font(11));

        label.setTextFill(
                Color.web("#101318")
        );

        label.setBackground(
                new Background(
                        new BackgroundFill(
                                ColorMapper.scoreToColor(
                                        player.score(),
                                        maxScore
                                ),
                                new CornerRadii(14),
                                Insets.EMPTY
                        )
                )
        );

        return label;
    }
}