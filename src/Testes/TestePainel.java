package Testes;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;

public class TestePainel {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame f = new JFrame("Teste");
            f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            f.add(new Telas_Player.Tela_Play());   // troque pelo painel que quiser ver
            f.setSize(500, 700);
            f.setLocationRelativeTo(null);
            f.setVisible(true);
        });
    }
}