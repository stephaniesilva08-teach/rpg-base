package com.rpg.core;

import java.util.NoSuchElementException;
import java.util.OptionalInt;
import java.util.Scanner;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Entrada por teclado con validacion y reintento.
 *
 * <p>Un hilo daemon propio ("lector-teclado") es el unico que llama a {@code scanner.nextLine()} y deja cada linea
 * en una cola. El hilo principal espera en la cola, y el temporizador de turno tambien puede dejar ahi un aviso de
 * "tiempo agotado". Asi el turno se pierde en el momento exacto en que se acaba el tiempo, sin esperar a que el
 * jugador pulse Enter (un {@code nextLine()} bloqueado no se puede cancelar desde otro hilo).</p>
 */
public class Consola {

    private enum Tipo { LINEA, TIEMPO_AGOTADO, FIN }

    private record Mensaje(Tipo tipo, String texto) {
    }

    private static final int LARGO_MAXIMO_NOMBRE = 15;
    private static final String PATRON_NOMBRE = "[\\p{L}0-9 ]+";

    private final Scanner scanner;
    private final BlockingQueue<Mensaje> cola = new LinkedBlockingQueue<>();
    private Thread lector;

    public Consola(Scanner scanner) {
        this.scanner = scanner;
    }

    public String leerLinea(String mensaje) {
        System.out.print(mensaje);
        while (true) {
            Mensaje recibido = siguiente();
            if (recibido.tipo() == Tipo.LINEA) {
                return recibido.texto().trim();
            }
            // Un aviso de tiempo agotado que llega fuera de un turno ya no sirve: se ignora.
        }
    }

    public int leerEntero(String mensaje, int minimo, int maximo) {
        while (true) {
            OptionalInt valor = interpretar(leerLinea(mensaje), minimo, maximo);
            if (valor.isPresent()) {
                return valor.getAsInt();
            }
        }
    }

    /**
     * Como {@link #leerEntero}, pero si el temporizador llama a {@link #notificarTiempoAgotado()} antes de que haya
     * una respuesta valida, deja de esperar.
     * @return el numero elegido, o vacio si se agoto el tiempo
     */
    public OptionalInt leerEnteroConTiempo(String mensaje, int minimo, int maximo) {
        while (true) {
            System.out.print(mensaje);
            Mensaje recibido = siguiente();
            if (recibido.tipo() == Tipo.TIEMPO_AGOTADO) {
                return OptionalInt.empty();
            }
            OptionalInt valor = interpretar(recibido.texto().trim(), minimo, maximo);
            if (valor.isPresent()) {
                return valor;
            }
        }
    }

    /** Lo llama el hilo del temporizador (TurnoCallback): despierta al hilo que espera la respuesta. */
    public void notificarTiempoAgotado() {
        cola.offer(new Mensaje(Tipo.TIEMPO_AGOTADO, null));
    }

    /** Si el aviso llego justo cuando el jugador respondia a tiempo, se descarta para que no afecte al turno siguiente. */
    public void descartarAvisoTiempo() {
        cola.removeIf(mensaje -> mensaje.tipo() == Tipo.TIEMPO_AGOTADO);
    }

    public String leerNombre(String mensaje) {
        while (true) {
            String nombre = leerLinea(mensaje);
            if (nombre.isEmpty()) {
                System.out.println("El nombre no puede estar vacio.");
            } else if (nombre.length() > LARGO_MAXIMO_NOMBRE) {
                System.out.println("Maximo " + LARGO_MAXIMO_NOMBRE + " caracteres.");
            } else if (!nombre.matches(PATRON_NOMBRE)) {
                System.out.println("Usa solo letras, numeros y espacios.");
            } else {
                return nombre;
            }
        }
    }

    private OptionalInt interpretar(String texto, int minimo, int maximo) {
        if (texto.isEmpty()) {
            System.out.println("No escribiste nada.");
            return OptionalInt.empty();
        }
        try {
            int valor = Integer.parseInt(texto);
            if (valor >= minimo && valor <= maximo) {
                return OptionalInt.of(valor);
            }
            System.out.printf("Numero fuera de rango (%d a %d).%n", minimo, maximo);
        } catch (NumberFormatException e) {
            System.out.println("\"" + texto + "\" no es un numero.");
        }
        return OptionalInt.empty();
    }

    /** Espera el siguiente mensaje. Mantiene el comportamiento de Scanner: sin mas entrada, NoSuchElementException. */
    private Mensaje siguiente() {
        iniciarLector();
        try {
            Mensaje recibido = cola.take();
            if (recibido.tipo() == Tipo.FIN) {
                cola.offer(recibido); // cualquier lectura posterior tambien debe ver el final
                throw new NoSuchElementException("No hay mas lineas en la entrada");
            }
            return recibido;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new NoSuchElementException("Lectura interrumpida");
        }
    }

    private synchronized void iniciarLector() {
        if (lector != null) {
            return;
        }
        lector = new Thread(this::leerTeclado, "lector-teclado");
        lector.setDaemon(true); // si el juego termina, este hilo bloqueado en el teclado no debe impedir cerrar la JVM
        lector.start();
    }

    private void leerTeclado() {
        try {
            while (true) {
                cola.offer(new Mensaje(Tipo.LINEA, scanner.nextLine()));
            }
        } catch (NoSuchElementException | IllegalStateException e) {
            // Fin de la entrada, o el Scanner se cerro al salir del juego: se avisa a quien este esperando.
            cola.offer(new Mensaje(Tipo.FIN, null));
        }
    }
}
