/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package Testes;
import Dados.Conexao;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
/**
 *
 * @author aluno
 */
public class TesteConexaoBanco {
    public static void main(String[] args) {

        System.out.println("===== TESTE DE CONEXAO SONART =====");

        try (Connection conexao = Conexao.abrir()) {

            System.out.println("CONEXAO ESTABELECIDA!");
            System.out.println("Banco: " 
                    + conexao.getCatalog());
            System.out.println("Usuario: "
                    + conexao.getMetaData().getUserName());

            String sql = "SELECT COUNT(*) AS total FROM musica";

            try (Statement stmt = conexao.createStatement();
                 ResultSet resultado = stmt.executeQuery(sql)) {

                if (resultado.next()) {
                    System.out.println("Musicas cadastradas: "
                            + resultado.getInt("total"));
                }
            }

            System.out.println("TESTE CONCLUIDO COM SUCESSO!");

        } catch (Exception e) {

            System.out.println("ERRO NA CONEXAO!");
            System.out.println("Motivo: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
