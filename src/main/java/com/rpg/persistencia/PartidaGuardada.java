package com.rpg.persistencia;

import com.rpg.entities.Gremio;
import com.rpg.entities.PerfilJugador;

import java.io.Serial;
import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.Map;


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
