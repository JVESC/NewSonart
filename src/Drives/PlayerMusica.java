package Drives;

import java.util.function.Consumer;
import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.util.Duration;
import javax.swing.SwingUtilities;

/**
 * Toca músicas do Google Drive usando JavaFX Media dentro de uma aplicação Swing.
 * Todos os callbacks (aoTerminar, aoPronto, aoErro) são entregues na thread do Swing.
 */
public class PlayerMusica {

    static {
        new JFXPanel();                       // liga o toolkit do JavaFX
        Platform.setImplicitExit(false);      // não encerra o JavaFX sozinho
    }

    private MediaPlayer player;                       // só mexer nele na thread do JavaFX
    private volatile double tempoSeg = 0;
    private volatile int geracao = 0;                 // descarta callbacks de músicas antigas
    private volatile boolean quererTocar = false;
    private volatile Runnable aoTerminar;
    private volatile Consumer<Double> aoPronto;
    private volatile Consumer<String> aoErro;

    // ---------- configuração ----------
    public void setAoTerminar(Runnable r) { aoTerminar = r; }
    public void setAoPronto(Consumer<Double> c) { aoPronto = c; }   // recebe a duração real em segundos
    public void setAoErro(Consumer<String> c) { aoErro = c; }

    public double getTempoSeg() { return tempoSeg; }

    // ---------- controles ----------
    /** idArquivo = ID do arquivo no Drive (o que fica em musica.caminho_arquivo). */
    public void tocar(String idArquivo) {
        final int minha = ++geracao;
        quererTocar = true;
        tempoSeg = 0;
        new Thread(() -> {
            try {
                String url = "https://www.googleapis.com/drive/v3/files/" + idArquivo
                        + "?alt=media&access_token=" + DriveToken.token();
                Platform.runLater(() -> {
                    if (minha == geracao) abrir(url, minha);
                });
            } catch (Exception e) {
                erro(minha, "Falha na autenticação com o Drive: " + e.getMessage());
            }
        }, "drive-token").start();
    }

    public void pausar() {
        quererTocar = false;
        Platform.runLater(() -> { if (player != null) player.pause(); });
    }

    public void retomar() {
        quererTocar = true;
        Platform.runLater(() -> { if (player != null) player.play(); });
    }

    public void pular(double seg) {
        tempoSeg = seg;                       // o slider não "volta" enquanto o seek acontece
        Platform.runLater(() -> { if (player != null) player.seek(Duration.seconds(seg)); });
    }

    public void parar() {
        geracao++;
        quererTocar = false;
        tempoSeg = 0;
        Platform.runLater(this::descartar);
    }

    /** 0.0 a 1.0 */
    public void setVolume(double v) {
        final double vol = Math.max(0, Math.min(1, v));
        Platform.runLater(() -> { if (player != null) player.setVolume(vol); });
    }

    // ---------- internos (thread do JavaFX) ----------
    private void abrir(String url, int minha) {
        descartar();
        try {
            Media media = new Media(url);
            MediaPlayer mp = new MediaPlayer(media);
            player = mp;

            mp.setOnReady(() -> {
                if (minha != geracao) return;
                double d = media.getDuration().toSeconds();
                Consumer<Double> cb = aoPronto;
                if (cb != null && d > 0 && !Double.isInfinite(d)) {
                    SwingUtilities.invokeLater(() -> cb.accept(d));
                }
                if (quererTocar) mp.play();
            });

            mp.currentTimeProperty().addListener((obs, antigo, novo) -> {
                if (minha == geracao) tempoSeg = novo.toSeconds();
            });

            mp.setOnEndOfMedia(() -> {
                if (minha != geracao) return;
                Runnable r = aoTerminar;
                if (r != null) SwingUtilities.invokeLater(r);
            });

            mp.setOnError(() -> erro(minha, String.valueOf(mp.getError())));
            media.setOnError(() -> erro(minha, String.valueOf(media.getError())));
        } catch (Exception e) {
            erro(minha, e.getMessage());
        }
    }

    private void descartar() {
        if (player != null) {
            try {
                player.stop();
                player.dispose();
            } catch (Exception ignorado) { }
            player = null;
        }
    }

    private void erro(int minha, String msg) {
        if (minha != geracao) return;
        Consumer<String> cb = aoErro;
        if (cb != null) SwingUtilities.invokeLater(() -> cb.accept(msg));
    }
}