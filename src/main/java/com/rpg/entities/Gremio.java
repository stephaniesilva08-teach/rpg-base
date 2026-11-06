package com.rpg.entities;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Agregacion: el gremio guarda referencias a personajes que existen por su cuenta. */
// Serializable: guarda la lista de Personaje, que tambien lo es. El ciclo Gremio <-> Personaje no es problema:
// ObjectOutputStream recuerda los objetos ya escritos y los reconstruye con las mismas referencias.
// TALLER ARCHIVOS: el gremio es parte de la partida serializada; el ciclo Gremio->Personaje se reconstruye al
// cargar una partida (ObjectOutputStream conserva las referencias entre objetos).
public class Gremio implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private final String nombre;
    private final List<Personaje> miembros = new ArrayList<>();

    public Gremio(String nombre) {
        this.nombre = nombre;
    }

    public void agregarMiembro(Personaje personaje) {
        if (personaje == null || miembros.contains(personaje)) {
            return;
        }
        miembros.add(personaje);
        if (personaje.getGremio() != this) {
            personaje.unirseAGremio(this);
        }
    }

    public void removerMiembro(Personaje personaje) {
        if (!miembros.remove(personaje)) {
            return;
        }
        if (personaje.getGremio() == this) {
            personaje.abandonarGremio();
        }
    }

    public String getNombre() {
        return nombre;
    }

    public List<Personaje> getMiembros() {
        return Collections.unmodifiableList(miembros);
    }
}
