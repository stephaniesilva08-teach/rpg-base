package com.rpg.core;

import java.util.NoSuchElementException;
import java.util.OptionalInt;
import java.util.Scanner;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;


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

    
    public void notificarTiempoAgotado() {
        cola.offer(new Mensaje(Tipo.TIEMPO_AGOTADO, null));
    }

    
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

    
    private Mensaje siguiente() {
        iniciarLector();
        try {
            Mensaje recibido = cola.take();
            if (recibido.tipo() == Tipo.FIN) {
                cola.offer(recibido); 
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
        lector.setDaemon(true); 
        lector.start();
    }

    private void leerTeclado() {
        try {
            while (true) {
                cola.offer(new Mensaje(Tipo.LINEA, scanner.nextLine()));
            }
        } catch (NoSuchElementException | IllegalStateException e) {
            
            cola.offer(new Mensaje(Tipo.FIN, null));
        }
    }
}
