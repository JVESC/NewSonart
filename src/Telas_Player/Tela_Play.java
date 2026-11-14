/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/GUIForms/JPanel.java to edit this template
 */
package Telas_Player;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import javax.swing.JPanel;
import Dados.Conexao;
import Dados.Faixa;
import Dados.FaixaDAO;
import Drives.PlayerMusica;
import java.awt.BorderLayout;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.net.URI;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.imageio.ImageIO;
import javax.swing.SwingWorker;
import java.util.List;
import java.util.ArrayList;
/**
 *
 * @author jvitor
 */
public class Tela_Play extends javax.swing.JPanel {
private DiscoVinil vinil;
    private final PlayerMusica player = new PlayerMusica();
    private final Map<String, BufferedImage> cacheCapas = new HashMap<>();
 
    private List<Faixa> fila = new ArrayList<>();
    private int indice = -1;
    private boolean tocando = false;
    private boolean faixaCarregada = false;   // o player já recebeu a faixa atual?
    private int duracaoSeg = 0;
    private javax.swing.Timer timerUI;
 
    public Tela_Play() {
        initComponents();
 
        // disco de vinil dentro do jPanel1
        vinil = new DiscoVinil();
        jPanel1.setOpaque(false);
        jPanel1.setLayout(new BorderLayout());
        jPanel1.add(vinil, BorderLayout.CENTER);
 
        configurarControles();
        carregarFila();
    }
 
    // ---------------- configuração dos controles ----------------
 
