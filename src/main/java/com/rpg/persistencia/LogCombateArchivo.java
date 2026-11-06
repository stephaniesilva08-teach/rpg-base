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

/**
 * Log de combate en un archivo de texto.
 *
 * <p>FileWriter vs Files.writeString: con {@code BufferedWriter + FileWriter} tu abres el flujo, escribes linea a
 * linea y lo cierras (try-with-resources); sirve para ir agregando texto poco a poco o para archivos grandes.
 * {@code Files.writeString(path, texto)} (NIO) abre, escribe todo el texto de una vez y cierra en una sola
 * llamada; es mas corto y sirve cuando ya tienes el texto completo y es de tamano razonable.</p>
 */
public final class LogCombateArchivo {

    private LogCombateArchivo() {
    }

    /**
     * Escribe el texto en el archivo.
     * @param agregar true = agrega al final (conserva lo anterior); false = sobrescribe el archivo entero
     */
    public static void guardar(File archivo, String texto, boolean agregar) throws IOException {
        ArchivosPartida.asegurarCarpetaPadre(archivo);
        try (BufferedWriter escritor = new BufferedWriter(new FileWriter(archivo, StandardCharsets.UTF_8, agregar))) {
            for (String linea : (Iterable<String>) texto.lines()::iterator) {
                escritor.write(linea);
                escritor.newLine(); // salto de linea propio del sistema, sin concatenarlo a mano
            }
            escritor.flush(); // el cierre del try-with-resources tambien lo haria; aqui se deja explicito
        }
    }

    /** Lee todas las lineas del archivo. Si no existe lanza FileNotFoundException. */
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
