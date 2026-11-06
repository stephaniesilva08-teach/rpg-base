package com.rpg.exceptions;

import java.io.Serial;


public class CapacidadExcedidaException extends JuegoException {

    @Serial
    private static final long serialVersionUID = 1L;

    public CapacidadExcedidaException(int capacidad) {
        super("Inventario lleno (capacidad: " + capacidad + ")");
    }
}
