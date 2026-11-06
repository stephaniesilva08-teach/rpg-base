package com.rpg.demo;

import com.rpg.entities.Guerrero;
import com.rpg.entities.Gremio;
import com.rpg.entities.Personaje;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.List;

/**
 * Prueba de la seccion 5 del "Checklist del taller de archivos".
 *
 * Punto: "Comprobar que despues de cargar, el Gremio y sus Personaje siguen relacionados
 * (el ciclo Personaje - Gremio se serializa bien)".
 *
 * Escribe un Gremio con sus Personajes, lo reconstruye desde el archivo y comprueba que
 * el enlace bidireccional se mantiene: cada Personaje recuperado sigue apuntando al mismo
 * Gremio que el que lo contiene (no a null ni a un Gremio distinto).
 */
public class DemoCicloGremio {

    public static void main(String[] args) throws Exception {
        Gremio gremio = new Gremio("GremioDemo");
        Guerrero a = new Guerrero("Ares");
        Guerrero b = new Guerrero("Borgia");
        gremio.agregarMiembro(a);
        gremio.agregarMiembro(b);

        File archivo = new File("demo-ciclo.dat");

        // --- Escritura ---
        try (ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(archivo))) {
            out.writeObject(gremio);
        }

        // --- Lectura ---
        Gremio cargado;
        try (ObjectInputStream in = new ObjectInputStream(new FileInputStream(archivo))) {
            cargado = (Gremio) in.readObject();
        }

        List<Personaje> miembros = cargado.getMiembros();

        // Estructura: los dos personajes estan recuperados y el gremio se ha reconstruido.
        boolean estructuraOk = miembros.size() == 2
                && miembros.get(0).getNombre().equals("Ares")
                && miembros.get(1).getNombre().equals("Borgia");

        // Ciclo: cada Personaje recuperado sigue apuntando al mismo Gremio que el que lo contiene.
        boolean cicloOk = estructuraOk
                && miembros.get(0).getGremio() == cargado
                && miembros.get(1).getGremio() == cargado;

        System.out.println("[Ciclo Gremio<->Personaje] estructura recuperada: " + estructuraOk);
        System.out.println("[Ciclo Gremio<->Personaje] ciclo preservado: " + cicloOk);
        System.out.println("  Gremio cargado: " + cargado.getNombre() + " | miembros: " + miembros.size());
        System.out.println("  Ares.getGremio() == Gremio cargado -> " + (miembros.get(0).getGremio() == cargado));
        System.out.println("  Borgia.getGremio() == Gremio cargado -> " + (miembros.get(1).getGremio() == cargado));

        // Cierre limpio del archivo de prueba.
        if (archivo.exists()) {
            archivo.delete();
        }

        if (!estructuraOk || !cicloOk) {
            System.exit(1);
        }
    }
}
