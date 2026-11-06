package com.rpg.persistencia;

import com.rpg.entities.Gremio;
import com.rpg.entities.PerfilJugador;

import java.io.Serial;
import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Foto del estado de la partida: lo que se escribe en el .dat con ObjectOutputStream.
 *
 * <p>Por que no se debe deserializar (readObject) datos que no son de confianza: al leer, Java reconstruye objetos
 * y ejecuta codigo de las clases que el archivo diga, y un archivo manipulado puede encadenar clases para ejecutar
 * codigo o consumir memoria y CPU hasta tumbar el programa. Por eso {@link GestorPartida} solo acepta clases del juego
 * y de java.lang / java.util (ObjectInputFilter). Alternativas mas seguras: JSON, o un formato de texto propio, que
 * solo leen datos y nunca instancian clases arbitrarias.</p>
 */
public class PartidaGuardada implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private final PerfilJugador perfil;
    private final Gremio gremio;
    private final LinkedHashMap<String, Integer> derrotados;
    private final String resumen;
    private final long guardadaEn;

    public PartidaGuardada(PerfilJugador perfil, Gremio gremio, Map<String, Integer> derrotados, String resumen) {
        this.perfil = perfil;
        this.gremio = gremio;
        this.derrotados = new LinkedHashMap<>(derrotados);
        this.resumen = resumen;
        this.guardadaEn = System.currentTimeMillis();
    }

    public PerfilJugador getPerfil() {
        return perfil;
    }

    public Gremio getGremio() {
        return gremio;
    }

    public Map<String, Integer> getDerrotados() {
        return derrotados;
    }

    public String getResumen() {
        return resumen;
    }

    public long getGuardadaEn() {
        return guardadaEn;
    }
}
