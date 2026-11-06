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


public class GestorPartida {

    
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
                ArchivosPartida.borrarConFile(temporal); 
            }
        }
    }

    
    public synchronized PartidaGuardada cargar() throws IOException, ClassNotFoundException {
        try (ObjectInputStream entrada = new ObjectInputStream(
                new BufferedInputStream(new FileInputStream(archivo)))) {
            entrada.setObjectInputFilter(FILTRO); 
            Object leido = entrada.readObject();
            if (leido instanceof PartidaGuardada partida) {
                return partida;
            }
            throw new InvalidObjectException("El archivo no contiene una partida de este juego");
        }
    }

    
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
