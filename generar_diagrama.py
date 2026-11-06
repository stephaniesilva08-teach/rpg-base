#!/usr/bin/env python3
"""Genera diagrama_mvc.png a partir del HTML corregido."""
import os
from PIL import Image, ImageDraw, ImageFont

OUTPUT = "/home/hp/rpg-base/diagrama_mvc.png"

# --- Fuentes ---
font_paths = [
    "/usr/share/fonts/truetype/dejavu/DejaVuSansMono.ttf",
    "/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf",
    "/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf",
]
fonte_normal = None
fonte_bold = None
for fp in font_paths:
    if os.path.exists(fp):
        try:
            fnt = ImageFont.truetype(fp, 14)
            if "Bold" in fp:
                fonte_bold = fnt
            else:
                fonte_normal = fnt
        except Exception:
            pass

if not fonte_normal:
    fonte_normal = ImageFont.load_default()
if not fonte_bold:
    fonte_bold = fonte_normal

# Colores
AZUL_CLARO = (137, 180, 250)
AZUL_CLIENTE = (166, 173, 200)
GRIS_OSCURO = (49, 50, 68)
GRIS_MEDIO = (69, 71, 90)
GRIS_SUAVE = (90, 93, 122)
AZUL_OSCURO = (137, 180, 250)
VERDE = (146, 192, 144)
BLANCO = (255, 255, 255)
NEGRO = (0, 0, 0)
GRIS_CLARO = (200, 200, 200)

# Crear canvas
width, height = 1000, 620
img = Image.new("RGB", (width, height), "#181825")
draw = ImageDraw.Draw(img)

# Fondo degradado (líneas horizontales para simular degradado)
for y in range(height):
    for x in range(width):
        if y < 200:
            img.putpixel((x, y), (17, 17, 27))
        else:
            img.putpixel((x, y), (30, 30, 46))

# ---- Dibujar boxes y texto ----
def draw_box(x1, y1, x2, y2, fill, outline, width=3):
    draw.rectangle([x1, y1, x2, y2], fill=fill, outline=outline, width=width)

def draw_text(x, y, text, fill, font):
    draw.text((x, y), text, fill=fill, font=font)

# ---- Main (Juego) ----
draw_box(120, 80, 480, 200, GRIS_OSCURO, AZUL_CLARO, 3)
draw.text((200, 108), "Main", fill=AZUL_CLARO, font=fonte_bold)
draw.text((200, 140), "opción 'Cargar heroe", fill=GRIS_SUAVE, font=fonte_normal)
draw.text((200, 160), "desde BD", fill=GRIS_SUAVE, font=fonte_normal)

# Flecha Main -> Controller
draw.line([(400, 140), (440, 160)], fill=(137, 180, 250), width=4)
draw.polygon([(415, 155), (415, 165), (435, 160)], fill=(137, 180, 250))
draw.text((395, 130), "client", fill=AZUL_CLARO, font=fonte_normal)

# ---- HeroeController ----
draw_box(440, 120, 800, 220, GRIS_OSCURO, AZUL_CLARO, 3)
draw.text((620, 145), "HeroeController", fill=AZUL_CLARO, font=fonte_bold)
draw.text((620, 175), "validar(nombre, tipo, vida, nivel)", fill=GRIS_SUAVE, font=fonte_normal)
draw.text((620, 195), "insertar() -> executeUpdate", fill=GRIS_SUAVE, font=fonte_normal)
draw.text((620, 215), "listar() -> executeQuery", fill=GRIS_SUAVE, font=fonte_normal)
draw.text((620, 235), "actualizarNivel() / eliminar() -> executeUpdate", fill=GRIS_SUAVE, font=fonte_normal)
draw.text((620, 260), "PreparedStatement · try-with-resources", fill=GRIS_SUAVE, font=fonte_normal)

# Flecha Controller -> DAO
draw.line([(800, 165), (840, 165)], fill=(166, 173, 200), width=4)
draw.polygon([(820, 160), (820, 170), (840, 165)], fill=(166, 173, 200))

