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


public class DemoCicloGremio {

    public static void main(String[] args) throws Exception {
        Gremio gremio = new Gremio("GremioDemo");
        Guerrero a = new Guerrero("Ares");
        Guerrero b = new Guerrero("Borgia");
        gremio.agregarMiembro(a);
        gremio.agregarMiembro(b);

        File archivo = new File("demo-ciclo.dat");

        
        try (ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(archivo))) {
            out.writeObject(gremio);
        }

        
        Gremio cargado;
        try (ObjectInputStream in = new ObjectInputStream(new FileInputStream(archivo))) {
            cargado = (Gremio) in.readObject();
        }

        List<Personaje> miembros = cargado.getMiembros();

        
        boolean estructuraOk = miembros.size() == 2
                && miembros.get(0).getNombre().equals("Ares")
                && miembros.get(1).getNombre().equals("Borgia");

        
        boolean cicloOk = estructuraOk
                && miembros.get(0).getGremio() == cargado
                && miembros.get(1).getGremio() == cargado;

        System.out.println("[Ciclo Gremio<->Personaje] estructura recuperada: " + estructuraOk);
        System.out.println("[Ciclo Gremio<->Personaje] ciclo preservado: " + cicloOk);
        System.out.println("  Gremio cargado: " + cargado.getNombre() + " | miembros: " + miembros.size());
        System.out.println("  Ares.getGremio() == Gremio cargado -> " + (miembros.get(0).getGremio() == cargado));
        System.out.println("  Borgia.getGremio() == Gremio cargado -> " + (miembros.get(1).getGremio() == cargado));

        
        if (archivo.exists()) {
            archivo.delete();
        }

        if (!estructuraOk || !cicloOk) {
            System.exit(1);
        }
    }
}
