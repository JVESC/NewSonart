/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package Dados;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author jvitor
 */
public class FaixaDAO {
        /** Todas as músicas cadastradas, na ordem de cadastro. */
    public static List<Faixa> listarTodas(Connection c) throws SQLException {
        String sql =
                "SELECT m.id_musica, m.nome, m.duracao_seg, m.caminho_arquivo, m.letra_sincronizada, "
              + "       al.nome AS album, al.capa, ar.nome AS artista "
              + "FROM musica m "
              + "JOIN album al ON al.id_album = m.id_album "
              + "JOIN artista ar ON ar.id_artista = al.id_artista "
              + "ORDER BY m.id_musica";
 
        List<Faixa> lista = new ArrayList<>();
        try (PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Faixa f = new Faixa();
                f.id = rs.getInt("id_musica");
                f.titulo = rs.getString("nome");
                f.duracaoSeg = rs.getInt("duracao_seg");
                f.idDrive = rs.getString("caminho_arquivo");
                f.letraSincronizada = rs.getString("letra_sincronizada");
                f.album = rs.getString("album");
                f.capaUrl = rs.getString("capa");
                f.artista = rs.getString("artista");
                lista.add(f);
            }
        }
        return lista;
    }
    
    public static List<Faixa> buscar(Connection c, String termo)
        throws SQLException {

    String sql =
        "SELECT m.id_musica, m.nome, m.duracao_seg, " +
        "m.caminho_arquivo, m.letra_sincronizada, " +
        "al.nome AS album, al.capa, ar.nome AS artista " +
        "FROM musica m " +
        "JOIN album al ON al.id_album = m.id_album " +
        "JOIN artista ar ON ar.id_artista = al.id_artista " +
        "WHERE m.nome ILIKE ? " +
        "OR ar.nome ILIKE ? " +
        "OR al.nome ILIKE ? " +
        "ORDER BY m.nome";

    List<Faixa> resultados = new ArrayList<>();

    try (PreparedStatement ps = c.prepareStatement(sql)) {

        String busca = "%" + termo.trim() + "%";

        ps.setString(1, busca);
        ps.setString(2, busca);
        ps.setString(3, busca);

        try (ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                Faixa f = new Faixa();

                f.id = rs.getInt("id_musica");
                f.titulo = rs.getString("nome");
                f.artista = rs.getString("artista");
                f.album = rs.getString("album");
                f.capaUrl = rs.getString("capa");
                f.idDrive = rs.getString("caminho_arquivo");
                f.letraSincronizada =
                    rs.getString("letra_sincronizada");
                f.duracaoSeg = rs.getInt("duracao_seg");

                resultados.add(f);
            }
        }
    }

    return resultados;
}

}
