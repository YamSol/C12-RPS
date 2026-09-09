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

/** RF10: a fila viva. Redesenhada a cada QueueChanged. */
public final class QueuePanel extends VBox {

    private final Label header = new Label("fila");
    private final FlowPane chips = new FlowPane(4, 4);

    /** Fixa no start (ADR-012); o default so evita NPE antes do primeiro torneio. */
    private ColorScale scale = ColorScale.forPlayers(2);

    public QueuePanel() {
        setSpacing(6);
        setPadding(new Insets(10));
        setPrefWidth(240);
        setBackground(new Background(new BackgroundFill(Color.web("#161a21"), CornerRadii.EMPTY, Insets.EMPTY)));

        header.setFont(Font.font(14));
        header.setTextFill(Color.web("#cfd3dc"));
        getChildren().addAll(header, chips);
    }

    public void scale(ColorScale scale) {
        this.scale = scale;
    }

    public void update(List<Player> waiting, int alive) {
        header.setText("fila: " + waiting.size() + "   vivos: " + alive);
        chips.getChildren().clear();
        for (Player player : waiting) {
            chips.getChildren().add(chip(player));
        }
    }

    private Label chip(Player player) {
        Label label = new Label(String.valueOf(player.id()));
        label.setPrefSize(28, 28);
        label.setAlignment(Pos.CENTER);
        label.setFont(Font.font(11));
        label.setTextFill(scale.textOn(player.score()));
        label.setBackground(new Background(new BackgroundFill(
                scale.colorFor(player.score()), new CornerRadii(14), Insets.EMPTY)));
        return label;
    }
}
