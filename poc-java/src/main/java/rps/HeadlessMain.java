package rps;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import rps.concurrency.EventBus;
import rps.concurrency.Events;
import rps.concurrency.Tournament;
import rps.concurrency.TournamentConfig;

/**
 * Roda o torneio sem GUI, so pra provar que as camadas de dominio e
 * concorrencia terminam sozinhas (RF09) e nao travam. Nao depende de JavaFX —
 * compila e roda com um JDK puro:
 *
 * <pre>
 * javac --release 21 -d out $(find src/main/java/rps/domain src/main/java/rps/concurrency -name '*.java') src/main/java/rps/HeadlessMain.java
 * java -cp out rps.HeadlessMain 60 4
 * </pre>
 */
public final class HeadlessMain {

    public static void main(String[] args) throws InterruptedException {
        int players = args.length > 0 ? Integer.parseInt(args[0]) : 60;
        int threads = args.length > 1 ? Integer.parseInt(args[1]) : 4;
        long delay = args.length > 2 ? Long.parseLong(args[2]) : 0;

        CountDownLatch done = new CountDownLatch(1);
        // Sem UI thread: o "executor de UI" e o proprio chamador.
        EventBus bus = new EventBus(Runnable::run);

        bus.subscribe(Events.MatchEnded.class, event ->
                System.out.println("match #" + event.match().id()
                        + " -> " + event.result().winner()
                        + " (" + event.result().rounds().size() + " rodada(s))"));
        bus.subscribe(Events.TournamentEnded.class, event -> {
            System.out.println("campeao: " + event.champion());
            done.countDown();
        });

        Tournament tournament = new Tournament(new TournamentConfig(players, threads, delay), bus);
        tournament.start();

        if (!done.await(60, TimeUnit.SECONDS)) {
            System.err.println("TIMEOUT: torneio nao terminou — provavel deadlock");
            tournament.stop();
            System.exit(1);
        }
        System.out.println("terminou com alive=" + tournament.queue().alive()
                + " activeMatches=" + tournament.activeMatches().size());
    }
}