# ---- HeroeDAO ----------
draw_box(840, 100, 1400, 220, GRIS_OSCURO, AZUL_CLARO, 3)
draw.text((1120, 128), "HeroeDAO", fill=AZUL_CLARO, font=fonte_bold)
draw.text((1120, 160), "insertar(Heroe) -> executeUpdate", fill=GRIS_SUAVE, font=fonte_normal)
draw.text((1120, 185), "listar() -> executeQuery", fill=GRIS_SUAVE, font=fonte_normal)
draw.text((1120, 210), "actualizarNivel() / eliminar() -> executeUpdate", fill=GRIS_SUAVE, font=fonte_normal)
draw.text((1120, 235), "PreparedStatement · try-with-resources", fill=GRIS_SUAVE, font=fonte_normal)

# ----- MySQL (Base de datos) -----
draw_box(1400, 80, 1880, 230, "#1e1e2e", (88, 91, 112), 2)
draw.text((1640, 105), "MySQL", fill=AZUL_CLARO, font=fonte_bold)
draw.text((1640, 130), "Base de datos: rpg_grupal_db", fill=GRIS_SUAVE, font=fonte_normal)
draw.text((1640, 155), "Tabla: heroes", fill=AZUL_CLARO, font=fonte_bold)
draw.text((1640, 175), "id INT AUTO_INCREMENT", fill=GRIS_SUAVE, font=fonte_normal)
draw.text((1640, 195), "nombre VARCHAR(100)", fill=GRIS_SUAVE, font=fonte_normal)
draw.text((1640, 215), "tipo ENUM(guerrero,mago)", fill=GRIS_SUAVE, font=fonte_normal)
draw.text((1640, 235), "vida_maxima INT", fill=GRIS_SUAVE, font=fonte_normal)
draw.text((1640, 255), "nivel INT", fill=GRIS_SUAVE, font=fonte_normal)

# Flecha DAO -> BD
draw.line([(1400, 160), (1385, 165)], fill=(88, 91, 112), width=2)

# Flecha BD -> DAO (retorno)
draw.line([(1375, 165), (1400, 165)], fill=(88, 91, 112), width=2)

# Flecha Controller -> Main
draw.line([(605, 180), (605, 165)], fill=(166, 173, 200), width=4)
draw.text((580, 150), "OK", fill=AZUL_CLARO, font=fonte_normal)

# Flecha Main -> DAO (via Controller)
draw.line([(400, 170), (550, 165)], fill=(137, 180, 250), width=2)

# ---- Explicación ----
draw.text((20, 300), "MVC ESTRICTO:", fill=AZUL_CLARO, font=fonte_bold)
draw.text((20, 330), "View (HeroeUI): solo Scanner + System.out", fill=GRIS_SUAVE, font=fonte_normal)
draw.text((20, 355), "Controller (HeroeController): valida y coordina", fill=GRIS_SUAVE, font=fonte_normal)
draw.text((20, 380), "DAO (HeroeDAO): único que usa JDBC", fill=GRIS_SUAVE, font=fonte_normal)
draw.text((20, 405), "Base de datos (MySQL): persistencia de Heroe", fill=GRIS_SUAVE, font=fonte_normal)

# Nota
draw.text((20, 460), "Insertar heroe: INSERT INTO heroes (nombre, tipo, vida_maxima, nivel) VALUES (?, ?, ?, ?)", fill=GRIS_SUAVE, font=fonte_normal)
draw.text((20, 485), "               executeUpdate() -> 1 fila afectada", fill=GRIS_SUAVE, font=fonte_normal)

# Guardar
img.save("/home/hp/rpg-base/diagrama_mvc.png", "PNG", quality=95)
print("PNG generado con éxito")
print(f"Compatimento: {os.path.getsize('/home/hp/rpg-base/diagrama_mvc.png')} bytes")
