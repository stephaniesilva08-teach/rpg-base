package com.rpg.persistencia;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;


public final class ArchivosPartida {

    public static final String CARPETA = "partidas";
    public static final String NOMBRE_PARTIDA = "partida.dat";
    public static final String NOMBRE_HISTORIAL = "historial-combates.txt";

    private ArchivosPartida() {
    }

    
    public static File archivoPartida() {
        return new File(CARPETA, NOMBRE_PARTIDA);
    }

    public static File archivoHistorial() {
        return new File(CARPETA, NOMBRE_HISTORIAL);
    }

    
    public static File archivoPartidaAbsoluto() {
        return archivoPartida().getAbsoluteFile();
    }

    
    public static void asegurarCarpetaPadre(File archivo) throws IOException {
        File carpeta = archivo.getAbsoluteFile().getParentFile();
        
        if (carpeta != null && !carpeta.isDirectory() && !carpeta.mkdirs() && !carpeta.isDirectory()) {
            throw new IOException("No se pudo crear la carpeta " + carpeta.getAbsolutePath());
        }
    }

    
    public static boolean crearSiNoExiste(File archivo) {
        try {
            asegurarCarpetaPadre(archivo);
            return archivo.createNewFile();
        } catch (IOException e) {
            System.out.println("No se pudo crear " + archivo.getName() + ": " + e.getMessage());
            return false;
        }
    }

    
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

    
    public static boolean borrarConFile(File archivo) {
        return archivo.delete();
    }

    
    public static boolean borrarConNio(File archivo) throws IOException {
        return Files.deleteIfExists(archivo.toPath());
    }
}
