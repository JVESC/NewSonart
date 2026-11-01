package Telas_Player;
import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.awt.image.BufferedImage;
/**
 *
 * @author jvitor
 */
public class DiscoVinil extends javax.swing.JPanel {

    // ajustes (mexa à vontade)
    private static final double VEL_MAX = Math.toRadians(2.5);  // graus por frame
    private static final double SUAVIDADE_AGULHA = 0.07;
    private static final double ACELERACAO = 0.04;
    private static final double FREADA = 0.02;                  // menor = mais inércia
    private static final double RAIO_INICIO = 0.95;             // % do raio onde a agulha pousa
    private static final double RAIO_FIM = 0.40;                // % do raio perto do rótulo
    private static final double FOLGA_REPOUSO = 18;             // graus fora do disco
    private static final double ESCALA_DISCO = 0.95;   // 1.0 = máximo que cabe
    
    private BufferedImage vinilPng, capa;
    private boolean tocando = false;
    private double progresso = 0;        // 0 a 1
    private double anguloDisco = 0;      // radianos
    private double velocidade = 0;       // radianos por frame
    private double anguloAgulha;         // graus, valor atual
    private double angRepouso, angInicio, angFim;

    // geometria (recalculada quando o painel muda de tamanho)
    private int tam, cx, cy, pivoX, pivoY, comprimento, tamCache = -1;

    private final Timer timer;
    
    public DiscoVinil() {
        initComponents();
        setOpaque(false);
        setPreferredSize(new Dimension(420, 340));
        try {
            vinilPng = ImageIO.read(getClass().getResource("/imagens/vinil.png"));
        } catch (Exception e) { e.printStackTrace(); }

        timer = new Timer(16, e -> atualizar());
    }
    
    // ---------- API pública ----------
    public void setCapa(BufferedImage c) { capa = c; repaint(); }

    public void setProgresso(double p) {         // chame com valorAtual / duracao
        progresso = Math.max(0, Math.min(1, p));
        if (!timer.isRunning()) repaint();
    }

    public void tocar()  { tocando = true;  if (!timer.isRunning()) timer.start(); }
    public void pausar() { tocando = false; if (!timer.isRunning()) timer.start(); }

    // ---------- lógica de animação ----------
    private void atualizar() {
        calcularGeometria();

        // 1) agulha
        double alvoAgulha = tocando
                ? angInicio + (angFim - angInicio) * progresso
                : angRepouso;
        anguloAgulha += (alvoAgulha - anguloAgulha) * SUAVIDADE_AGULHA;

        // 2) disco só gira de verdade quando a agulha encostou
        boolean naPista = anguloAgulha >= angInicio - 1.0;
        double alvoVel = (tocando && naPista) ? VEL_MAX : 0;
        double fator = alvoVel > velocidade ? ACELERACAO : FREADA;
        velocidade += (alvoVel - velocidade) * fator;
        anguloDisco += velocidade;

        // 3) parou tudo: desliga o timer para não gastar CPU
        if (!tocando && velocidade < 0.0005 && Math.abs(anguloAgulha - angRepouso) < 0.05) {
            velocidade = 0;
            anguloAgulha = angRepouso;
            timer.stop();
        }
        repaint();
    }

    private void calcularGeometria() {
        int w = getWidth(), h = getHeight();
        int chave = w * 10000 + h;
        if (chave == tamCache) return;
        tamCache = chave;

        tam = (int) (Math.min(h, w / 1.3) * ESCALA_DISCO);
        int larguraTotal = (int) (tam * 1.24) + 16;       // disco + braço
        cx = (w - larguraTotal) / 2 + tam / 2;
        cy = h / 2;
        pivoX = cx + (int) (tam * 0.74);
        pivoY = cy - (int) (tam * 0.46);
        comprimento = (int) (tam * 0.78);

        angInicio = anguloParaRaio(tam / 2.0 * RAIO_INICIO);
        angFim = anguloParaRaio(tam / 2.0 * RAIO_FIM);
        angRepouso = angInicio - FOLGA_REPOUSO;
        anguloAgulha = Math.max(anguloAgulha, angRepouso);   // evita salto no primeiro frame
        if (!timer.isRunning() && !tocando) anguloAgulha = angRepouso;
    }

    // acha o ângulo em que a ponta da agulha fica a "raio" pixels do centro do disco
    private double anguloParaRaio(double raio) {
        for (double a = 0; a < 90; a += 0.05) {
            double r = Math.toRadians(a);
            double tx = pivoX - comprimento * Math.sin(r);
            double ty = pivoY + comprimento * Math.cos(r);
            if (Math.hypot(tx - cx, ty - cy) <= raio) return a;
        }
        return 45;
    }

    // ---------- desenho ----------
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (vinilPng == null) return;
        calcularGeometria();

        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);

        // disco + capa (giram juntos)
        Graphics2D gd = (Graphics2D) g2.create();
        gd.rotate(anguloDisco, cx, cy);
        if (capa != null) {
            int tc = (int) (tam * 0.48);
            gd.setClip(new Ellipse2D.Double(cx - tc / 2.0, cy - tc / 2.0, tc, tc));
            gd.drawImage(capa, cx - tc / 2, cy - tc / 2, tc, tc, null);
            gd.setClip(null);
        }
        gd.drawImage(vinilPng, cx - tam / 2, cy - tam / 2, tam, tam, null);
        gd.dispose();

        // agulha (não gira com o disco)
        double r = Math.toRadians(anguloAgulha);
        double tx = pivoX - comprimento * Math.sin(r);
        double ty = pivoY + comprimento * Math.cos(r);
        double ux = (pivoX - tx) / comprimento, uy = (pivoY - ty) / comprimento;

        g2.setStroke(new BasicStroke(6, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.setColor(new Color(0x888780));
        g2.drawLine(pivoX, pivoY, (int) tx, (int) ty);

        g2.setStroke(new BasicStroke(13, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));   // cabeça
        g2.setColor(new Color(0x5F5E5A));
        g2.drawLine((int) (tx + ux * 28), (int) (ty + uy * 28), (int) tx, (int) ty);

        g2.setColor(new Color(0xB4B2A9));                                                    // base
        g2.fillOval(pivoX - 16, pivoY - 16, 32, 32);
        g2.setColor(new Color(0x5F5E5A));
        g2.fillOval(pivoX - 6, pivoY - 6, 12, 12);

        g2.dispose();
    }

    
    

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 400, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 300, Short.MAX_VALUE)
        );
    }// </editor-fold>//GEN-END:initComponents


    // Variables declaration - do not modify//GEN-BEGIN:variables
    // End of variables declaration//GEN-END:variables

}