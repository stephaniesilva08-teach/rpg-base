package com.rpg.core;

import com.rpg.persistencia.LogCombateArchivo;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;


public class RegistroCombate {

    private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final StringBuilder texto = new StringBuilder();
    
    private final ThreadLocal<String> etiqueta = new ThreadLocal<>();

    public synchronized void agregar(String linea) {
        String etiquetada = etiqueta.get() == null ? linea : "[" + etiqueta.get() + "] " + linea;
        System.out.println(etiquetada);
        texto.append(etiquetada).append(System.lineSeparator());
    }

    public void agregar(String formato, Object... argumentos) {
        agregar(String.format(formato, argumentos));
    }

    public synchronized void limpiar() {
        texto.setLength(0);
    }

    
    public void usarEtiqueta(String nombre) {
        etiqueta.set(nombre);
    }

    public void quitarEtiqueta() {
        etiqueta.remove();
    }

    public synchronized String getTexto() {
        return texto.toString();
    }

    
    
    public void guardarEnArchivo(File archivo, boolean agregar) throws IOException {
        String contenido = "===== Registro guardado el " + LocalDateTime.now().format(FORMATO_FECHA) + " ====="
                + System.lineSeparator() + getTexto();
// TALLER ARCHIVOS
        LogCombateArchivo.guardar(archivo, contenido, agregar);
    }

    
    
    public static List<String> leerDeArchivo(File archivo) throws IOException {
        return LogCombateArchivo.leer(archivo);
    }
}
