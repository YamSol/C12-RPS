package rps.ui;

import java.util.LinkedHashMap;
import java.util.Map;

import javafx.geometry.Insets;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.RowConstraints;
import javafx.scene.paint.Color;

import rps.domain.Match;

/**
 * RF11 / UC05: o espaco se redivide sozinho conforme o numero de matches ativos.
 * 1 -> tela cheia; 2 -> 50/50; N -> grade quadrada mais proxima, celulas iguais.
 */
public final class ArenaPanel extends GridPane {

    private final Map<Integer, MatchView> views = new LinkedHashMap<>();

    /** Fixa no start (ADR-012); o default so evita NPE antes do primeiro torneio. */
    private ColorScale scale = ColorScale.forPlayers(2);

    public ArenaPanel() {
        setHgap(8);
        setVgap(8);
        setPadding(new Insets(10));
        setBackground(new Background(new BackgroundFill(Color.web("#0e1116"), CornerRadii.EMPTY, Insets.EMPTY)));
    }

    public void scale(ColorScale scale) {
        this.scale = scale;
    }

    public void add(Match match) {
        views.put(match.id(), new MatchView(match, scale));
        relayout();
    }

    public void remove(Match match) {
        views.remove(match.id());
        relayout();
    }

    /** Repinta todos (score de alguem pode ter mudado). */
    public void refreshAll() {
        views.values().forEach(MatchView::refresh);
    }

    public MatchView view(Match match) {
        return views.get(match.id());
    }

    private void relayout() {
        getChildren().clear();
        getColumnConstraints().clear();
        getRowConstraints().clear();

        int n = views.size();
        if (n == 0) {
            return;
        }
        int cols = (int) Math.ceil(Math.sqrt(n));
        int rows = (int) Math.ceil((double) n / cols);

        for (int c = 0; c < cols; c++) {
            ColumnConstraints cc = new ColumnConstraints();
            cc.setPercentWidth(100.0 / cols);
            cc.setHgrow(Priority.ALWAYS);
            getColumnConstraints().add(cc);
        }
        for (int r = 0; r < rows; r++) {
            RowConstraints rc = new RowConstraints();
            rc.setPercentHeight(100.0 / rows);
            rc.setVgrow(Priority.ALWAYS);
            getRowConstraints().add(rc);
        }

        int i = 0;
        for (MatchView view : views.values()) {
            view.refresh();
            view.setMaxWidth(Double.MAX_VALUE);
            view.setMaxHeight(Double.MAX_VALUE);
            GridPane.setHgrow(view, Priority.ALWAYS);
            GridPane.setVgrow(view, Priority.ALWAYS);
            add(view, i % cols, i / cols);
            i++;
        }
    }
}
