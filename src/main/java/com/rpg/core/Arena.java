package com.rpg.core;

import com.rpg.entities.Personaje;
import com.rpg.exceptions.SinManaException;
import com.rpg.exceptions.VidaInsuficienteException;

import java.util.Random;

/** Reglas de un combate: ataques, habilidades y duelos automaticos. Los efectos DoT/HoT corren en sus propios hilos. */
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

    /** Prepara a los combatientes: los mensajes de sus efectos continuos iran al registro de combate. */
    public void iniciarCombate(Personaje... combatientes) {
        for (Personaje combatiente : combatientes) {
            combatiente.setEscuchaEfectos(registro::agregar);
        }
    }

    /** Fin del combate: interrumpe los hilos de efectos que sigan corriendo y espera a que terminen limpios. */
    public void terminarCombate(Personaje... combatientes) {
        for (Personaje combatiente : combatientes) {
            combatiente.cancelarEfectos();
        }
    }

    /** Comprueba si puede actuar este turno. @return true si el personaje puede actuar */
    public boolean iniciarTurno(Personaje personaje) {
        personaje.alIniciarTurno();
        // TALLER CONCURRENCIA (resuelto): aqui se procesaban los efectos turno a turno. Ahora cada efecto tiene su
        // hilo (EfectoContinuoTask) y aplica sus ticks por su cuenta; aqui solo se mira el resultado.
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

    /** @return true si el especial se ejecuto; false si fallo por falta de mana o de vida */
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

    /** Turno completo controlado por el programa (enemigos y gladiadores). */
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

    /**
     * Pelea hasta que uno cae (o se acaban las rondas). Entre ronda y ronda hay una pausa para que los efectos
     * continuos alcancen a aplicarse y se pueda seguir el duelo. @return el ganador
     */
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

    /** @return false si interrumpieron al hilo durante la pausa (el duelo se detiene) */
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
