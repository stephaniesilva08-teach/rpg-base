package com.rpg.demo;

import java.io.File;
import java.io.FileOutputStream;
import java.io.NotSerializableException;
import java.io.ObjectOutputStream;
import java.io.Serializable;


public class DemoNotSerializableException {

    
    static class HijoNoSerializable {
        int valor = 42;
    }

    
    
    
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

        
        if (archivo.exists()) {
            archivo.delete();
        }
    }
}
