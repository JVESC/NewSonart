/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/GUIForms/JPanel.java to edit this template
 */
package Telas_Painel;
import Dados.Conexao;
import Dados.Faixa;
import Dados.FaixaDAO;
import Telas_Player.Tela_Play;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.net.URI;
import java.util.List;
import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
/**
 *
 * @author aluno
 */
public class Tela_Pesquisa extends javax.swing.JPanel {
private Tela_Play player;
private JTextField campoPesquisa;
private JPanel painelResultados;
private JLabel lblStatus;
    /**
     * Creates new form Tela_Pesquisa
     */
   public Tela_Pesquisa() {
    this(null);
}

    public Tela_Pesquisa(Tela_Play player) {
    initComponents();

    this.player = player;

    configurarTela();
}
    
    private void configurarTela() {
    removeAll();
    setLayout(new BorderLayout(10, 10));
    setBorder(new EmptyBorder(20, 20, 20, 20));

    // Campo de pesquisa
    JPanel topo = new JPanel(new BorderLayout(10, 0));

    campoPesquisa = new JTextField();
    campoPesquisa.setFont(new Font("Arial", Font.PLAIN, 16));
    campoPesquisa.setPreferredSize(new Dimension(300, 40));

    JButton btnPesquisar = new JButton("Pesquisar");

    topo.add(campoPesquisa, BorderLayout.CENTER);
    topo.add(btnPesquisar, BorderLayout.EAST);

    add(topo, BorderLayout.NORTH);

    // Área dos resultados
    painelResultados = new JPanel();
    painelResultados.setLayout(
        new BoxLayout(painelResultados, BoxLayout.Y_AXIS)
    );

    JScrollPane scroll = new JScrollPane(painelResultados);
    scroll.setBorder(null);

    add(scroll, BorderLayout.CENTER);

    lblStatus = new JLabel("Pesquise uma música.");
    add(lblStatus, BorderLayout.SOUTH);

    btnPesquisar.addActionListener(e -> pesquisar());
    campoPesquisa.addActionListener(e -> pesquisar());
    }
    
    private void pesquisar() {

    String termo = campoPesquisa.getText().trim();

    if (termo.isEmpty()) {
        return;
    }

    painelResultados.removeAll();
    lblStatus.setText("Pesquisando...");

    painelResultados.revalidate();
    painelResultados.repaint();

    new SwingWorker<List<Faixa>, Void>() {

        @Override
        protected List<Faixa> doInBackground() throws Exception {

            try (java.sql.Connection c = Conexao.abrir()) {
                return FaixaDAO.buscar(c, termo);
            }
        }

        @Override
        protected void done() {

            try {
                List<Faixa> resultados = get();

                painelResultados.removeAll();

                if (resultados.isEmpty()) {
                    lblStatus.setText("Nenhuma música encontrada.");
                } else {

                    lblStatus.setText(
                        resultados.size() + " resultado(s)"
                    );

                    for (Faixa faixa : resultados) {
                        painelResultados.add(
                            criarResultado(faixa, resultados)
                        );

                        painelResultados.add(
                            Box.createVerticalStrut(8)
                        );
                    }
                }

                painelResultados.revalidate();
                painelResultados.repaint();

            } catch (Exception ex) {
                lblStatus.setText("Erro ao pesquisar músicas.");
                ex.printStackTrace();
            }
        }

    }.execute();
    }
    
    private JPanel criarResultado(
        Faixa faixa,
        List<Faixa> resultados) {

    JPanel item = new JPanel(new BorderLayout(12, 0));

    item.setBorder(
        BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(
                new Color(65, 65, 65)
            ),
            new EmptyBorder(8, 8, 8, 8)
        )
    );

    item.setMaximumSize(
        new Dimension(Integer.MAX_VALUE, 76)
    );

    // Miniatura da capa
    JLabel capa = new JLabel();
    capa.setPreferredSize(new Dimension(55, 55));

    capa.setHorizontalAlignment(JLabel.CENTER);

    carregarMiniCapa(faixa.capaUrl, capa);

    // Informações
    JPanel informacoes = new JPanel();
    informacoes.setLayout(
        new BoxLayout(informacoes, BoxLayout.Y_AXIS)
    );

    JLabel titulo = new JLabel(faixa.titulo);
    titulo.setFont(new Font("Arial", Font.BOLD, 14));

    JLabel artista = new JLabel(faixa.artista);
    artista.setFont(new Font("Arial", Font.PLAIN, 12));

    informacoes.add(titulo);
    informacoes.add(Box.createVerticalStrut(5));
    informacoes.add(artista);

    // Botão Play
   
    JButton btnPlay = new JButton("▶");

    btnPlay.setToolTipText("Reproduzir música");

    btnPlay.addActionListener(e -> {
        player.reproduzirMusica(faixa, resultados);
    });

    item.add(capa, BorderLayout.WEST);
    item.add(informacoes, BorderLayout.CENTER);
    item.add(btnPlay, BorderLayout.EAST);

    return item;
    }
    
    private void carregarMiniCapa(
        String url,
        JLabel label) {

    if (url == null || url.isBlank()) {
        label.setText("♪");
        return;
    }

    new SwingWorker<ImageIcon, Void>() {

        @Override
        protected ImageIcon doInBackground() throws Exception {

            BufferedImage imagem = ImageIO.read(
                URI.create(url).toURL()
            );

            if (imagem == null) {
                return null;
            }

            Image reduzida = imagem.getScaledInstance(
                55, 55,
                Image.SCALE_SMOOTH
            );

            return new ImageIcon(reduzida);
        }

        @Override
        protected void done() {

            try {
                ImageIcon icone = get();

                if (icone != null) {
                    label.setIcon(icone);
                    label.setText("");
                } else {
                    label.setText("♪");
                }

            } catch (Exception ex) {
                label.setText("♪");
            }
        }

    }.execute();
    }
    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jTextField1 = new javax.swing.JTextField();
        jBPesq = new javax.swing.JButton();

        setPreferredSize(new java.awt.Dimension(643, 680));

        jTextField1.setText("jTextField1");

        jBPesq.setText("Pesquisar");

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGap(31, 31, 31)
                .addComponent(jTextField1, javax.swing.GroupLayout.DEFAULT_SIZE, 457, Short.MAX_VALUE)
                .addGap(18, 18, 18)
                .addComponent(jBPesq)
                .addGap(45, 45, 45))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGap(38, 38, 38)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jTextField1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jBPesq))
                .addContainerGap(614, Short.MAX_VALUE))
        );
    }// </editor-fold>//GEN-END:initComponents


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton jBPesq;
    private javax.swing.JTextField jTextField1;
    // End of variables declaration//GEN-END:variables
}
