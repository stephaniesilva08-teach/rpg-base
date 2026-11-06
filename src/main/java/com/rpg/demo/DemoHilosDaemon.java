package com.rpg.demo;

/**
 * Prueba de la seccion "Investigacion obligatoria" de la guia (taller de concurrencia).
 *
 * Punto: Hilos daemon.
 *
 * Demuestra dos cosas:
 * 1. La JVM cierra aunque el hilo daemon siga vivo (no espera a los hilos daemon).
 * 2. setDaemon(true) despues de start() lanza IllegalThreadStateException.
 *
 * Diferencia entre hilo de usuario y hilo daemon:
 * - Hilo de usuario: la JVM espera a que acabe antes de devolver el control y cerrarse.
 * - Hilo daemon: la JVM puede cerrarse en cualquier momento; no impide el cierre final.
 */
public class DemoHilosDaemon {
    public static void main(String[] args) throws Exception {
        // ---- Demo 1: JVM cierra aunque el hilo daemon siga vivo ----
        System.out.println("[Investigacion] Demo 1: JVM cierra aunque el hilo daemon siga vivo.");
        System.out.println("  Creando un hilo DAEMON que se mantiene vivo (ciclo infinito)...");
        Thread daemon = new Thread(() -> {
            int i = 0;
            while (true) {
                System.out.println("  [daemon] vivo (" + i + ")");
                i++;
                try { Thread.sleep(400); } catch (InterruptedException e) { break; }
            }
        }, "daemon-vivo");
        daemon.setDaemon(true);   // marcamos como daemon ANTES de start()
        daemon.start();

        Thread.sleep(1000);
        System.out.println("  [main] hilo principal termina. La JVM se cerrara solo.");
        Thread.sleep(1000);

        // ---- Demo 2: setDaemon(true) despues de start() lanza IllegalThreadStateException ----
        System.out.println("[Investigacion] Demo 2: setDaemon(true) despues de start() lanza IllegalThreadStateException.");
        System.out.println("  Creando otro hilo y llamando setDaemon(true) DESPUES de start().");
        Thread otro = new Thread(() -> System.out.println("  [otro] corriendo"));
        otro.start();
        try {
            otro.setDaemon(true);
            System.out.println("  [main] ERROR: no lanzo excepcion");
        } catch (IllegalThreadStateException e) {
            System.out.println("  [main] OK: IllegalThreadStateException -> " + e);
        }
    }
}
