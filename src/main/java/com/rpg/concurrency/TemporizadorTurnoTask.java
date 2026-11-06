package com.rpg.concurrency;

import com.rpg.core.TurnoCallback;

public class TemporizadorTurnoTask implements Runnable {

    private static final long UN_SEGUNDO_MS = 1000L;
    private static final int SEGUNDOS_DE_AVISO = 5;

    private final int segundos;
    private final TurnoCallback callback;

    public TemporizadorTurnoTask(int segundos, TurnoCallback callback) {
        if (segundos <= 0) {
            throw new IllegalArgumentException("La duracion debe ser positiva");
        }
        if (callback == null) {
            throw new IllegalArgumentException("El callback no puede ser null");
        }
        this.segundos = segundos;
        this.callback = callback;
    }

    @Override
    public void run() {
        for (int restante = segundos; restante > 0; restante--) {
            if (restante == SEGUNDOS_DE_AVISO && segundos > SEGUNDOS_DE_AVISO) {
                System.out.printf("%n[Quedan %d segundos para elegir]%n", restante);
            }
            try {
                Thread.sleep(UN_SEGUNDO_MS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
        }
        callback.onTiempoAgotado();
    }
}
