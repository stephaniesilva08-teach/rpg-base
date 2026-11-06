package com.rpg.model;

/**
 * Entidad Heroe - POJO (Plain Old Java Object)
 * Representa un heroe del juego con atributos básicos.
 * Sigue el principio de encapsulamiento: atributos privados con getters y setters validados.
 */
public class Heroe {
    private int id;
    private String nombre;
    private String tipo;      // "guerrero" o "mago"
    private int vidaMaxima;
    private int nivel;

    /**
     * Constructor por defecto (requerido para JPA y serialización).
     */
    public Heroe() {
    }

    /**
     * Constructor con todos los atributos.
     */
    public Heroe(String nombre, String tipo, int vidaMaxima, int nivel) {
        setId(nombre, tipo, vidaMaxima, nivel);
    }

    /**
     * Establece los atributos del heroe con validación.
     */
    public void setId(String nombre, String tipo, int vidaMaxima, int nivel) {
        if (nombre == null || nombre.isBlank()) {
            throw new IllegalArgumentException("El nombre no puede estar vacio.");
        }
        if (tipo == null || !tipo.equals("guerrero") && !tipo.equals("mago")) {
            throw new IllegalArgumentException("Tipo invalido. Debe ser 'guerrero' o 'mago'.");
        }
        if (vidaMaxima <= 0) {
            throw new IllegalArgumentException("La vida maxima debe ser mayor a 0.");
        }
        if (nivel < 1) {
            throw new IllegalArgumentException("El nivel debe ser mayor o igual a 1.");
        }

        this.nombre = nombre;
        this.tipo = tipo;
        this.vidaMaxima = vidaMaxima;
        this.nivel = nivel;
    }

    // ============ GETTERS ============

    public int getId() {
        return id;
    }

    public String getNombre() {
        return nombre;
    }

    public String getTipo() {
        return tipo;
    }

    public int getVidaMaxima() {
        return vidaMaxima;
    }

    public int getNivel() {
        return nivel;
    }

    // ============ SETTERS ============

    public void setId(int id) {
        this.id = id;
    }

    public void setNombre(String nombre) {
        if (nombre == null || nombre.isBlank()) {
            throw new IllegalArgumentException("El nombre no puede estar vacio.");
        }
        this.nombre = nombre;
    }

    public void setTipo(String tipo) {
        if (tipo == null || !tipo.equals("guerrero") && !tipo.equals("mago")) {
            throw new IllegalArgumentException("Tipo invalido. Debe ser 'guerrero' o 'mago'.");
        }
        this.tipo = tipo;
    }

    public void setVidaMaxima(int vidaMaxima) {
        if (vidaMaxima <= 0) {
            throw new IllegalArgumentException("La vida maxima debe ser mayor a 0.");
        }
        this.vidaMaxima = vidaMaxima;
    }

    public void setNivel(int nivel) {
        if (nivel < 1) {
            throw new IllegalArgumentException("El nivel debe ser mayor o igual a 1.");
        }
        this.nivel = nivel;
    }

    /**
     * Devuelve una representacion en cadena del heroe.
     */
    @Override
    public String toString() {
        return "Heroe{" +
                "id=" + id +
                ", nombre='" + nombre + '\'' +
                ", tipo='" + tipo + '\'' +
                ", vidaMaxima=" + vidaMaxima +
                ", nivel=" + nivel +
                '}';
    }

    /**
     * Compara dos heroes por su identificador unico.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Heroe heroe = (Heroe) o;
        return id == heroe.id;
    }

    @Override
    public int hashCode() {
        return Integer.hashCode(id);
    }
}
