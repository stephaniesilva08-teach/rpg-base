package com.rpg.view;

import com.rpg.controller.HeroeController;
import com.rpg.model.Heroe;

import java.util.List;
import java.util.Scanner;

/**
 * Interfaz de usuario (View) para la gestion de heroes.
 * <p>
 * Esta clase maneja la entrada y salida de datos vía consola.
 * <p>
 * <b>Regla MVC:</b> Esta clase no conoce la base de datos. Solo usa el Controlador
 * para comunicarse con el DAO.
 * </p>
 */
public class HeroeUI {

    private final Scanner scanner;

    /**
     * Constructor.
     *
     * @param scanner el scanner para leer la entrada del usuario
     */
    public HeroeUI(Scanner scanner) {
        this.scanner = scanner;
    }

    /**
     * Muestra el menu principal de la aplicacion de heroes.
     */
    private void mostrarMenu() {
        System.out.println("\n=== REGISTRO DE HEROES (JDBC + MVC) ===");
        System.out.println("1. Crear nuevo heroe");
        System.out.println("2. Listar todos los heroes");
        System.out.println("3. Buscar heroe por tipo");
        System.out.println("4. Actualizar nivel de heroe");
        System.out.println("5. Eliminar heroe");
        System.out.println("6. Volver al menu principal");
        System.out.print("Opcion: ");
    }

    /**
     * Inicia la aplicacion y muestra el menu principal.
     */
    public void iniciar() {
        int opcion;
        do {
            mostrarMenu();
            opcion = leerEntero("Opcion: ", 0, 6);

            switch (opcion) {
                case 1 -> crearNuevoHeroe();
                case 2 -> listarTodosLosHeroes();
                case 3 -> buscarHeroePorTipo();
                case 4 -> actualizarNivel();
                case 5 -> eliminarHeroe();
                case 6 -> System.out.println("Volviendo al menu principal...");
                default -> System.out.println("Hasta luego.");
            }

        } while (opcion != 0);
    }

    /**
     * Crea un nuevo heroe.
     */
    private void crearNuevoHeroe() {
        System.out.println("\n--- CREAR NUEVO HÉRERO ---");

        String nombre = leerTexto("Nombre: ");
        String tipo = leerTexto("Tipo (guerrero/mago): ").toLowerCase();

        // Validar tipo
        if (!tipo.equals("guerrero") && !tipo.equals("mago")) {
            System.out.println("Error: El tipo debe ser 'guerrero' o 'mago'.");
            return;
        }

        int vidaMaxima = leerEntero("Vida maxima: ", 1, 9999);
        int nivel = leerEntero("Nivel (>= 1): ", 1, 9999);

        boolean exito = HeroeController.crearHeroe(nombre, tipo, vidaMaxima, nivel);

        if (exito) {
            System.out.println("Heroe creado correctamente.");
        } else {
            System.out.println("No se pudo crear el heroe.");
        }
    }

    /**
     * Lista todos los heroes.
     */
    private void listarTodosLosHeroes() {
        System.out.println("\n--- LISTA DE HEROES ---");

        List<Heroe> heroes = HeroeController.listarHeroes();

        if (heroes.isEmpty()) {
            System.out.println("No hay heroes registrados.");
            return;
        }

        for (Heroe heroe : heroes) {
            System.out.printf("ID: %d | %s | %s | Vida: %d | Nivel: %d%n",
                    heroe.getId(), heroe.getNombre(), heroe.getTipo(),
                    heroe.getVidaMaxima(), heroe.getNivel());
        }
    }

    /**
     * Busca heroes por tipo.
     */
    private void buscarHeroePorTipo() {
        System.out.println("\n--- BUSCAR POR TIPO ---");
        String tipo = leerTexto("Tipo (guerrero/mago): ").toLowerCase();

        if (!tipo.equals("guerrero") && !tipo.equals("mago")) {
            System.out.println("Error: El tipo debe ser 'guerrero' o 'mago'.");
            return;
        }

        List<Heroe> heroes = HeroeController.buscarHeroesPorTipo(tipo);

        if (heroes.isEmpty()) {
            System.out.println("No se encontraron heroes de tipo '" + tipo + "'.");
            return;
        }

        System.out.printf("Se encontraron %d heroe(s) de tipo '%s':%n", heroes.size(), tipo);
        for (Heroe heroe : heroes) {
            System.out.printf("ID: %d | %s | Vida: %d | Nivel: %d%n",
                    heroe.getId(), heroe.getNombre(), heroe.getVidaMaxima(), heroe.getNivel());
        }
    }

    /**
     * Actualiza el nivel de un heroe.
     */
    private void actualizarNivel() {
        System.out.println("\n--- ACTUALIZAR NIVEL ---");

        try {
            int id = leerEntero("ID del heroe: ", 1, 999999);
            int nuevoNivel = leerEntero("Nuevo nivel (>= 1): ", 1, 999999);

            boolean exito = HeroeController.actualizarNivel(id, nuevoNivel);

            if (exito) {
                System.out.println("Nivel actualizado correctamente.");
            } else {
                System.out.println("No se pudo actualizar. El heroe con ese ID no existe.");
            }
        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    /**
     * Elimina un heroe.
     */
    private void eliminarHeroe() {
        System.out.println("\n--- ELIMINAR HÉRERO ---");

        try {
            int id = leerEntero("ID del heroe: ", 1, 999999);

            boolean exito = HeroeController.eliminarHeroe(id);

            if (exito) {
                System.out.println("Heroe eliminado correctamente.");
            } else {
                System.out.println("No se pudo eliminar. El heroe con ese ID no existe.");
            }
        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    // ============ METODOS AUXILIARES ============

    /**
     * Lee un entero del usuario con validacion.
     *
     * @param mensaje mensaje a mostrar
     * @param minimo  valor minimo permitido
     * @param maximo  valor maximo permitido
     * @return el entero leido
     */
    private int leerEntero(String mensaje, int minimo, int maximo) {
        while (true) {
            System.out.print(mensaje);
            String entrada = scanner.nextLine().trim();

            try {
                int valor = Integer.parseInt(entrada);
                if (valor >= minimo && valor <= maximo) {
                    return valor;
                } else {
                    System.out.printf("Error: El valor debe estar entre %d y %d.%n", minimo, maximo);
                }
            } catch (NumberFormatException e) {
                System.out.println("Error: Debe ingresar un numero entero.");
            }
        }
    }

    /**
     * Lee un texto del usuario.
     *
     * @param mensaje mensaje a mostrar
     * @return el texto leido
     */
    private String leerTexto(String mensaje) {
        System.out.print(mensaje);
        return scanner.nextLine().trim();
    }
}
