package com.rpg;

import com.rpg.core.Consola;
import com.rpg.core.MotorJuego;
import com.rpg.view.HeroeUI;

import java.util.NoSuchElementException;
import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        try {
            Consola consola = new Consola(scanner);
            MotorJuego motor = new MotorJuego(consola);

            motor.iniciarAutoGuardado();
            motor.avisarPartidaExistente();
            try {
                ejecutarMenu(consola, motor);
            } finally {
                motor.cerrar();
            }
        } catch (NoSuchElementException e) {
            System.out.println("\nEntrada finalizada. Hasta luego.");
        }
    }

    private static void ejecutarMenu(Consola consola, MotorJuego motor) {
        int opcion;
        do {
            mostrarMenu();
            opcion = consola.leerEntero("Opcion: ", 0, 8);
            switch (opcion) {
                case 1 -> motor.combateRapido();
                case 2 -> motor.jugarTorneo();
                case 3 -> motor.mostrarBestiario();
                case 4 -> motor.mostrarHistorial();
                case 5 -> motor.cargarPartida();
                case 6 -> motor.mostrarArchivos();
                case 7 -> motor.borrarArchivos();
                case 8 -> iniciarMenuHeroes(consola);
                default -> System.out.println("Hasta luego.");
            }
        } while (opcion != 0);
    }

    private static void mostrarMenu() {
        System.out.println("\n=== SIMULADOR DE BATALLA RPG ===");
        System.out.println("1. Combate rapido");
        System.out.println("2. Torneo");
        System.out.println("3. Ver bestiario");
        System.out.println("4. Ver historial de combates");
        System.out.println("5. Cargar partida");
        System.out.println("6. Ver archivos del juego");
        System.out.println("7. Borrar archivos guardados");
        System.out.println("8. Gestion de heroes (JDBC + MVC)");
        System.out.println("0. Salir");
    }

    /**
     * Inicia el menu de gestion de heroes (modulo JDBC + MVC).
     * Coordina con el Controlador y laVista, sin tocar la base de datos directamente.
     */
    private static void iniciarMenuHeroes(Consola consola) {
        // Crear un Scanner independiente para la vista de heroes
        // (El HeroeUI necesita un Scanner, no un Consola)
        Scanner scanner = new Scanner(System.in);
        HeroeUI heroeUI = new HeroeUI(scanner);
        heroeUI.iniciar();
    }
}
