package com.rpg.persistencia;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

/** Rutas de los archivos del juego y operaciones basicas con {@link File}. */
public final class ArchivosPartida {

    public static final String CARPETA = "partidas";
    public static final String NOMBRE_PARTIDA = "partida.dat";
    public static final String NOMBRE_HISTORIAL = "historial-combates.txt";

    private ArchivosPartida() {
    }

    /** 
     * Ruta RELATIVA: se interpreta desde la carpeta donde se ejecuta {@code java} (el directorio de trabajo), asi que
     * "partidas/partida.dat" apunta a un sitio distinto segun desde donde lances el juego.
     */
    public static File archivoPartida() {
        return new File(CARPETA, NOMBRE_PARTIDA);
    }

    public static File archivoHistorial() {
        return new File(CARPETA, NOMBRE_HISTORIAL);
    }

    /** 
     * Ruta ABSOLUTA: empieza en la raiz del sistema de archivos y apunta siempre al mismo sitio, sin importar
     * desde donde se ejecute el programa.
     */
    public static File archivoPartidaAbsoluto() {
        return archivoPartida().getAbsoluteFile();
    }

    /** Crea la carpeta que contendra el archivo (por ejemplo partidas/). Si ya existe, no falla. */
    public static void asegurarCarpetaPadre(File archivo) throws IOException {
        File carpeta = archivo.getAbsoluteFile().getParentFile();
        // mkdirs() devuelve false si la carpeta ya existia; por eso se vuelve a mirar isDirectory().
        if (carpeta != null && !carpeta.isDirectory() && !carpeta.mkdirs() && !carpeta.isDirectory()) {
            throw new IOException("No se pudo crear la carpeta " + carpeta.getAbsolutePath());
        }
    }

    /** @return true si el archivo se creo ahora; false si ya existia o no se pudo crear. */
    public static boolean crearSiNoExiste(File archivo) {
        try {
            asegurarCarpetaPadre(archivo);
            return archivo.createNewFile();
        } catch (IOException e) {
            System.out.println("No se pudo crear " + archivo.getName() + ": " + e.getMessage());
            return false;
        }
    }

    /** Una linea con nombre, rutas, si existe y cuanto pesa. */
    public static String describir(File archivo) {
        String estado;
        if (!archivo.exists()) {
            estado = "no existe";
        } else if (archivo.isDirectory()) {
            estado = "carpeta";
        } else {
            estado = "existe, " + archivo.length() + " bytes";
        }
        return String.format("%s [%s] ruta relativa: %s | ruta absoluta: %s",
                archivo.getName(), estado, archivo.getPath(), archivo.getAbsolutePath());
    }

    /** 
     * Borra con File.delete(). Solo devuelve un boolean: false puede significar "no existia" o "existe pero no se
     * pudo borrar" (permisos, archivo en uso...), y no hay forma de saber cual de los dos fue.
     */
    public static boolean borrarConFile(File archivo) {
        return archivo.delete();
    }

    /** 
     * Borra con Files.deleteIfExists. Distingue los casos: devuelve false solo si el archivo no existia, y si existe
     * pero no se puede borrar lanza una IOException que explica el motivo.
     */
    public static boolean borrarConNio(File archivo) throws IOException {
        return Files.deleteIfExists(archivo.toPath());
    }
}
