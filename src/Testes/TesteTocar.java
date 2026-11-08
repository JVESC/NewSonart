
package Testes;

import Drives.PlayerMusica;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class TesteTocar {

    public static void main(String[] args) throws Exception {

        PlayerMusica p = new PlayerMusica();
        CountDownLatch fim = new CountDownLatch(1);

        p.setVolume(0.8);

        p.setAoPronto(seg ->
            System.out.println("Pronto! Duração: " + seg + "s")
        );

        p.setAoErro(msg -> {
            System.err.println("ERRO: " + msg);
            fim.countDown();
        });

        p.setAoTerminar(() -> {
            System.out.println("Música terminou.");
            fim.countDown();
        });

        System.out.println("Solicitando reprodução...");

        p.tocar("14g-VL3okWWKVdY9_AKM8gcA6e7We8Vyx");

        System.out.println("Aguardando o término da música...");

        boolean terminou = fim.await(10, TimeUnit.MINUTES);

        if (!terminou) {
            System.out.println("Tempo limite do teste atingido.");
        }

        System.exit(0);
    }
}
