-- ============================================================
-- ESQUEMA DE LA BASE DE DATOS PARA EL EJERCICIO JDBC + MVC
-- Unidad 5.2: Registro de Heroes persistente
-- ============================================================

-- Base de datos: rpg_grupal_db
-- Tabla: heroes

-- Si la base de datos no existe, crearla
CREATE DATABASE IF NOT EXISTS rpg_grupal_db;
USE rpg_grupal_db;

-- Tabla de heroes
CREATE TABLE IF NOT EXISTS heroes (
    id INT AUTO_INCREMENT PRIMARY KEY,
    nombre VARCHAR(100) NOT NULL,
    tipo ENUM('guerrero', 'mago') NOT NULL,
    vida_maxima INT NOT NULL,
    nivel INT NOT NULL,
    creado_en TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY idx_nombre (nombre)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
