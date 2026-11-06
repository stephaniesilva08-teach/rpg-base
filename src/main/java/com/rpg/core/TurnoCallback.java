package com.rpg.core;

/** Aviso que recibe el juego cuando el temporizador de turno llega a cero. */
@FunctionalInterface
public interface TurnoCallback {

    /** Se ejecuta en el hilo del temporizador, no en el hilo principal. */
    void onTiempoAgotado();
}
