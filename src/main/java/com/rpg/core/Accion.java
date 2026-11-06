package com.rpg.core;

/** Acciones que el jugador puede elegir en su turno. */
public enum Accion {
    ATACAR,
    ESPECIAL,
    POCION,
    ESTADO,
    HUIR,
    /** No es algo que el jugador elija: el temporizador de turno llego a cero antes de que respondiera. */
    TIEMPO_AGOTADO
}
