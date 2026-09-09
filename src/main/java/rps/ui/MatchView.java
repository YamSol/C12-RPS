package rps.ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;

import rps.domain.Match;
import rps.domain.Round;

/**
 * Um match desenhado: dois retangulos coloridos pelo score de cada jogador,
 * com a jogada corrente no meio. So le estado, nao decide nada.
 */
public final class MatchView extends VBox {

    private final Match match;
    private final ColorScale scale;
    private final VBox left = new VBox();
    private final VBox right = new VBox();
    private final Label leftName = new Label();
    private final Label rightName = new Label();
    private final Label leftMove = new Label("...");
    private final Label rightMove = new Label("...");
    private final Label title = new Label();

    public MatchView(Match match, ColorScale scale) {
        this.match = match;
        this.scale = scale;
        setSpacing(4);
        setPadding(new Insets(6));

        title.setFont(Font.font(12));
        title.setTextFill(Color.web("#cfd3dc"));
        title.setText("match #" + match.id());

        HBox arena = new HBox(6);
        HBox.setHgrow(left, Priority.ALWAYS);
        HBox.setHgrow(right, Priority.ALWAYS);
        VBox.setVgrow(arena, Priority.ALWAYS);
        arena.getChildren().addAll(
                side(left, match.p1(), leftName, leftMove),
                side(right, match.p2(), rightName, rightMove));

        getChildren().addAll(title, arena);
        refresh();
    }

    private VBox side(VBox box, rps.domain.Player player, Label nameLabel, Label moveLabel) {
        box.setAlignment(Pos.CENTER);
        box.setSpacing(2);
        nameLabel.setText(player.toString());
        nameLabel.setFont(Font.font(13));
        moveLabel.setFont(Font.font(11));
        box.getChildren().addAll(nameLabel, moveLabel);
        HBox.setHgrow(box, Priority.ALWAYS);
        box.setMaxWidth(Double.MAX_VALUE);
        box.setMaxHeight(Double.MAX_VALUE);
        return box;
    }

    /** Repinta a partir do estado atual do match. Chamado sempre na UI thread. */
    public void refresh() {
        paint(left, leftName, leftMove, match.p1().score());
        paint(right, rightName, rightMove, match.p2().score());

        Round round = match.lastRound();
        leftMove.setText(round == null ? "..." : round.p1().name());
        rightMove.setText(round == null ? "..." : round.p2().name());

        if (round == null) {
            return;
        }
        if (round.isDraw()) {
            title.setText("match #" + match.id() + " — empate, replay");
            left.setOpacity(1);
            right.setOpacity(1);
            return;
        }
        // Rodada decisiva: apaga o perdedor. O runner ainda vai segurar o match
        // na arena por um instante antes de publicar MatchEnded.
        boolean p1Won = round.p1().beats(round.p2()) > 0;
        title.setText("match #" + match.id() + " — vence " + (p1Won ? match.p1() : match.p2()));
        (p1Won ? right : left).setOpacity(0.25);
    }

    private void paint(VBox box, Label nameLabel, Label moveLabel, int score) {
        box.setBackground(new Background(new BackgroundFill(
                scale.colorFor(score), new CornerRadii(6), Insets.EMPTY)));
        // O texto acompanha o nivel: a rampa tem ponta escura e ponta clara.
        Color texto = scale.textOn(score);
        nameLabel.setTextFill(texto);
        moveLabel.setTextFill(texto);
    }

    public Match match() {
        return match;
    }
}
