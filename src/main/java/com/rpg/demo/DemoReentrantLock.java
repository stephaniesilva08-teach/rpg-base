package com.rpg.demo;

import java.util.concurrent.locks.ReentrantLock;

/**
 * Prueba de la seccion "Investigacion obligatoria" de la guia (taller de concurrencia).
 *
 * Punto: ReentrantLock.
 *
 * RecibeDanio reescrita con lock()/unlock() en try-finally. Si el unlock() no estuviera en el
 * finally, y ocurriese una excepcion durante el bloque, el lock se quedaria tomado para siempre
 * (deadlock permanente).
 *
 * Cuando prefiero ReentrantLock a synchronized:
 * - tryLock() con tiempo de espera (no bloqueo indefinido).
 * - lockInterruptibly() para interrumpir la espera.
 * - fairness (preferencia por el hilo que mas tiempo espera).
 * - Varias condiciones (Condition) sobre el mismo cerrojo.
 */
public class DemoReentrantLock {
    private final ReentrantLock lock = new ReentrantLock();
    private int vida = 100;

    /**
     * RecibeDanio reescrita con ReentrantLock: lock() al inicio, unlock() obligatoriamente en finally.
     */
    public void recibirDanio(int cantidad) {
        if (cantidad < 0) {
            throw new IllegalArgumentException("El danio no puede ser negativo");
        }
        lock.lock();
        try {
            vida = Math.max(0, vida - cantidad);
            System.out.println("[ReentrantLock] vida = " + vida);
        } finally {
            lock.unlock();   // siempre en finally
        }
    }

    public static void main(String[] args) throws Exception {
        DemoReentrantLock d = new DemoReentrantLock();
        d.recibirDanio(30);
        d.recibirDanio(50);

        System.out.println("[DemoReentrantLock] Explicacion:");
        System.out.println("  Prefiero ReentrantLock a synchronized cuando necesito:");
        System.out.println("  - tryLock() con tiempo de espera (no bloqueo indefinido).");
        System.out.println("  - lockInterruptibly() para interrumpir la espera.");
        System.out.println("  - fairness (preferencia por el hilo que mas tiempo espera).");
        System.out.println("  - varias condiciones (Condition) sobre el mismo cerrojo.");
    }
}
