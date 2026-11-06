package com.rpg;

import com.rpg.view.HeroeUI;

import java.util.Scanner;

/**
 * Punto de entrada del módulo JDBC + MVC (Registro de Heroes persistente).
 *
 * <p>Según el taller2.txt, el Main debe ser mínimo: únicamente arranca la aplicación
 * llamando a la vista (View). No contiene menú de juego, ni lógica de negocio.</p>
 *
 * <p>Este Main inicia la aplicación de gestión de heroes. El módulo del juego RPG
 * (simulador de batalla) mantiene su propio punto de entrada en MainJuego.</p>
 */
public class Main {
    /**
     * Punto de entrada de la aplicación.
     * Arranca la aplicación únicamente creando la Vista (HeroeUI) y llamando a iniciar().
     *
     * @param args argumentos de línea de comandos (no utilizados)
     */
    public static void main(String[] args) {
        new HeroeUI(new Scanner(System.in)).iniciar();
    }
}
