package com.rpg.concurrency;

public class AutoGuardadoDaemon implements Runnable {

    private final long intervaloMs;
    private final Runnable guardado;

    public AutoGuardadoDaemon(long intervaloMs, Runnable guardado) {
        if (intervaloMs <= 0) {
            throw new IllegalArgumentException("El intervalo debe ser positivo");
        }
        this.intervaloMs = intervaloMs;
        this.guardado = guardado;
    }

    public static Thread iniciar(long intervaloMs, Runnable guardado) {
        Thread hilo = new Thread(new AutoGuardadoDaemon(intervaloMs, guardado), "auto-guardado");
        hilo.setDaemon(true);
        hilo.start();
        return hilo;
    }

    @Override
    public void run() {
        while (true) {
            try {
                Thread.sleep(intervaloMs);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
            try {
                guardado.run();
            } catch (RuntimeException e) {
                System.out.println("[Autoguardado] fallo inesperado: " + e);
            }
        }
    }
}
