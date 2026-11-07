package Testes;

import Drives.PlayerMusica;

public class TesteTocar {

    public static void main(String[] args) throws Exception {
        PlayerMusica p = new PlayerMusica();
        System.out.println("Solicitando reprodução...");
        p.tocar("14g-VL3okWWKVdY9_AKM8gcA6e7We8Vyx");
        System.out.println("Solicitação enviada.");
        p.setAoPronto(seg -> System.out.println("Pronto! Duração: " + seg + "s"));
        p.setAoErro(msg -> System.err.println("ERRO: " + msg));
        p.setAoTerminar(() -> {
            System.out.println("Terminou.");
            System.exit(0);
        });

        // ID de um arquivo do Drive (o que aparece no seu log de PENDENTES)
        p.tocar("14g-VL3okWWKVdY9_AKM8gcA6e7We8Vyx");
        p.setVolume(0.8);

        Thread.sleep(60_000);   // mantém o programa vivo enquanto toca
        System.exit(0);
    }
}