package rps.ui;

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

/**
 * Sub-issue #15: o mapa cor -> score. Sem isso a escala do RF07 e indecifravel
 * pra quem olha a tela.
 *
 * <p>Montada uma vez por torneio, junto com a {@link ColorScale}, porque a
 * escala e fixa do start ao fim (ADR-012).
 */
public final class ScaleLegend extends VBox {

    private final Label header = new Label("escala — vitorias");
    private final FlowPane amostras = new FlowPane(3, 3);

    public ScaleLegend() {
        setSpacing(6);
        setPadding(new Insets(10));
        setBackground(new Background(new BackgroundFill(Color.web("#161a21"), CornerRadii.EMPTY, Insets.EMPTY)));

        header.setFont(Font.font(12));
        header.setTextFill(Color.web("#cfd3dc"));
        getChildren().addAll(header, amostras);
    }

    /** Redesenha a legenda pra escala que o torneio vai usar. */
    public void show(ColorScale scale) {
        amostras.getChildren().clear();
        for (int nivel = 0; nivel < scale.levels(); nivel++) {
            amostras.getChildren().add(amostra(scale, nivel));
        }
    }

    private Label amostra(ColorScale scale, int nivel) {
        Label label = new Label(scale.labelFor(nivel));
        label.setPrefSize(26, 22);
        label.setAlignment(Pos.CENTER);
        label.setFont(Font.font(10));
        label.setTextFill(scale.textOn(nivel));
        label.setBackground(new Background(new BackgroundFill(
                scale.colorFor(nivel), new CornerRadii(4), Insets.EMPTY)));
        return label;
    }
}
