package rps.ui;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
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
 * RF12 / UC06: o resultado do torneio, desenhado dentro da janela.
 * Ocupa o mesmo espaco da ArenaPanel e entra no lugar dela quando o torneio acaba.
 *
 * Tambem salva os dados finais da execucao em resultados.csv.
 */
public final class ResultPanel extends VBox {

    private final Label header = new Label("resultado do torneio");
    private final Label champion = new Label();
    private final Label championChip = new Label();
    private final FlowPane cards = new FlowPane(10, 10);

    /**
     * Fixa no start, igual aos outros paineis.
     * O default so evita NPE.
     */
    private ColorScale scale = ColorScale.forPlayers(2);

    public ResultPanel() {
        setSpacing(18);
        setPadding(new Insets(30));
        setAlignment(Pos.CENTER);

        setBackground(
                new Background(
                        new BackgroundFill(
                                Color.web("#0e1116"),
                                CornerRadii.EMPTY,
                                Insets.EMPTY
                        )
                )
        );

        header.setFont(Font.font(13));
        header.setTextFill(Color.web("#8b93a1"));

        champion.setFont(
                Font.font(
                        null,
                        FontWeight.BOLD,
                        26
                )
        );

        champion.setTextFill(
                Color.web("#f2f4f8")
        );

        championChip.setPrefSize(52, 52);
        championChip.setAlignment(Pos.CENTER);

        championChip.setFont(
                Font.font(
                        null,
                        FontWeight.BOLD,
                        18
                )
        );

        HBox linhaCampeao =
                new HBox(
                        14,
                        championChip,
                        champion
                );

        linhaCampeao.setAlignment(Pos.CENTER);

        cards.setAlignment(Pos.CENTER);

        getChildren().addAll(
                header,
                linhaCampeao,
                cards
        );
    }

    public void scale(ColorScale scale) {
        this.scale = scale;
    }

    /**
     * Mostra o resultado final do torneio.
     *
     * Agora tambem recebe a quantidade de jogadores
     * e de threads utilizadas na execucao.
     */
    public void show(
            Player winner,
            TournamentMetrics metrics,
            int jogadores,
            int threads
    ) {

        if (winner == null) {

            championChip.setText("");
            championChip.setBackground(null);

            champion.setText(
                    "torneio encerrado sem campeao"
            );

        } else {

            championChip.setText(
                    String.valueOf(
                            winner.id()
                    )
            );

            championChip.setTextFill(
                    scale.textOn(
                            winner.score()
                    )
            );

            championChip.setBackground(
                    new Background(
                            new BackgroundFill(
                                    scale.colorFor(
                                            winner.score()
                                    ),
                                    new CornerRadii(26),
                                    Insets.EMPTY
                            )
                    )
            );

            champion.setText(
                    "campeao: jogador #"
                            + winner.id()
                            + "  ·  "
                            + winner.score()
                            + " vitorias"
            );
        }

        cards.getChildren().setAll(

                card(
                        String.valueOf(
                                metrics.matches()
                        ),
                        "partidas"
                ),

                card(
                        format(
                                "%.2f s",
                                metrics.totalMillis()
                                        / 1000.0
                        ),
                        "tempo total"
                ),

                card(
                        format(
                                "%.0f ms",
                                metrics.avgMatchMillis()
                        ),
                        "media por partida"
                ),

                card(
                        format(
                                "%.1f",
                                metrics.matchesPerSecond()
                        ),
                        "partidas/s"
                )
        );

        /*
         * Quando o torneio acaba,
         * salva os dados desta execucao no CSV.
         */
        salvarResultado(
                winner,
                metrics,
                jogadores,
                threads
        );
    }

    /**
     * Salva os dados finais no arquivo resultados.csv.
     *
     * Cada torneio gera uma nova linha.
     * O arquivo nao e sobrescrito.
     */
    private void salvarResultado(
            Player winner,
            TournamentMetrics metrics,
            int jogadores,
            int threads
    ) {

        File arquivoCSV =
                new File(
                        "resultados.csv"
                );

        /*
         * Se o arquivo ainda nao existe,
         * precisamos criar o cabecalho.
         */
        boolean arquivoNovo =
                !arquivoCSV.exists();

        try (
                FileWriter arquivo =
                        new FileWriter(
                                arquivoCSV,
                                true
                        )
        ) {

            /*
             * Primeira linha do CSV.
             *
             * Assim depois fica facil
             * identificar cada coluna
             * no Python ou Excel.
             */
            if (arquivoNovo) {

                arquivo.write(
                        "jogadores,"
                        + "threads,"
                        + "campeao,"
                        + "vitorias_campeao,"
                        + "partidas,"
                        + "tempo_total_ms,"
                        + "tempo_medio_partida_ms,"
                        + "partidas_por_segundo\n"
                );
            }

            int championId =
                    winner == null
                            ? -1
                            : winner.id();

            int championWins =
                    winner == null
                            ? 0
                            : winner.score();

            /*
             * Salva uma linha com os
             * resultados desta execucao.
             */
            arquivo.write(
                    jogadores + ","
                    + threads + ","
                    + championId + ","
                    + championWins + ","
                    + metrics.matches() + ","
                    + metrics.totalMillis() + ","
                    + format(
                            "%.2f",
                            metrics.avgMatchMillis()
                    ) + ","
                    + format(
                            "%.2f",
                            metrics.matchesPerSecond()
                    )
                    + "\n"
            );

        } catch (IOException e) {

            System.out.println(
                    "Erro ao salvar resultado: "
                            + e.getMessage()
            );
        }
    }

    /**
     * Cria cada card da tela de resultados.
     */
    private VBox card(
            String valor,
            String rotulo
    ) {

        Label valorLabel =
                new Label(valor);

        valorLabel.setFont(
                Font.font(
                        null,
                        FontWeight.BOLD,
                        22
                )
        );

        valorLabel.setTextFill(
                Color.web("#f2f4f8")
        );

        Label rotuloLabel =
                new Label(rotulo);

        rotuloLabel.setFont(
                Font.font(11)
        );

        rotuloLabel.setTextFill(
                Color.web("#8b93a1")
        );

        VBox box =
                new VBox(
                        4,
                        valorLabel,
                        rotuloLabel
                );

        box.setAlignment(Pos.CENTER);

        box.setPrefWidth(128);

        box.setPadding(
                new Insets(
                        16,
                        14,
                        16,
                        14
                )
        );

        box.setBackground(
                new Background(
                        new BackgroundFill(
                                Color.web("#161a21"),
                                new CornerRadii(8),
                                Insets.EMPTY
                        )
                )
        );

        return box;
    }

    /**
     * Mantem ponto como separador decimal,
     * importante para o CSV e para o Python.
     */
    private static String format(
            String pattern,
            double valor
    ) {

        return String.format(
                Locale.ROOT,
                pattern,
                valor
        );
    }
}