    private void configurarControles() {
        btnPlay.setText("Play");
        btnVoltar.setText("Voltar");
        btnAvancar.setText("Avançar");
        lblTitulo.setText("Carregando...");
        lblArtista.setText(" ");
        lblTempo.setText("0:00 / 0:00");
        jSTempo.setValue(0);
        jSTempo.setEnabled(false);
 
        // a cada 0,5 s o slider acompanha o tempo real do player
        timerUI = new javax.swing.Timer(500, e -> {
            if (!jSTempo.getValueIsAdjusting()) {
                jSTempo.setValue((int) player.getTempoSeg());
            }
        });
 
        // slider mexeu (timer ou usuário): atualiza agulha e rótulo de tempo
        jSTempo.addChangeListener(e -> {
            if (jSTempo.getMaximum() > 0) {
                vinil.setProgresso(jSTempo.getValue() / (double) jSTempo.getMaximum());
            }
            atualizarRotuloTempo();
        });
 
        // usuário soltou o slider: pula para aquele ponto
        jSTempo.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseReleased(MouseEvent e) {
                if (faixaCarregada) player.pular(jSTempo.getValue());
            }
        });
 
        btnPlay.addActionListener(e -> alternarPlay());
        btnAvancar.addActionListener(e -> avancar());
        btnVoltar.addActionListener(e -> voltar());
 
        player.setAoTerminar(this::fimDaMusica);
 
        // duração real do arquivo (corrige o duracao_seg do banco se estiver diferente)
        player.setAoPronto(real -> {
            int r = (int) Math.round(real);
            if (r > 0 && Math.abs(r - duracaoSeg) > 1) definirDuracao(r);
        });
 
        player.setAoErro(msg -> {
            System.err.println("Erro no player: " + msg);
            pararVisual();
            lblArtista.setText("Erro ao tocar (veja o Output)");
        });
    }
 
    // ---------------- fila (vem do banco) ----------------
 
    private void carregarFila() {
        new SwingWorker<List<Faixa>, Void>() {
            @Override
            protected List<Faixa> doInBackground() throws Exception {
                try (Connection c = Conexao.abrir()) {
                    return FaixaDAO.listarTodas(c);
                }
            }
 
            @Override
            protected void done() {
                try {
                    fila = get();
                    if (fila.isEmpty()) {
                        lblTitulo.setText("Nenhuma música cadastrada");
                        return;
                    }
                    selecionar(0, false);       // mostra a primeira, sem tocar ainda
                } catch (Exception ex) {
                    ex.printStackTrace();
                    lblTitulo.setText("Erro ao acessar o banco");
                }
            }
        }.execute();
    }
 
    // ---------------- troca de faixa ----------------
 
    private void selecionar(int i, boolean iniciar) {
        if (fila.isEmpty()) return;
        int n = fila.size();
        indice = ((i % n) + n) % n;
        Faixa f = fila.get(indice);
 
        player.parar();
        faixaCarregada = false;
        timerUI.stop();
 
        lblTitulo.setText(f.titulo);
        lblArtista.setText(f.artista + " - " + f.album);
        definirDuracao(f.duracaoSeg);
        jSTempo.setValue(0);
        jSTempo.setEnabled(true);
        vinil.setProgresso(0);
        carregarCapa(f.capaUrl);
 
        if (iniciar) {
            iniciarReproducao(f);
        } else {
            tocando = false;
            vinil.pausar();
            btnPlay.setText("Play");
        }
    }
 
    private void iniciarReproducao(Faixa f) {
        faixaCarregada = true;
        tocando = true;
        player.tocar(f.idDrive);
        vinil.tocar();
        timerUI.start();
        btnPlay.setText("Pausar");
    }
 
    // ---------------- botões ----------------
 
    private void alternarPlay() {
        if (indice < 0) return;
        if (!tocando) {
            if (faixaCarregada) {
                player.retomar();
                vinil.tocar();
                timerUI.start();
                tocando = true;
                btnPlay.setText("Pausar");
            } else {
                iniciarReproducao(fila.get(indice));
            }
        } else {
            player.pausar();
            vinil.pausar();
            timerUI.stop();
            tocando = false;
            btnPlay.setText("Play");
        }
    }
 
    private void avancar() {
        selecionar(indice + 1, true);
    }
 
    private void voltar() {
        if (jSTempo.getValue() > 3 && faixaCarregada) {   // já passou de 3 s: reinicia a faixa
            jSTempo.setValue(0);
            player.pular(0);
        } else {
            selecionar(indice - 1, true);
        }
    }
 
    private void fimDaMusica() {
        if (fila.size() > 1) {
            avancar();
        } else {                                           // só uma música: para no começo
            player.parar();
            pararVisual();
            jSTempo.setValue(0);
            vinil.setProgresso(0);
        }
    }
 
    private void pararVisual() {
        tocando = false;
        faixaCarregada = false;
        vinil.pausar();
        timerUI.stop();
        btnPlay.setText("Play");
    }
 
    // ---------------- tempo ----------------
 
    private void definirDuracao(int segundos) {
        duracaoSeg = segundos;
        jSTempo.setMaximum(Math.max(1, segundos));
        atualizarRotuloTempo();
    }
 
    private void atualizarRotuloTempo() {
        lblTempo.setText(formatar(jSTempo.getValue()) + " / " + formatar(duracaoSeg));
    }
 
    private static String formatar(int seg) {
        return String.format("%d:%02d", seg / 60, seg % 60);
    }
 
    // ---------------- capa no vinil ----------------
 
    private void carregarCapa(String url) {
        vinil.setCapa(null);
        if (url == null || url.isBlank()) return;
 
        BufferedImage guardada = cacheCapas.get(url);
        if (guardada != null) {
            vinil.setCapa(guardada);
            return;
        }
 
        new SwingWorker<BufferedImage, Void>() {
            @Override
            protected BufferedImage doInBackground() throws Exception {
                BufferedImage original = ImageIO.read(URI.create(url).toURL());
                return original == null ? null : prepararCapa(original, 400);
            }
 
            @Override
            protected void done() {
                try {
                    BufferedImage img = get();
                    if (img == null) return;
                    cacheCapas.put(url, img);
                    // só aplica se ainda for a capa da faixa atual
                    if (indice >= 0 && url.equals(fila.get(indice).capaUrl)) {
                        vinil.setCapa(img);
                    }
                } catch (Exception ex) {
                    System.err.println("Não foi possível carregar a capa: " + ex.getMessage());
                }
            }
        }.execute();
    }
 
    // recorta o centro quadrado e reduz uma vez só (o disco gira bem mais leve com isso)
    private static BufferedImage prepararCapa(BufferedImage src, int lado) {
        int s = Math.min(src.getWidth(), src.getHeight());
        int x = (src.getWidth() - s) / 2;
        int y = (src.getHeight() - s) / 2;
        BufferedImage out = new BufferedImage(lado, lado, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = out.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.drawImage(src, 0, 0, lado, lado, x, y, x + s, y + s, null);
        g.dispose();
        return out;
    }
    
    public void receberMusica(
        Faixa faixa,
        List<Faixa> resultados) {

    if (faixa == null) return;

    this.fila = new ArrayList<>(resultados);

    int posicao = -1;

    for (int i = 0; i < fila.size(); i++) {
        if (fila.get(i).id == faixa.id) {
            posicao = i;
            break;
        }
    }

    if (posicao >= 0) {
        selecionar(posicao, false);
    }
    }
    
    public void reproduzirMusica(
        Faixa faixa,
        List<Faixa> resultados) {

    if (faixa == null) return;

    this.fila = new ArrayList<>(resultados);

    for (int i = 0; i < fila.size(); i++) {
        if (fila.get(i).id == faixa.id) {
            selecionar(i, true);
            return;
        }
    }
}
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        btnVoltar = new javax.swing.JButton();
        btnPlay = new javax.swing.JButton();
        btnAvancar = new javax.swing.JButton();
        jSTempo = new javax.swing.JSlider();
        jPanel1 = new javax.swing.JPanel();
        lblTitulo = new javax.swing.JLabel();
        lblArtista = new javax.swing.JLabel();
        lblTempo = new javax.swing.JLabel();

        setPreferredSize(new java.awt.Dimension(420, 700));

        btnVoltar.setText("Voltar");

        btnPlay.setText("Play");

        btnAvancar.setText("Avançar");

        jPanel1.setAlignmentX(0.0F);
        jPanel1.setAlignmentY(0.0F);
        jPanel1.setPreferredSize(new java.awt.Dimension(420, 700));

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 327, Short.MAX_VALUE)
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 310, Short.MAX_VALUE)
        );

        lblTitulo.setText("Nada a Mostrar");

        lblArtista.setText("Nada a Mostrar");

        lblTempo.setText("Nada a Mostrar");

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addGap(24, 24, 24)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(lblArtista)
                            .addComponent(lblTitulo, javax.swing.GroupLayout.PREFERRED_SIZE, 93, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(lblTempo)
                            .addComponent(jSTempo, javax.swing.GroupLayout.PREFERRED_SIZE, 360, javax.swing.GroupLayout.PREFERRED_SIZE)))
                    .addGroup(layout.createSequentialGroup()
                        .addGap(32, 32, 32)
                        .addComponent(btnVoltar, javax.swing.GroupLayout.PREFERRED_SIZE, 90, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(40, 40, 40)
                        .addComponent(btnPlay, javax.swing.GroupLayout.PREFERRED_SIZE, 90, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(29, 29, 29)
                        .addComponent(btnAvancar, javax.swing.GroupLayout.PREFERRED_SIZE, 90, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(layout.createSequentialGroup()
                        .addGap(42, 42, 42)
                        .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, 327, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap(36, Short.MAX_VALUE))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGap(21, 21, 21)
                .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, 310, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(lblTitulo)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(lblArtista)
                .addGap(29, 29, 29)
                .addComponent(lblTempo)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jSTempo, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(34, 34, 34)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(btnPlay)
                    .addComponent(btnAvancar)
                    .addComponent(btnVoltar))
                .addContainerGap(191, Short.MAX_VALUE))
        );
    }// </editor-fold>//GEN-END:initComponents


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btnAvancar;
    private javax.swing.JButton btnPlay;
    private javax.swing.JButton btnVoltar;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JSlider jSTempo;
    private javax.swing.JLabel lblArtista;
    private javax.swing.JLabel lblTempo;
    private javax.swing.JLabel lblTitulo;
    // End of variables declaration//GEN-END:variables
}
