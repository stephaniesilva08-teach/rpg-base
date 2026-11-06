package com.rpg.dao;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Clase responsable de gestionar la conexion a la base de datos MySQL.
 * <p>
 * Lee las credenciales desde un archivo config.txt ubicado en la raiz del proyecto.
 * El archivo tiene formato clave=valor:
 * <ul>
 *   <li>url=jdbc:mysql://localhost:3306/NOMBRE_DE_LA_BD</li>
 *   <li>usuario=su_usuario</li>
 *   <li>clave=su_clave</li>
 * </ul>
 * </p>
 * <p>
 * La URL JDBC tiene el siguiente formato:
 * <ul>
 *   <li><b>protocolo:</b> jdbc:mysql:// - indica que se conecta a MySQL via JDBC</li>
 *   <li><b>host:</b> localhost - direccion IP o nombre del servidor MySQL</li>
 *   <li><b>puerto:</b> 3306 - puerto por defecto de MySQL</li>
 *   <li><b>esquema:</b> NOMBRE_DE_LA_BD - nombre de la base de datos a utilizar</li>
 * </ul>
 * </p>
 * <p>
 * Nota: No se hardcodean datos sensibles en el codigo fuente. El archivo config.txt
 * debe estar en .gitignore y no debe ser subido al repositorio.
 * </p>
 */
public class ConexionDB {

    // Ruta relativa del archivo de configuracion (en la raiz del proyecto)
    private static final String CONFIG_FILE = "config.txt";

    // Atributos de conexion cargados desde el archivo
    private static String url;
    private static String usuario;
    private static String clave;

    /**
     * Constructor privado para evitar instanciacion.
     * Esta clase solo tiene metodos estaticos.
     */
    private ConexionDB() {
    }

    /**
     * Carga las credenciales desde el archivo config.txt.
     * Debe llamarse al inicio de la aplicacion antes de usar la conexion.
     *
     * @throws IOException si el archivo no existe o no se puede leer
     * @throws IllegalArgumentException si faltan claves o hay errores de formato
     */
    public static void cargarCredenciales() throws IOException {
        File archivo = new File(CONFIG_FILE);
        if (!archivo.exists()) {
            throw new FileNotFoundException("No se encontro el archivo de configuracion: " + CONFIG_FILE);
        }

        url = null;
        usuario = null;
        clave = null;

        // Leer el archivo linea por linea con BufferedReader
        try (BufferedReader lector = new BufferedReader(new FileReader(archivo))) {
            String linea;
            while ((linea = lector.readLine()) != null) {
                // Ignorar lineas vacias
                if (linea.isBlank()) {
                    continue;
                }
                // Separar clave y valor usando split con limite 2
                String[] partes = linea.split("=", 2);
                if (partes.length == 2) {
                    String claveNombre = partes[0].trim();
                    String valor = partes[1].trim();

                    switch (claveNombre) {
                        case "url":
                            url = valor;
                            break;
                        case "usuario":
                            usuario = valor;
                            break;
                        case "clave":
                            clave = valor;
                            break;
                    }
                } else {
                    throw new IllegalArgumentException("Formato invalido en línea: " + linea);
                }
            }
        }

        // Verificar que todas las claves fueron encontradas
        if (url == null || usuario == null || clave == null) {
            throw new IllegalArgumentException(
                "Faltan claves en el archivo de configuracion. " +
                "Requiere: url, usuario, clave"
            );
        }
    }

    /**
     * Devuelve una conexion a la base de datos MySQL.
     *
     * Explica la URL:
     * <ul>
     *   <li><b>jdbc:mysql://</b> - protocolo JDBC para MySQL</li>
     *   <li><b>localhost</b> - host (servidor MySQL)</li>
     *   <li><b>3306</b> - puerto default de MySQL</li>
     *   <li><b>rpg_grupal_db</b> - esquema/base de datos a usar</li>
     * </ul>
     *
     * @return Connection con el driver MySQL
     * @throws SQLException si no se puede establecer la conexion
     */
    public static Connection obtenerConexion() throws SQLException {
        // Cargar credenciales si no se han cargado aun
        try {
            cargarCredenciales();
        } catch (IOException e) {
            throw new SQLException("No se pudo cargar la configuracion: " + e.getMessage(), e);
        }

        // Establecer la conexion
        return DriverManager.getConnection(url, usuario, clave);
    }
}
