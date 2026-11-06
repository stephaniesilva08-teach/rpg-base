package com.rpg.controller;

import com.rpg.entities.Guerrero;
import com.rpg.entities.Mago;
import com.rpg.entities.Personaje;
import com.rpg.entities.enums.TipoPersonaje;
import com.rpg.model.Heroe;
import com.rpg.dao.HeroeDAO;

import java.util.List;

/**
 * Integracion entre el modulo MVC+DAO de heroes y el motor de combate del juego.
 * <p>
 * Esta clase permite cargar un heroe desde la base de datos y usarlo en un combate.
 * Ayuda a mantener el Main minimo y a separar las responsabilidades.
 * </p>
 */
public class HeroeGameIntegration {

    /**
     * Constructor privado.
     */
    private HeroeGameIntegration() {
    }

    /**
     * Carga un heroe de la base de datos por su ID y lo convierte
     * en un objeto del juego (Guerrero o Mago) listo para combate.
     *
     * @param id       el ID del heroe en la base de datos
     * @param combate el motor de combate (MotorJuego) para crear el personaje
     * @return el personaje creado listo para combate
     */
    public static Personaje cargarHeroeParaCombate(int id) {
        // Obtener el heroe de la base de datos por ID
        List<Heroe> heroes = HeroeController.listarHeroes();
        Personaje personaje = null;

        for (Heroe heroe : heroes) {
            if (heroe.getId() == id) {
                // Convertir el heroe DB a un personaje del juego
                if ("guerrero".equals(heroe.getTipo())) {
                    // Buscar un Guerrero que coincida con el nombre
                    // (El constructor de Guerrero toma el nombre)
                    personaje = new Guerrero(heroe.getNombre());
                } else if ("mago".equals(heroe.getTipo())) {
                    // Buscar un Mago que coincida con el nombre
                    personaje = new Mago(heroe.getNombre());
                }
                break;
            }
        }

        return personaje;
    }
}
