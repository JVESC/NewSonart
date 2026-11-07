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
    
    private static final String URL = "jdbc:postgresql://192.168.100.102:5432/Sonart";
    private static final String USUARIO = "casaos";
    private static final String SENHA = "casaos";
 
    public static Connection abrir() throws SQLException {
        return DriverManager.getConnection(URL, USUARIO, SENHA);
    }

}
