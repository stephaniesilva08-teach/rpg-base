package com.rpg.demo;

import java.util.concurrent.locks.ReentrantLock;


public class DemoReentrantLock {
    private final ReentrantLock lock = new ReentrantLock();
    private int vida = 100;

    
    public void recibirDanio(int cantidad) {
        if (cantidad < 0) {
            throw new IllegalArgumentException("El danio no puede ser negativo");
        }
        lock.lock();
        try {
            vida = Math.max(0, vida - cantidad);
            System.out.println("[ReentrantLock] vida = " + vida);
        } finally {
            lock.unlock();   
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
