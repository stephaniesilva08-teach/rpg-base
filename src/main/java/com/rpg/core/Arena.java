package com.rpg.core;

import com.rpg.entities.Personaje;
import com.rpg.exceptions.SinManaException;
import com.rpg.exceptions.VidaInsuficienteException;

import java.util.Random;


public class Arena {

    private static final int PROBABILIDAD_CRITICO = 15;
    private static final double FACTOR_CRITICO = 1.5;
    private static final int VARIACION_DANIO = 3;
    private static final int PROBABILIDAD_ESPECIAL_IA = 35;
    private static final int MAXIMO_RONDAS = 40;

    private final RegistroCombate registro;
    private final Random azar;

    public Arena(RegistroCombate registro, Random azar) {
        this.registro = registro;
        this.azar = azar;
    }

    
    public void iniciarCombate(Personaje... combatientes) {
        for (Personaje combatiente : combatientes) {
            combatiente.setEscuchaEfectos(registro::agregar);
        }
    }

    
    public void terminarCombate(Personaje... combatientes) {
        for (Personaje combatiente : combatientes) {
            combatiente.cancelarEfectos();
        }
    }

    
    public boolean iniciarTurno(Personaje personaje) {
        personaje.alIniciarTurno();
        
        
        if (!personaje.estaVivo()) {
            registro.agregar("%s cae derrotado por sus efectos", personaje.getNombre());
            return false;
        }
        if (personaje.consumirAturdimiento()) {
            registro.agregar("%s esta aturdido y pierde el turno", personaje.getNombre());
            return false;
        }
        return true;
    }

    public void atacar(Personaje atacante, Personaje defensor) {
        boolean critico = azar.nextInt(100) < PROBABILIDAD_CRITICO;
        double factor = critico ? FACTOR_CRITICO : 1.0;
        int danioBruto = (int) Math.round(atacante.calcularDanio() * factor) + azar.nextInt(VARIACION_DANIO + 1);
        int vidaAntes = defensor.getVida();
        defensor.defender(danioBruto);
        registro.agregar("%s ataca a %s%s y causa %d de dano", atacante.getNombre(), defensor.getNombre(),
                critico ? " (CRITICO)" : "", vidaAntes - defensor.getVida());
        registro.agregar("   %s", FormatoCombate.resumen(defensor));
    }

    
    public boolean intentarEspecial(Personaje atacante, Personaje defensor) {
        try {
            registro.agregar(atacante.realizarAtaqueEspecial(defensor));
            registro.agregar("   %s", FormatoCombate.resumen(defensor));
            return true;
        } catch (SinManaException | VidaInsuficienteException e) {
            registro.agregar("%s no puede usar su habilidad: %s", atacante.getNombre(), e.getMessage());
            return false;
        }
    }

    
    public void turnoAutomatico(Personaje atacante, Personaje defensor) {
        if (!atacante.estaVivo() || !defensor.estaVivo() || !iniciarTurno(atacante)) {
            return;
        }
        boolean quiereEspecial = azar.nextInt(100) < PROBABILIDAD_ESPECIAL_IA;
        if (quiereEspecial && intentarEspecial(atacante, defensor)) {
            return;
        }
        atacar(atacante, defensor);
    }

    
    public Personaje duelo(Personaje a, Personaje b) {
        iniciarCombate(a, b);
        try {
            for (int ronda = 1; ronda <= MAXIMO_RONDAS && a.estaVivo() && b.estaVivo(); ronda++) {
                registro.agregar("-- Ronda %d --", ronda);
                turnoAutomatico(a, b);
                turnoAutomatico(b, a);
                if (a.estaVivo() && b.estaVivo() && !pausarRonda()) {
                    break;
                }
            }
        } finally {
            terminarCombate(a, b);
        }
        return a.getVida() >= b.getVida() ? a : b;
    }

    
    private boolean pausarRonda() {
        try {
            Thread.sleep(200);
            return true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            registro.agregar("Duelo interrumpido");
            return false;
        }
    }
}
