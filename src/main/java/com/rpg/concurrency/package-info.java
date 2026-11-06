/**
 * Taller de concurrencia: todo lo que corre en hilos aparte.
 * <ul>
 *   <li>{@link com.rpg.concurrency.TemporizadorTurnoTask} y {@link com.rpg.concurrency.GestorTurno}: limite de tiempo por turno.</li>
 *   <li>{@link com.rpg.concurrency.EfectoContinuoTask}: efectos DoT / HoT, un hilo por efecto, cancelables con interrupt().</li>
 *   <li>{@link com.rpg.concurrency.AutoGuardadoDaemon}: guardado periodico en un hilo daemon.</li>
 * </ul>
 */
package com.rpg.concurrency;
