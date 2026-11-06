package com.rpg.concurrency;

import com.rpg.entities.EstadoEfecto;
import com.rpg.entities.Personaje;

public class EfectoContinuoTask implements Runnable {

    private final Personaje objetivo;
    private final EstadoEfecto efecto;
    private final long intervaloMs;

    public EfectoContinuoTask(Personaje objetivo, EstadoEfecto efecto, long intervaloMs) {
        if (intervaloMs <= 0) {
            throw new IllegalArgumentException("El intervalo debe ser positivo");
        }
        this.objetivo = objetivo;
        this.efecto = efecto;
        this.intervaloMs = intervaloMs;
    }

    public String getNombre() {
        return efecto.getNombre();
    }

    @Override
    public void run() {
        try {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    Thread.sleep(intervaloMs);
                } catch (InterruptedException e) {
                    objetivo.limpiarEfecto(efecto);
                    objetivo.notificarEfecto(objetivo.getNombre() + ": " + efecto.getNombre() + " se disipa");
                    Thread.currentThread().interrupt();
                    break;
                }
                String mensaje = objetivo.aplicarTickEfecto(efecto);
                if (mensaje == null) {
                    break;
                }
                objetivo.notificarEfecto(mensaje);
                if (efecto.haTerminado()) {
                    objetivo.notificarEfecto(objetivo.getNombre() + ": termina " + efecto.getNombre());
                    break;
                }
                if (!objetivo.estaVivo()) {
                    break;
                }
            }
        } finally {
            objetivo.limpiarEfecto(efecto);
        }
    }
}
