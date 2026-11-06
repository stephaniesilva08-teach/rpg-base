package com.rpg.entities;

import com.rpg.entities.enums.TipoEfecto;

import java.io.Serial;
import java.io.Serializable;


public class EstadoEfecto implements Serializable {

    
    
    @Serial
    private static final long serialVersionUID = 1L;

    private final String nombre;
    private final TipoEfecto tipo;
    private final int valorPorTick;
    
    private volatile int turnosRestantes;

    public EstadoEfecto(String nombre, TipoEfecto tipo, int valorPorTick, int duracion) {
        this.nombre = nombre;
        this.tipo = tipo;
        this.valorPorTick = valorPorTick;
        this.turnosRestantes = duracion;
    }

    public EstadoEfecto copia() {
        return new EstadoEfecto(nombre, tipo, valorPorTick, turnosRestantes);
    }

    
    public void consumirTurno() {
        turnosRestantes--;
    }

    public boolean haTerminado() {
        return turnosRestantes <= 0;
    }

    public boolean esDanio() {
        return tipo == TipoEfecto.DANIO_CONTINUO;
    }

    public String getNombre() {
        return nombre;
    }

    public int getValorPorTick() {
        return valorPorTick;
    }

    public int getTurnosRestantes() {
        return turnosRestantes;
    }
}
