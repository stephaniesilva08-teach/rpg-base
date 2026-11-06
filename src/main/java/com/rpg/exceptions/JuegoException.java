package com.rpg.exceptions;

import java.io.Serial;


public abstract class JuegoException extends Exception {

    @Serial
    private static final long serialVersionUID = 1L;

    protected JuegoException(String mensaje) {
        super(mensaje);
    }
}
