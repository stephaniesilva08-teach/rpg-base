package com.rpg.core;

import com.rpg.entities.Escuadron;
import com.rpg.entities.Guerrero;
import com.rpg.entities.Mago;
import com.rpg.entities.Personaje;

import java.util.List;
import java.util.Optional;

/** Torneo de 4 gladiadores: dos semifinales (en paralelo) y una final. */
public class TorneoManager {

    private static final long MUESTRA_DURANTE_MS = 300L;

    private final Arena arena;
    private final RegistroCombate registro;

    public TorneoManager(Arena arena, RegistroCombate registro) {
        this.arena = arena;
        this.registro = registro;
    }

    /** @return el campeon del torneo, o vacio si el torneo se interrumpio antes de terminar */
    public Optional<Personaje> jugarTorneo() {
        List<Personaje> gladiadores = crearGladiadores().getMiembros();

        // TALLER CONCURRENCIA (resuelto): cada semifinal corre en su propio Thread y escribe su ganador en esta
        // memoria compartida. El hilo principal solo la lee DESPUES de los join(): join() garantiza que el otro
        // hilo ya termino y que sus escrituras son visibles.
        Personaje[] finalistas = new Personaje[2];
        Thread semifinalA = new Thread(tarea("Semifinal A",
                () -> finalistas[0] = simularSemifinal(gladiadores.get(0), gladiadores.get(1))), "semifinal-A");
        Thread semifinalB = new Thread(tarea("Semifinal B",
                () -> finalistas[1] = simularSemifinal(gladiadores.get(2), gladiadores.get(3))), "semifinal-B");

        mostrarEstados("antes de start()", semifinalA, semifinalB);
        semifinalA.start();
        semifinalB.start();
        mostrarEstados("justo despues de start()", semifinalA, semifinalB);

        try {
            Thread.sleep(MUESTRA_DURANTE_MS);
            mostrarEstados("durante la ejecucion", semifinalA, semifinalB);
            semifinalA.join();
            semifinalB.join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            semifinalA.interrupt();
            semifinalB.interrupt();
            registro.agregar("Torneo interrumpido antes de terminar las semifinales");
            return Optional.empty();
        }
        mostrarEstados("despues de join()", semifinalA, semifinalB);

        if (finalistas[0] == null || finalistas[1] == null) {
            registro.agregar("Una semifinal fallo y no hay finalista: no se puede jugar la final");
            return Optional.empty();
        }
        return Optional.of(simularFinal(finalistas[0], finalistas[1]));
    }

    public Personaje simularSemifinal(Personaje a, Personaje b) {
        registro.agregar("=== SEMIFINAL: %s vs %s ===", a.getNombre(), b.getNombre());
        Personaje ganador = arena.duelo(a, b);
        registro.agregar("Clasifica a la final: %s", ganador.getNombre());
        return ganador;
    }

    private Personaje simularFinal(Personaje a, Personaje b) {
        a.restaurarVida();
        b.restaurarVida();
        registro.agregar("=== FINAL: %s vs %s ===", a.getNombre(), b.getNombre());
        return arena.duelo(a, b);
    }

    /** Envuelve el cuerpo de una semifinal: etiqueta sus lineas del registro y no deja morir al hilo en silencio. */
    private Runnable tarea(String etiqueta, Runnable cuerpo) {
        return () -> {
            registro.usarEtiqueta(etiqueta);
            try {
                cuerpo.run();
            } catch (RuntimeException e) {
                registro.agregar("%s fallo: %s", etiqueta, e);
            } finally {
                registro.quitarEtiqueta();
            }
        };
    }

    /** Muestra Thread.getState() de cada semifinal para ver el ciclo de vida del hilo. */
    private void mostrarEstados(String momento, Thread semifinalA, Thread semifinalB) {
        registro.agregar("[Hilos %s] %s: %s | %s: %s", momento,
                semifinalA.getName(), semifinalA.getState(), semifinalB.getName(), semifinalB.getState());
        registro.agregar("   (%s)", describirEstado(semifinalA.getState()));
    }

    private static String describirEstado(Thread.State estado) {
        return switch (estado) {
            case NEW -> "NEW: creado pero todavia no arranco";
            case RUNNABLE -> "RUNNABLE: ejecutandose o listo para ejecutarse";
            case TIMED_WAITING -> "TIMED_WAITING: dormido con sleep() esperando que pase el tiempo";
            case TERMINATED -> "TERMINATED: su run() ya termino";
            default -> estado + ": esperando un candado o a otro hilo";
        };
    }

    private Escuadron crearGladiadores() {
        Escuadron escuadron = new Escuadron("Gladiadores");
        escuadron.agregar(new Guerrero("Maximo"));
        escuadron.agregar(new Mago("Circe"));
        escuadron.agregar(new Guerrero("Aquiles"));
        escuadron.agregar(new Mago("Merlina"));
        return escuadron;
    }
}
