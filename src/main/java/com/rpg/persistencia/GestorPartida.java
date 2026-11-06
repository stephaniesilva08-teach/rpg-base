package com.rpg.persistencia;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InvalidObjectException;
import java.io.ObjectInputFilter;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

/**
 * Guarda, carga y borra la partida (.dat) con serializacion.
 *
 * <p>Todos los metodos son {@code synchronized} sobre este objeto: el auto-guardado (hilo daemon) y el hilo
 * principal comparten UNA instancia, asi nunca escriben y leen el mismo archivo a la vez.</p>
 *
 * <p>La escritura es a prueba de cortes: primero se escribe un archivo temporal y solo cuando esta completo se
 * renombra sobre el definitivo. Si el juego se cierra en mitad del guardado, partida.dat sigue siendo la copia
 * anterior entera, nunca un archivo a medio escribir.</p>
 */
public class GestorPartida {

    /** Solo se reconstruyen clases del juego y de java.lang / java.util; todo lo demas se rechaza. */
    private static final ObjectInputFilter FILTRO = ObjectInputFilter.Config.createFilter(
            "maxdepth=100;maxrefs=100000;com.rpg.**;java.lang.*;java.util.*;!*");

    private final File archivo;

    public GestorPartida(File archivo) {
        this.archivo = archivo;
    }

    public File getArchivo() {
        return archivo;
    }

    public synchronized boolean existe() {
        return archivo.exists();
    }

    /** @throws java.io.NotSerializableException si un objeto anidado no es Serializable */
    public synchronized void guardar(PartidaGuardada partida) throws IOException {
        ArchivosPartida.asegurarCarpetaPadre(archivo);
        File temporal = new File(archivo.getAbsoluteFile().getParentFile(), archivo.getName() + ".tmp");
        boolean terminado = false;
        try {
            try (ObjectOutputStream salida = new ObjectOutputStream(
                    new BufferedOutputStream(new FileOutputStream(temporal)))) {
                salida.writeObject(partida);
            }
            moverSobre(temporal, archivo);
            terminado = true;
        } finally {
            if (!terminado) {
                ArchivosPartida.borrarConFile(temporal); // no dejar un temporal roto
            }
        }
    }

    /**
     * @throws java.io.FileNotFoundException si todavia no hay partida guardada
     * @throws java.io.InvalidClassException si el archivo es de otra version o contiene clases no permitidas
     * @throws ClassNotFoundException si el archivo usa una clase que ya no existe
     */
    public synchronized PartidaGuardada cargar() throws IOException, ClassNotFoundException {
        try (ObjectInputStream entrada = new ObjectInputStream(
                new BufferedInputStream(new FileInputStream(archivo)))) {
            entrada.setObjectInputFilter(FILTRO); // antes del primer readObject
            Object leido = entrada.readObject();
            if (leido instanceof PartidaGuardada partida) {
                return partida;
            }
            throw new InvalidObjectException("El archivo no contiene una partida de este juego");
        }
    }

    /** Borra con Files.deleteIfExists. @return true si habia partida y se borro */
    public synchronized boolean borrar() throws IOException {
        return ArchivosPartida.borrarConNio(archivo);
    }

    private static void moverSobre(File origen, File destino) throws IOException {
        try {
            Files.move(origen.toPath(), destino.toPath(), StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException e) {
            Files.move(origen.toPath(), destino.toPath(), StandardCopyOption.REPLACE_EXISTING);
        }
    }
}
