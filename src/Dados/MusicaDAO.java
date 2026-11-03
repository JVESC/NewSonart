/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package Dados;
import java.sql.*;

/**
 *
 * @author jvitor
 */
public class MusicaDAO {
    public static int inserir(Connection c, MetadadosMusica m, String idDrive) throws SQLException {
        c.setAutoCommit(false);
        try {
            // evita cadastrar a mesma musica duas vezes
            try (PreparedStatement ps = c.prepareStatement(
                    "SELECT 1 FROM musica WHERE caminho_arquivo = ?")) {
                ps.setString(1, idDrive);
                if (ps.executeQuery().next()) throw new SQLException("Essa música já está cadastrada.");
            }

            int idArtista = obterOuCriarArtista(c, m.artista);
            int idAlbum = obterOuCriarAlbum(c, m, idArtista);

            int idMusica;
            try (PreparedStatement ps = c.prepareStatement(
                    "INSERT INTO musica (nome, duracao_seg, letra, letra_sincronizada, caminho_arquivo, id_album) "
                  + "VALUES (?, ?, ?, ?, ?, ?) RETURNING id_musica")) {
                ps.setString(1, m.titulo);
                ps.setInt(2, m.duracaoSeg);
                ps.setString(3, m.letra);
                ps.setString(4, m.letraSincronizada);
                ps.setString(5, idDrive);
                ps.setInt(6, idAlbum);
                ResultSet rs = ps.executeQuery();
                rs.next();
                idMusica = rs.getInt(1);
            }

            try (PreparedStatement ps = c.prepareStatement(
                    "INSERT INTO artista_musica (id_artista, id_musica) VALUES (?, ?)")) {
                ps.setInt(1, idArtista);
                ps.setInt(2, idMusica);
                ps.executeUpdate();
            }

            c.commit();
            return idMusica;
        } catch (SQLException e) {
            c.rollback();
            throw e;
        } finally {
            c.setAutoCommit(true);
        }
    }

    private static int obterOuCriarArtista(Connection c, String nome) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("SELECT id_artista FROM artista WHERE nome = ?")) {
            ps.setString(1, nome);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt(1);
        }
        try (PreparedStatement ps = c.prepareStatement(
                "INSERT INTO artista (nome) VALUES (?) RETURNING id_artista")) {
            ps.setString(1, nome);
            ResultSet rs = ps.executeQuery();
            rs.next();
            return rs.getInt(1);
        }
    }

    private static int obterOuCriarAlbum(Connection c, MetadadosMusica m, int idArtista) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "SELECT id_album FROM album WHERE nome = ? AND id_artista = ?")) {
            ps.setString(1, m.album);
            ps.setInt(2, idArtista);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt(1);
        }
        try (PreparedStatement ps = c.prepareStatement(
                "INSERT INTO album (nome, capa, ano_lancamento, id_artista) VALUES (?, ?, ?, ?) RETURNING id_album")) {
            ps.setString(1, m.album);
            ps.setString(2, m.capa);
            ps.setInt(3, m.anoLancamento);
            ps.setInt(4, idArtista);
            ResultSet rs = ps.executeQuery();
            rs.next();
            return rs.getInt(1);
        }
    }
}
