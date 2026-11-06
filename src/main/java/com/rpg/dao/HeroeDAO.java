package com.rpg.dao;

import com.rpg.model.Heroe;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object (DAO) para la entidad Heroe.
 * <p>
 * Encapsula toda la logica de acceso a datos relacionada con los heroes.
 * Solo usa JDBC (PreparedStatement) y nunca se preocupa de la vista o del controlador.
 * </p>
 * <p>
 * Metodos:
 * <ul>
 *   <li><b>insertar(Heroe)</b> - inserta un heroe, devuelve filas afectadas</li>
 *   <li><b>listar()</b> - lista todos los heroes, devuelve List&lt;Heroe&gt;</li>
 *   <li><b>buscarPorTipo(String tipo)</b> - busca heroes por tipo (guerrero/mago)</li>
 *   <li><b>actualizarNivel(int id, int nivel)</b> - actualiza el nivel de un heroe</li>
 *   <li><b>eliminar(int id)</b> - elimina un heroe por su ID</li>
 * </ul>
 * </p>
 */
public class HeroeDAO {

    /**
     * Constructor privado. Solo metodos estaticos.
     */
    private HeroeDAO() {
    }

    /**
     * Inserta un nuevo heroe en la base de datos.
     *
     * @param heroe el objeto Heroe a insertar
     * @return numero de filas afectadas (1 si fue exitoso)
     * @throws SQLException si ocurre un error de BD
     */
    public static int insertar(Heroe heroe) throws SQLException {
        String sql = "INSERT INTO heroes (nombre, tipo, vida_maxima, nivel) VALUES (?, ?, ?, ?)";
        int filasAfectadas = 0;

        // try-with-resources: cierra automagicamente el PreparedStatement y la Connection
        try (Connection conn = ConexionDB.obtenerConexion();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, heroe.getNombre());
            stmt.setString(2, heroe.getTipo());
            stmt.setInt(3, heroe.getVidaMaxima());
            stmt.setInt(4, heroe.getNivel());

            filasAfectadas = stmt.executeUpdate();
        }

        return filasAfectadas;
    }

    /**
     * Lista todos los heroes de la base de datos.
     *
     * @return lista de heroes (List&lt;Heroe&gt;)
     * @throws SQLException si ocurre un error de BD
     */
    public static List<Heroe> listar() throws SQLException {
        List<Heroe> heroes = new ArrayList<>();
        String sql = "SELECT id, nombre, tipo, vida_maxima, nivel FROM heroes";

        // try-with-resources: cierra automagicamente Connection y PreparedStatement
        try (Connection conn = ConexionDB.obtenerConexion();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                Heroe heroe = new Heroe();
                heroe.setId(rs.getInt("id"));
                heroe.setNombre(rs.getString("nombre"));
                heroe.setTipo(rs.getString("tipo"));
                heroe.setVidaMaxima(rs.getInt("vida_maxima"));
                heroe.setNivel(rs.getInt("nivel"));
                heroes.add(heroe);
            }
        }

        return heroes;
    }

    /**
     * Busca heroes por tipo (guerrero o mago).
     *
     * @param tipo el tipo de heroe a buscar
     * @return lista de heroes que coinciden con el tipo
     * @throws SQLException si ocurre un error de BD
     */
    public static List<Heroe> buscarPorTipo(String tipo) throws SQLException {
        List<Heroe> heroes = new ArrayList<>();
        String sql = "SELECT id, nombre, tipo, vida_maxima, nivel FROM heroes WHERE tipo = ?";

        // try-with-resources: cierra automagicamente Connection y PreparedStatement
        try (Connection conn = ConexionDB.obtenerConexion();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, tipo);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Heroe heroe = new Heroe();
                    heroe.setId(rs.getInt("id"));
                    heroe.setNombre(rs.getString("nombre"));
                    heroe.setTipo(rs.getString("tipo"));
                    heroe.setVidaMaxima(rs.getInt("vida_maxima"));
                    heroe.setNivel(rs.getInt("nivel"));
                    heroes.add(heroe);
                }
            }
        }

        return heroes;
    }

    /**
     * Actualiza el nivel de un heroe por su ID.
     *
     * @param id    el ID del heroe a actualizar
     * @param nivel el nuevo nivel
     * @return numero de filas afectadas (1 si se actualizo, 0 si no existe)
     * @throws SQLException si ocurre un error de BD
     */
    public static int actualizarNivel(int id, int nivel) throws SQLException {
        String sql = "UPDATE heroes SET nivel = ? WHERE id = ?";
        int filasAfectadas = 0;

        // try-with-resources: cierra automagicamente Connection y PreparedStatement
        try (Connection conn = ConexionDB.obtenerConexion();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, nivel);
            stmt.setInt(2, id);

            filasAfectadas = stmt.executeUpdate();
        }

        return filasAfectadas;
    }

    /**
     * Elimina un heroe por su ID.
     *
     * @param id el ID del heroe a eliminar
     * @return numero de filas afectadas (1 si se elimino, 0 si no existe)
     * @throws SQLException si ocurre un error de BD
     */
    public static int eliminar(int id) throws SQLException {
        String sql = "DELETE FROM heroes WHERE id = ?";
        int filasAfectadas = 0;

        // try-with-resources: cierra automagicamente Connection y PreparedStatement
        try (Connection conn = ConexionDB.obtenerConexion();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, id);

            filasAfectadas = stmt.executeUpdate();
        }

        return filasAfectadas;
    }
}
