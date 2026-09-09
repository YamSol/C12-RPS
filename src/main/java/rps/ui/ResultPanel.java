package rps.ui;

import java.util.Locale;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import rps.concurrency.TournamentMetrics;
import rps.domain.Player;

/**
 * RF12 / UC06: o resultado do torneio, desenhado <b>dentro</b> da janela. Ocupa
 * o mesmo espaco da {@link ArenaPanel} e entra no lugar dela quando o torneio
 * acaba — a arena esta vazia nessa hora, entao nao ha o que esconder, e o
 * resultado aparece onde o olho ja estava.
 *
 * <p>So formata: os numeros chegam prontos no {@code TournamentEnded}.
 */
public final class ResultPanel extends VBox {

    private final Label header = new Label("resultado do torneio");
    private final Label champion = new Label();
    private final Label championChip = new Label();
    private final FlowPane cards = new FlowPane(10, 10);

    /** Fixa no start (ADR-012), igual aos outros paineis; o default so evita NPE. */
    private ColorScale scale = ColorScale.forPlayers(2);

    public ResultPanel() {
        setSpacing(18);
        setPadding(new Insets(30));
        setAlignment(Pos.CENTER);
        setBackground(new Background(new BackgroundFill(Color.web("#0e1116"), CornerRadii.EMPTY, Insets.EMPTY)));

        header.setFont(Font.font(13));
        header.setTextFill(Color.web("#8b93a1"));

        champion.setFont(Font.font(null, FontWeight.BOLD, 26));
        champion.setTextFill(Color.web("#f2f4f8"));

        championChip.setPrefSize(52, 52);
        championChip.setAlignment(Pos.CENTER);
        championChip.setFont(Font.font(null, FontWeight.BOLD, 18));

        HBox linhaCampeao = new HBox(14, championChip, champion);
        linhaCampeao.setAlignment(Pos.CENTER);

        cards.setAlignment(Pos.CENTER);
        getChildren().addAll(header, linhaCampeao, cards);
    }

    public void scale(ColorScale scale) {
        this.scale = scale;
    }

    /** Redesenha o painel com o campeao e as metricas do torneio que acabou. */
    public void show(Player winner, TournamentMetrics metrics) {
        if (winner == null) {
            championChip.setText("");
            championChip.setBackground(null);
            champion.setText("torneio encerrado sem campeao");
        } else {
            championChip.setText(String.valueOf(winner.id()));
            championChip.setTextFill(scale.textOn(winner.score()));
            championChip.setBackground(new Background(new BackgroundFill(
                    scale.colorFor(winner.score()), new CornerRadii(26), Insets.EMPTY)));
            champion.setText("campeao: jogador #" + winner.id()
                    + "  ·  " + winner.score() + " vitorias");
        }

        cards.getChildren().setAll(
                card(String.valueOf(metrics.matches()), "partidas"),
                card(format("%.2f s", metrics.totalMillis() / 1000.0), "tempo total"),
                card(format("%.0f ms", metrics.avgMatchMillis()), "media por partida"),
                card(format("%.1f", metrics.matchesPerSecond()), "partidas/s"));
    }

    /** Um numero grande com o rotulo embaixo — a unidade fica no valor. */
    private VBox card(String valor, String rotulo) {
        Label valorLabel = new Label(valor);
        valorLabel.setFont(Font.font(null, FontWeight.BOLD, 22));
        valorLabel.setTextFill(Color.web("#f2f4f8"));

        Label rotuloLabel = new Label(rotulo);
        rotuloLabel.setFont(Font.font(11));
        rotuloLabel.setTextFill(Color.web("#8b93a1"));

        // Largura fixa: as quatro caixas cabem lado a lado na arena mesmo com a
        // janela no tamanho minimo util, e o FlowPane so quebra abaixo disso.
        VBox box = new VBox(4, valorLabel, rotuloLabel);
        box.setAlignment(Pos.CENTER);
        box.setPrefWidth(128);
        box.setPadding(new Insets(16, 14, 16, 14));
        box.setBackground(new Background(new BackgroundFill(
                Color.web("#161a21"), new CornerRadii(8), Insets.EMPTY)));
        return box;
    }

    private static String format(String pattern, double valor) {
        return String.format(Locale.ROOT, pattern, valor);
    }
}
