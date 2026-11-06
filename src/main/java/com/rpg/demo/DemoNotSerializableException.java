package com.rpg.demo;

import java.io.File;
import java.io.FileOutputStream;
import java.io.NotSerializableException;
import java.io.ObjectOutputStream;
import java.io.Serializable;

/**
 * Prueba de la seccion 5 del "Checklist del taller de archivos".
 *
 * Punto: "Provocar y resolver una NotSerializableException por un objeto anidado que no es Serializable".
 *
 * Un objeto que implementa Serializable, pero que contiene un campo anidado que NO lo es,
 * lanza NotSerializableException al serializarse. Esto demuestra que el algoritmo de
 * serializacion revisa completo el grafo de objetos, no solo la superficie del contenedor.
 */
public class DemoNotSerializableException {

    // Campo que NO es Serializable: hace que el wrapper falle la serializacion.
    static class HijoNoSerializable {
        int valor = 42;
    }

    // Wrapper que SI es Serializable, pero carga un HijoNoSerializable anidado.
    // Al intentar serializar un WrapperSerializable, el ObjectOutputStream recorre el
    // grafo y encuentra el HijoNoSerializable, que no es Serializable, y lanza la excepcion.
    static class WrapperSerializable implements Serializable {
        int id;
        HijoNoSerializable hijo;

        public WrapperSerializable(int id) {
            this.id = id;
            this.hijo = new HijoNoSerializable();
        }
    }

    public static void main(String[] args) throws Exception {
        WrapperSerializable w = new WrapperSerializable(1);
        File archivo = new File("demo-no-serializable.dat");
        boolean lanzada = false;

        try {
            try (ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(archivo))) {
                out.writeObject(w);
            }
        } catch (NotSerializableException e) {
            lanzada = true;
            System.out.println("[NOTSerializable] EXITOSA: excepcion lanzada como se esperaba.");
            System.out.println("  mensaje: " + e.getMessage());
            System.out.println("  clase que fallo: " + e.getClass().getName());
            System.out.println("  resolucion: hacer Serializable el campo anidado, o marcarlo transient.");
        }

        if (!lanzada) {
            System.out.println("[NOTSerializable] ERROR: la excepcion NO se lanzo (el objeto parecía serializable).");
            System.exit(1);
        }

        // Limpieza del archivo (puede quedar un header escrito antes de fallar).
        if (archivo.exists()) {
            archivo.delete();
        }
    }
}
