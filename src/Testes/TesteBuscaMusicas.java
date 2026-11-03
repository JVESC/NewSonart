/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package Testes;

import Dados.*;
import java.sql.*;


/**
 *
 * @author jvitor
 */
public class TesteBuscaMusicas {
    
    public static void main(String[] args) throws Exception {
    FonteMusica FonteMusica = new FonteMusica();
    MetadadosMusica metadadosMusica = new MetadadosMusica();
    MusicaDAO musicaDAO = new MusicaDAO();
    
    var lista = FonteMusica.buscar("Daft Punk", "Get Lucky");   // use uma música real
    if (lista.isEmpty()) {
        System.out.println("Nada encontrado. Veja as mensagens acima.");
        return;
    }
    lista.forEach(System.out::println);
    MetadadosMusica escolhida = lista.get(0);

   
    FonteMusica.completar(escolhida);
    System.out.println("Ano: " + escolhida.anoLancamento + " | Capa: " + escolhida.capa);
    System.out.println("Tem letra sincronizada? " + (escolhida.letraSincronizada != null));

    try (Connection c = DriverManager.getConnection(
            "jdbc:postgresql://localhost:5432/Sonart", "postgres", "admin")) {
        int id = MusicaDAO.inserir(c, escolhida, "ID_DO_ARQUIVO_NO_DRIVE");
        System.out.println("Gravada com id_musica = " + id);
    }
}
}
