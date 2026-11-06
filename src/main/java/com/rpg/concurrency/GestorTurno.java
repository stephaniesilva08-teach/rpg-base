package com.rpg.concurrency;

import com.rpg.core.TurnoCallback;

public class GestorTurno {

    private Thread hilo;

    public void iniciar(int segundos, TurnoCallback callback) {
        cancelar();
        hilo = new Thread(new TemporizadorTurnoTask(segundos, callback), "temporizador-turno");
        hilo.start();
    }

    public void cancelar() {
        if (hilo == null) {
            return;
        }
        hilo.interrupt();
        try {
            hilo.join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        hilo = null;
    }
}
