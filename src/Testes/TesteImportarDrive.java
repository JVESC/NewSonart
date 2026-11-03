package Testes;

import Dados.Conexao;
import Dados.ImportadorDrive;
import java.sql.Connection;

public class TesteImportarDrive {

    // ID da PASTA no Drive: é a parte depois de /folders/ no link da pasta
    private static final String PASTA_ID = "1uylWBSqIMxU6YyHgNXMd6v97TKx37M4S";

    public static void main(String[] args) throws Exception {
        try (Connection c = Conexao.abrir()) {
            ImportadorDrive.Resultado r = ImportadorDrive.importar(c, PASTA_ID, System.out::println);

            if (!r.pendentes.isEmpty()) {
                System.out.println("\n=== PENDENTES (cadastrar manualmente) ===");
                r.pendentes.forEach(System.out::println);
            }
            if (!r.naoSuportados.isEmpty()) {
                System.out.println("\n=== FORMATO NÃO SUPORTADO (converta para MP3 ou M4A) ===");
                r.naoSuportados.forEach(System.out::println);
            }
            if (!r.erros.isEmpty()) {
                System.out.println("\n=== ERROS ===");
                r.erros.forEach(System.out::println);
            }
        }
    }
}