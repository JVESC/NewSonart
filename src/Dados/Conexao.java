/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package Dados;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 *
 * @author jvitor
 */
public class Conexao {
    
    private static final String URL = "jdbc:postgresql://localhost:5432/Sonart";
    private static final String USUARIO = "postgres";
    private static final String SENHA = "admin";
 
    public static Connection abrir() throws SQLException {
        return DriverManager.getConnection(URL, USUARIO, SENHA);
    }

}
