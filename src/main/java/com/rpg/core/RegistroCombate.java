package com.rpg.core;

import com.rpg.persistencia.LogCombateArchivo;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Imprime cada evento del combate y lo guarda para incluirlo en el guardado de la partida.
 * Lo usan varios hilos a la vez (semifinales en paralelo, hilos de efectos), asi que los metodos son synchronized:
 * sin eso dos lineas podrian mezclarse a la mitad o perderse en el StringBuilder.
 */
public class RegistroCombate {

    private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final StringBuilder texto = new StringBuilder();
    // Cada hilo puede marcar sus lineas (por ejemplo "Semifinal A") para distinguirlas cuando corren a la vez.
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

    /** Las lineas que escriba el hilo actual llevaran este prefijo hasta que llame a {@link #quitarEtiqueta()}. */
    public void usarEtiqueta(String nombre) {
        etiqueta.set(nombre);
    }

    public void quitarEtiqueta() {
        etiqueta.remove();
    }

    public synchronized String getTexto() {
        return texto.toString();
    }

    /**
     * Vuelca el registro actual a un archivo de texto.
     * @param agregar true = agrega al final y conserva los combates anteriores; false = sobrescribe el archivo
     */
    // TALLER ARCHIVOS: vuelve a escribir el registro acumulado en un archivo de texto (.txt), con agregar o sobrescrito.
    public void guardarEnArchivo(File archivo, boolean agregar) throws IOException {
        String contenido = "===== Registro guardado el " + LocalDateTime.now().format(FORMATO_FECHA) + " ====="
                + System.lineSeparator() + getTexto();
        LogCombateArchivo.guardar(archivo, contenido, agregar);
    }

    /** Lee de vuelta un registro guardado. Si el archivo no existe lanza FileNotFoundException. */
    // TALLER ARCHIVOS: lee de vuelta un registro guardado en un archivo de texto.
    public static List<String> leerDeArchivo(File archivo) throws IOException {
        return LogCombateArchivo.leer(archivo);
    }
}
