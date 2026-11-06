package com.rpg.persistencia;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;


public final class LogCombateArchivo {

    private LogCombateArchivo() {
    }

    
    public static void guardar(File archivo, String texto, boolean agregar) throws IOException {
        ArchivosPartida.asegurarCarpetaPadre(archivo);
        try (BufferedWriter escritor = new BufferedWriter(new FileWriter(archivo, StandardCharsets.UTF_8, agregar))) {
            for (String linea : (Iterable<String>) texto.lines()::iterator) {
                escritor.write(linea);
                escritor.newLine(); 
            }
            escritor.flush(); 
        }
    }

    
    public static List<String> leer(File archivo) throws IOException {
        List<String> lineas = new ArrayList<>();
        try (BufferedReader lector = new BufferedReader(new FileReader(archivo, StandardCharsets.UTF_8))) {
            String linea;
            while ((linea = lector.readLine()) != null) {
                lineas.add(linea);
            }
        }
        return lineas;
    }
}
