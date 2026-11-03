/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package Dados;

/**
 *
 * @author jvitor
 */

public class MetadadosMusica {
    public String titulo, artista, album, capa;
    public String letra, letraSincronizada;
    public int duracaoSeg, anoLancamento;
    public long idAlbumDeezer;

    @Override public String toString() {          // aparece na lista de escolha
        return artista + " - " + titulo + "  [" + album + "]  " + duracaoSeg + "s";
    }
}
