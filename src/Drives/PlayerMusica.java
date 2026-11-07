
package Drives;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.function.Consumer;

import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.util.Duration;

import javax.swing.SwingUtilities;

public class PlayerMusica {

    static {
        new JFXPanel();
        Platform.setImplicitExit(false);
    }

    private static final HttpClient HTTP = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NORMAL)
            .connectTimeout(java.time.Duration.ofSeconds(30))
            .build();

    private MediaPlayer player;

    private volatile double tempoSeg = 0;
    private volatile int geracao = 0;
    private volatile boolean quererTocar = false;
    private volatile double volume = 0.8;

    private volatile Runnable aoTerminar;
    private volatile Consumer<Double> aoPronto;
    private volatile Consumer<String> aoErro;

    public void setAoTerminar(Runnable r) {
        aoTerminar = r;
    }

    public void setAoPronto(Consumer<Double> c) {
        aoPronto = c;
    }

    public void setAoErro(Consumer<String> c) {
        aoErro = c;
    }

    public double getTempoSeg() {
        return tempoSeg;
    }

    public void tocar(String idArquivo) {

        if (idArquivo == null || idArquivo.isBlank()) {
            erro(geracao, "ID do arquivo do Drive vazio.");
            return;
        }

        final int minha = ++geracao;

        quererTocar = true;
        tempoSeg = 0;

        Platform.runLater(this::descartar);

        Thread download = new Thread(() -> {
            try {
                System.out.println("Obtendo token do Google Drive...");

                String token = DriveToken.token();

                System.out.println("Token obtido.");
                System.out.println("Verificando cache...");

                Path arquivo = obterArquivoLocal(idArquivo, token);

                if (minha != geracao) {
                    return;
                }

                System.out.println("Arquivo local: " + arquivo);
                System.out.println("Tamanho: " + Files.size(arquivo) + " bytes");
                System.out.println("Abrindo áudio no JavaFX...");

                Platform.runLater(() -> {
                    if (minha == geracao) {
                        abrir(arquivo, minha);
                    }
                });

            } catch (Exception e) {
                e.printStackTrace();
                erro(minha, "Erro ao preparar música: " + e.getMessage());
            }
        }, "sonart-download");

        download.setDaemon(true);
        download.start();
    }

    private Path obterArquivoLocal(String idArquivo, String token)
            throws Exception {

        String base = System.getenv("LOCALAPPDATA");

        if (base == null || base.isBlank()) {
            base = System.getProperty("user.home");
        }

        Path pasta = Path.of(base, "Sonart", "cache");
        Files.createDirectories(pasta);

        Path destino = pasta.resolve(idArquivo + ".mp3");

        if (Files.exists(destino) && Files.size(destino) > 0) {
            System.out.println("Cache encontrado.");
            return destino;
        }

        System.out.println("Baixando música do Google Drive...");

        String url = "https://www.googleapis.com/drive/v3/files/"
                + idArquivo
                + "?alt=media&supportsAllDrives=true";

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(java.time.Duration.ofMinutes(10))
                .header("Authorization", "Bearer " + token)
                .header("Accept", "audio/mpeg, application/octet-stream")
                .GET()
                .build();

        Path temporario = Files.createTempFile(pasta, "download-", ".tmp");

        try {
            HttpResponse<Path> response = HTTP.send(
                    request,
                    HttpResponse.BodyHandlers.ofFile(temporario)
            );

            if (response.statusCode() != 200) {
                String detalhe = "";

                try {
                    detalhe = Files.readString(
                            temporario,
                            StandardCharsets.UTF_8
                    );
                } catch (Exception ignorado) {
                }

                throw new IOException(
                        "Google Drive respondeu HTTP "
                        + response.statusCode() + ". " + detalhe
                );
            }

            long tamanho = Files.size(temporario);

            if (tamanho == 0) {
                throw new IOException("Arquivo recebido está vazio.");
            }

            Files.move(
                    temporario,
                    destino,
                    StandardCopyOption.REPLACE_EXISTING
            );

            System.out.println("Download concluído: " + tamanho + " bytes");

            return destino;

        } finally {
            Files.deleteIfExists(temporario);
        }
    }

    private void abrir(Path arquivo, int minha) {

        descartar();

        try {
            Media media = new Media(arquivo.toUri().toString());

            media.setOnError(() ->
                    erro(minha, "Erro na mídia: " + media.getError())
            );

            MediaPlayer mp = new MediaPlayer(media);
            player = mp;

            mp.setVolume(volume);

            mp.setOnReady(() -> {
                if (minha != geracao) {
                    return;
                }

                double duracao = media.getDuration().toSeconds();

                System.out.println("Áudio pronto!");
                System.out.println("Duração: " + duracao + " segundos");

                Consumer<Double> callback = aoPronto;

                if (callback != null) {
                    SwingUtilities.invokeLater(() -> callback.accept(duracao));
                }

                if (quererTocar) {
                    System.out.println("Iniciando reprodução...");
                    mp.play();
                }
            });

            mp.setOnPlaying(() -> {
                if (minha == geracao) {
                    System.out.println("SONART: Reprodução iniciada!");
                }
            });

            mp.setOnPaused(() -> {
                if (minha == geracao) {
                    System.out.println("SONART: Música pausada.");
                }
            });

            mp.setOnEndOfMedia(() -> {
                if (minha != geracao) {
                    return;
                }

                System.out.println("SONART: Música terminou.");

                Runnable callback = aoTerminar;

                if (callback != null) {
                    SwingUtilities.invokeLater(callback);
                }
            });

            mp.setOnError(() ->
                    erro(minha, "Erro no MediaPlayer: " + mp.getError())
            );

            mp.currentTimeProperty().addListener((obs, anterior, atual) -> {
                if (minha == geracao) {
                    tempoSeg = atual.toSeconds();
                }
            });

        } catch (Exception e) {
            erro(minha, "Não foi possível abrir o áudio: " + e.getMessage());
        }
    }

    public void pausar() {
        quererTocar = false;

        Platform.runLater(() -> {
            if (player != null) {
                player.pause();
            }
        });
    }

    public void retomar() {
        quererTocar = true;

        Platform.runLater(() -> {
            if (player != null) {
                player.play();
            }
        });
    }

    public void pular(double segundos) {
        tempoSeg = segundos;

        Platform.runLater(() -> {
            if (player != null) {
                player.seek(Duration.seconds(segundos));
            }
        });
    }

    public void parar() {
        geracao++;
        quererTocar = false;
        tempoSeg = 0;

        Platform.runLater(this::descartar);
    }

    public void setVolume(double valor) {
        volume = Math.max(0, Math.min(1, valor));

        Platform.runLater(() -> {
            if (player != null) {
                player.setVolume(volume);
            }
        });
    }

    private void descartar() {
        if (player != null) {
            try {
                player.stop();
                player.dispose();
            } catch (Exception ignorado) {
            }

            player = null;
        }
    }

    private void erro(int minha, String mensagem) {
        if (minha != geracao) {
            return;
        }

        System.err.println("SONART ERRO: " + mensagem);

        Consumer<String> callback = aoErro;

        if (callback != null) {
            SwingUtilities.invokeLater(() -> callback.accept(mensagem));
        }
    }
}
