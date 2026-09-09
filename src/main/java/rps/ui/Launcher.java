package rps.ui;

import javafx.application.Application;

/**
 * Main class que NAO estende Application. Sem isso, rodar JavaFX a partir do
 * classpath (nao-modular) falha com "JavaFX runtime components are missing".
 * Detalhe de toolchain que ja conta como dado pra comparacao de stacks.
 */
public final class Launcher {

    public static void main(String[] args) {
        Application.launch(MainApp.class, args);
    }
}
