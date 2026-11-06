package com.rpg.entities;

import com.rpg.entities.enums.TipoEfecto;

import java.io.Serial;
import java.io.Serializable;

/** Efecto periodico (DoT o HoT): cambia la vida un valor fijo en cada tick durante cierta duracion. */
public class EstadoEfecto implements Serializable {

    // Identifica la version de la clase en el archivo guardado: si la clase cambia de forma incompatible hay que
    // subir este numero, y asi un .dat viejo falla con InvalidClassException en vez de cargar datos mal interpretados.
    @Serial
    private static final long serialVersionUID = 1L;

    private final String nombre;
    private final TipoEfecto tipo;
    private final int valorPorTick;
    // volatile: lo decrementa el hilo del efecto y lo lee el hilo principal (por ejemplo al mostrar el estado).
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

    /** Solo lo llama el hilo propio del efecto (a traves de Personaje.aplicarTickEfecto). */
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
