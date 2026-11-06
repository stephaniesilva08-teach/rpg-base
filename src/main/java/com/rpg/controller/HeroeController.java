package com.rpg.controller;

import com.rpg.dao.HeroeDAO;
import com.rpg.model.Heroe;

import java.util.List;

/**
 * Controlador para la gestion de heroes.
 * <p>
 * Se encarga de:
 * <ul>
 *   <li>Validar los datos del usuario (nombre, tipo, nivel)</li>
 *   <li>Llamar al DAO para realizar las operaciones en la base de datos</li>
 *   <li>Decidir el mensaje a mostrar en funcion del resultado</li>
 * </ul>
 * </p>
 * <p>
 * No accede directamente a la base de datos. Solo coordina entre la vista y el DAO.
 * </p>
 */
public class HeroeController {

    /**
     * Constructor privado. Solo metodos estaticos.
     */
    private HeroeController() {
    }

    /**
     * Crea un nuevo heroe en la base de datos.
     *
     * @param nombre el nombre del heroe
     * @param tipo   el tipo de heroe (guerrero o mago)
     * @param vidaMaxima la vida maxima del heroe
     * @param nivel el nivel del heroe
     * @return true si se inserto correctamente, false en caso contrario
     */
    public static boolean crearHeroe(String nombre, String tipo, int vidaMaxima, int nivel) {
        try {
            // Crear el objeto modelo
            Heroe heroe = new Heroe(nombre, tipo, vidaMaxima, nivel);

            // Insertar en la base de datos
            int filasAfectadas = HeroeDAO.insertar(heroe);

            // Si se inserto correctamente, devolver true
            return filasAfectadas > 0;

        } catch (IllegalArgumentException e) {
            // Error de validacion del modelo
            System.out.println("Error de validacion: " + e.getMessage());
            return false;

        } catch (Exception e) {
            // Error de base de datos
            System.out.println("Error al crear el heroe: " + e.getMessage());
            return false;
        }
    }

    /**
     * Lista todos los heroes de la base de datos.
     *
     * @return lista de heroes
     */
    public static List<Heroe> listarHeroes() {
        try {
            return HeroeDAO.listar();
        } catch (Exception e) {
            System.out.println("Error al listar heroes: " + e.getMessage());
            return List.of();
        }
    }

    /**
     * Busca heroes por tipo (guerrero o mago).
     *
     * @param tipo el tipo de heroe a buscar
     * @return lista de heroes que coinciden con el tipo
     */
    public static List<Heroe> buscarHeroesPorTipo(String tipo) {
        try {
            return HeroeDAO.buscarPorTipo(tipo);
        } catch (Exception e) {
            System.out.println("Error al buscar heroes: " + e.getMessage());
            return List.of();
        }
    }

    /**
     * Actualiza el nivel de un heroe por su ID.
     *
     * @param id     el ID del heroe
     * @param nivel  el nuevo nivel
     * @return true si se actualizo correctamente, false si no se encontro el heroe
     */
    public static boolean actualizarNivel(int id, int nivel) {
        try {
            int filasAfectadas = HeroeDAO.actualizarNivel(id, nivel);

            // Si se actualizo correctamente, devolver true
            return filasAfectadas > 0;

        } catch (IllegalArgumentException e) {
            System.out.println("Error de validacion: " + e.getMessage());
            return false;

        } catch (Exception e) {
            System.out.println("Error al actualizar el nivel: " + e.getMessage());
            return false;
        }
    }

    /**
     * Elimina un heroe por su ID.
     *
     * @param id el ID del heroe a eliminar
     * @return true si se elimino correctamente, false si no se encontro
     */
    public static boolean eliminarHeroe(int id) {
        try {
            int filasAfectadas = HeroeDAO.eliminar(id);

            // Si se elimino correctamente, devolver true
            return filasAfectadas > 0;

        } catch (Exception e) {
            System.out.println("Error al eliminar el heroe: " + e.getMessage());
            return false;
        }
    }
}
