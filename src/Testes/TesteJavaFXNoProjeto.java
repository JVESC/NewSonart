
package Testes;

import java.io.File;
import java.util.concurrent.CountDownLatch;

import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;

public class TesteJavaFXNoProjeto {

    public static void main(String[] args) throws Exception {
        System.out.println("===== AMBIENTE DE EXECUÇÃO =====");
        System.out.println("Java: " + System.getProperty("java.version"));
        System.out.println("Java Home: " + System.getProperty("java.home"));
        System.out.println("Java Library Path: " + System.getProperty("java.library.path"));
        System.out.println("OS: " + System.getProperty("os.name"));
        System.out.println("Arquitetura: " + System.getProperty("os.arch"));
        System.out.println("Diretório: " + System.getProperty("user.dir"));
        System.out.println("Classpath: " + System.getProperty("java.class.path"));
        System.out.println("LD_LIBRARY_PATH: " + System.getenv("LD_LIBRARY_PATH"));
        System.out.println("GST_PLUGIN_PATH: " + System.getenv("GST_PLUGIN_PATH"));
        System.out.println("GST_PLUGIN_SYSTEM_PATH: " + System.getenv("GST_PLUGIN_SYSTEM_PATH"));
        System.out.println("===============================");
        new JFXPanel();
        Platform.setImplicitExit(false);

        CountDownLatch fim = new CountDownLatch(1);

        String caminho = System.getProperty("user.home")
                + "/Sonart/cache/14g-VL3okWWKVdY9_AKM8gcA6e7We8Vyx.mp3";

        Platform.runLater(() -> {

            try {
                System.out.println("Criando Media...");

                Media media = new Media(
                        new File(caminho).toURI().toString()
                );

                media.setOnError(() ->
                        System.err.println(
                                "ERRO MEDIA: " + media.getError()
                        )
                );

                MediaPlayer player = new MediaPlayer(media);

                player.setOnReady(() -> {
                    System.out.println("ÁUDIO PRONTO!");
                    player.play();
                    System.out.println("REPRODUÇÃO INICIADA!");
                });

                player.setOnEndOfMedia(() -> {
                    System.out.println("MÚSICA FINALIZADA!");
                    fim.countDown();
                });

                player.setOnError(() -> {
                    System.err.println(
                            "ERRO PLAYER: " + player.getError()
                    );
                    fim.countDown();
                });

            } catch (Exception e) {
                e.printStackTrace();
                fim.countDown();
            }
        });

        fim.await();
    }
}
