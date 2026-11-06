package com.rpg.core;

import com.rpg.concurrency.AutoGuardadoDaemon;
import com.rpg.concurrency.GestorTurno;
import com.rpg.entities.Enemigo;
import com.rpg.entities.EstadoEfecto;
import com.rpg.entities.Gremio;
import com.rpg.entities.Guerrero;
import com.rpg.entities.Mago;
import com.rpg.entities.PerfilJugador;
import com.rpg.entities.Personaje;
import com.rpg.entities.enums.RaridadItem;
import com.rpg.entities.enums.TipoEfecto;
import com.rpg.exceptions.CapacidadExcedidaException;
import com.rpg.items.Inventario;
import com.rpg.items.Pocion;
import com.rpg.persistencia.ArchivosPartida;
import com.rpg.persistencia.GestorPartida;
import com.rpg.persistencia.PartidaGuardada;

import java.io.EOFException;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InvalidClassException;
import java.io.NotSerializableException;
import java.io.StreamCorruptedException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Random;

/** Coordina el flujo del juego: crear heroe, combatir, torneo, bestiario y guardado. */
public class MotorJuego {

    private static final String NOMBRE_GREMIO = "Guardianes del Alba";
    private static final int CURACION_POCION_VIDA = 30;
    private static final int MAXIMO_LINEAS_HISTORIAL = 200;
    private static final long ESPERA_CIERRE_MS = 2000L;

    private final Consola consola;
    private final RegistroCombate registro = new RegistroCombate();
    private final Arena arena = new Arena(registro, new Random());
    private final Bestiario bestiario = new Bestiario();
    private final TorneoManager torneo = new TorneoManager(arena, registro);
    // UNA sola instancia compartida por el hilo principal y el auto-guardado: sus metodos son synchronized.
    private final GestorPartida gestorPartida = new GestorPartida(ArchivosPartida.archivoPartida());

    // Candado de la partida: perfil, gremio, bestiario, ultimoGuardado y sesionActiva los toca el hilo principal y
    // tambien el hilo de auto-guardado, asi que todo acceso compartido va dentro de synchronized (candado).
    private final Object candado = new Object();
    private PerfilJugador perfil = new PerfilJugador();
    private Gremio gremio = new Gremio(NOMBRE_GREMIO);
    private String ultimoGuardado = "";   // estado de la partida en memoria (la ultima copia)
    // true cuando ya hay una partida en marcha en esta sesion (se creo un heroe, hubo torneo o se cargo una partida).
    // Mientras sea false el auto-guardado no escribe nada, para no pisar una partida vieja con una vacia.
    private boolean sesionActiva;
    private Thread hiloAutoGuardado;

    public MotorJuego(Consola consola) {
        this.consola = consola;
    }

    // ---- opciones del menu principal

    public void combateRapido() {
        registro.limpiar();
        Personaje jugador = crearJugador();
        Enemigo enemigo = elegirEnemigo();
        registro.agregar("%n=== %s vs %s ===", jugador.getNombre(), enemigo.getNombre());

        arena.iniciarCombate(jugador, enemigo);
        boolean huyo;
        try {
            huyo = combatir(jugador, enemigo);
        } finally {
            // Termine como termine el combate, los hilos de efectos se interrumpen y terminan limpios.
            arena.terminarCombate(jugador, enemigo);
        }

        cerrarCombate(jugador, enemigo, huyo);
        archivarRegistro();
        guardarProgreso();
    }

    public void jugarTorneo() {
        synchronized (candado) {
            sesionActiva = true;
        }
        registro.limpiar();
        Optional<Personaje> campeon = torneo.jugarTorneo();
        if (campeon.isEmpty()) {
            registro.agregar("%nEl torneo no se pudo completar.");
            return;
        }
        synchronized (candado) {
            registro.agregar("%nCAMPEON DEL TORNEO: %s", campeon.get().getNombre());
            perfil.desbloquearLogro("Campeon del Torneo");
        }
        archivarRegistro();
        guardarProgreso();
    }

    public void mostrarBestiario() {
        System.out.println("\n=== BESTIARIO ===");
        synchronized (candado) {
            System.out.print(bestiario.describir());
            System.out.printf("Victorias: %d | Derrotas: %d | Logros: %s%n",
                    perfil.getVictorias(), perfil.getDerrotas(), perfil.getLogros());
        }
    }

    // ---- creacion de personajes

    private Personaje crearJugador() {
        int clase = consola.leerEntero("Clase (1. Guerrero, 2. Mago): ", 1, 2);
        String nombre = consola.leerNombre("Nombre del heroe: ");
        Personaje jugador = switch (clase) {
            case 1 -> new Guerrero(nombre);
            default -> new Mago(nombre);
        };
        entregarKitInicial(jugador);
        synchronized (candado) {
            sesionActiva = true;
            jugador.unirseAGremio(gremio);
            System.out.printf("%s se une al gremio %s (%d miembros).%n",
                    jugador.getNombre(), gremio.getNombre(), gremio.getMiembros().size());
        }
        return jugador;
    }

    private void entregarKitInicial(Personaje jugador) {
        List<Pocion> kit = List.of(
                new Pocion("Pocion de Vida", CURACION_POCION_VIDA, RaridadItem.COMUN),
                new Pocion("Elixir de Regeneracion", 0, RaridadItem.RARO,
                        new EstadoEfecto("Regeneracion", TipoEfecto.CURACION_CONTINUA, 6, 3), false),
                new Pocion("Antidoto", 0, RaridadItem.COMUN, null, true));
        try {
            for (Pocion pocion : kit) {
                jugador.getInventario().agregar(pocion);
            }
        } catch (CapacidadExcedidaException e) {
            System.out.println("No se pudo entregar todo el kit: " + e.getMessage());
        }
    }

    private Enemigo elegirEnemigo() {
        List<String> nombres = bestiario.nombres();
        System.out.println("Rivales disponibles:");
        for (int i = 0; i < nombres.size(); i++) {
            System.out.printf("  %d. %s%n", i + 1, nombres.get(i));
        }
        while (true) {
            String texto = consola.leerLinea("Rival (numero o nombre): ");
            Enemigo enemigo = resolverRival(texto, nombres);
            if (enemigo != null) {
                return enemigo;
            }
            System.out.println("No existe ese rival.");
        }
    }

    private Enemigo resolverRival(String texto, List<String> nombres) {
        try {
            int numero = Integer.parseInt(texto);
            boolean valido = numero >= 1 && numero <= nombres.size();
            return valido ? bestiario.crearEnemigo(nombres.get(numero - 1)) : null;
        } catch (NumberFormatException e) {
            return bestiario.crearEnemigo(texto);
        }
    }

    // ---- bucle de combate

    /** @return true si el jugador huyo */
    private boolean combatir(Personaje jugador, Enemigo enemigo) {
        int ronda = 1;
        while (jugador.estaVivo() && enemigo.estaVivo()) {
            registro.agregar("%n-- Ronda %d --", ronda);
            if (arena.iniciarTurno(jugador) && turnoJugador(jugador, enemigo)) {
                return true;
            }
            arena.turnoAutomatico(enemigo, jugador);
            ronda++;
        }
        return false;
    }

    /**
     * Turno del jugador. Las acciones que no gastan turno (ver estado, cancelar) vuelven a pedir.
     * Si se agota el tiempo el turno se pierde.
     * @return true si el jugador decidio huir
     */
    private boolean turnoJugador(Personaje jugador, Enemigo enemigo) {
        boolean turnoGastado;
        do {
            registro.agregar(FormatoCombate.resumen(jugador));
            registro.agregar(FormatoCombate.resumen(enemigo));
            Accion accion = pedirAccion();
            if (!jugador.estaVivo() || !enemigo.estaVivo()) {
                return false; // mientras elegia, un efecto continuo (DoT/HoT) pudo decidir el combate
            }
            if (accion == Accion.HUIR) {
                return true;
            }
            turnoGastado = switch (accion) {
                case ATACAR -> {
                    arena.atacar(jugador, enemigo);
                    yield true;
                }
                case ESPECIAL -> arena.intentarEspecial(jugador, enemigo);
                case POCION -> usarPocion(jugador);
                case TIEMPO_AGOTADO -> perderTurno(jugador);
                default -> {
                    mostrarEstado(jugador);
                    yield false;
                }
            };
        } while (!turnoGastado && enemigo.estaVivo());
        return false;
    }

    /**
     * Pide la accion al jugador con un limite de tiempo.
     * Un hilo temporizador cuenta hacia atras; si llega a cero llama al callback y el turno se pierde. Si el jugador
     * responde antes, el temporizador se cancela con interrupt().
     */
    private Accion pedirAccion() {
        // TALLER CONCURRENCIA (resuelto): la lectura ya no bloquea sin limite de tiempo.
        System.out.println("1. Atacar  2. Habilidad especial  3. Usar pocion  4. Ver estado  0. Huir");
        GestorTurno temporizador = new GestorTurno();
        temporizador.iniciar(30, this::avisarTiempoAgotado);
        try {
            OptionalInt opcion = consola.leerEnteroConTiempo("Accion: ", 0, 4);
            if (opcion.isEmpty()) {
                return Accion.TIEMPO_AGOTADO;
            }
            return switch (opcion.getAsInt()) {
                case 1 -> Accion.ATACAR;
                case 2 -> Accion.ESPECIAL;
                case 3 -> Accion.POCION;
                case 4 -> Accion.ESTADO;
                default -> Accion.HUIR;
            };
        } finally {
            temporizador.cancelar();
            consola.descartarAvisoTiempo();
        }
    }

    /** TurnoCallback: corre en el hilo del temporizador, asi que solo hace cosas seguras entre hilos. */
    private void avisarTiempoAgotado() {
        System.out.println("\n[Tiempo agotado]");
        consola.notificarTiempoAgotado();
    }

    private boolean perderTurno(Personaje jugador) {
        registro.agregar("%s no eligio a tiempo y pierde el turno", jugador.getNombre());
        // Se espera un Enter: asi, si el jugador estaba escribiendo cuando se acabo el tiempo, esa linea tardia se
        // consume aqui y no se cuela como respuesta del turno siguiente.
        consola.leerLinea("Presiona Enter para continuar...");
        return true;
    }

    private boolean usarPocion(Personaje jugador) {
        Inventario<Pocion> inventario = jugador.getInventario();
        if (inventario.estaVacio()) {
            System.out.println("No tienes pociones.");
            return false;
        }
        int numero = 1;
        for (Pocion pocion : inventario) {
            System.out.printf("  %d. %s%n", numero++, pocion.getNombre());
        }
        int eleccion = consola.leerEntero("Pocion (0 = cancelar): ", 0, inventario.tamanio());
        if (eleccion == 0) {
            return false;
        }
        registro.agregar(jugador.usarPocion(inventario.quitar(eleccion - 1)));
        return true;
    }

    private void mostrarEstado(Personaje jugador) {
        System.out.printf("Habilidades: %s | Arma: %s | Defensa: %d | Pociones: %d%n",
                jugador.getHabilidades(),
                jugador.getArma() != null ? jugador.getArma().getNombre() : "ninguna",
                jugador.getDefensa(), jugador.getInventario().tamanio());
        for (EstadoEfecto efecto : jugador.getEfectos()) {
            System.out.printf("Efecto activo: %s (%d ticks)%n", efecto.getNombre(), efecto.getTurnosRestantes());
        }
    }

    // ---- resultado del combate

    private void cerrarCombate(Personaje jugador, Enemigo enemigo, boolean huyo) {
        synchronized (candado) {
            if (huyo) {
                registro.agregar("%s huye del combate.", jugador.getNombre());
            } else if (jugador.estaVivo()) {
                registro.agregar("%nVictoria de %s!", jugador.getNombre());
                perfil.registrarVictoria();
                bestiario.registrarEnemigoDerrotado(enemigo.getNombre());
                desbloquearLogros(jugador, enemigo);
                repartirBotin(jugador);
            } else {
                registro.agregar("%n%s ha caido ante %s.", jugador.getNombre(), enemigo.getNombre());
                perfil.registrarDerrota();
            }
        }
    }

    private void desbloquearLogros(Personaje jugador, Enemigo enemigo) {
        boolean intacto = jugador.getVida() == jugador.getVidaMaxima();
        List<String> candidatos = List.of("Primera Victoria", "Cazador de " + enemigo.getNombre(),
                intacto ? "Intocable" : "");
        for (String logro : candidatos) {
            if (!logro.isEmpty() && perfil.desbloquearLogro(logro)) {
                registro.agregar("Logro desbloqueado: %s", logro);
            }
        }
    }

    private void repartirBotin(Personaje jugador) {
        try {
            jugador.getInventario().agregar(new Pocion("Pocion de Vida", CURACION_POCION_VIDA, RaridadItem.COMUN));
            registro.agregar("Botin: Pocion de Vida.");
        } catch (CapacidadExcedidaException e) {
            registro.agregar("Botin perdido: %s", e.getMessage());
        }
    }

    // ---- guardado (memoria + disco)

    /**
     * Guarda la partida: deja el resumen en memoria (ultimoGuardado) y escribe el estado en disco con
     * serializacion (partidas/partida.dat). Se llama al terminar una pelea o torneo.
     */
    // TALLER ARCHIVOS: el estado se guarda en memoria (ultimoGuardado) y en disco con serializacion.
    public void guardarProgreso() {
        synchronized (candado) {
            guardar("Progreso guardado");
        }
    }

    /**
     * Lo llama el hilo daemon cada cierto tiempo (tambien en mitad de un combate: guarda las estadisticas actuales
     * de los personajes). No hace nada si no hay partida en marcha o si nada cambio desde el ultimo guardado.
     */
    // TALLER ARCHIVOS: guardado periodico en disco cada cierto intervalo (hilo daemon).
    public void autoguardar() {
        synchronized (candado) {
            if (sesionActiva && !construirResumen().equals(ultimoGuardado)) {
                guardar("Autoguardado");
            }
        }
    }

    /** Llamar con el candado tomado. */
    private void guardar(String etiqueta) {
        ultimoGuardado = construirResumen();
        try {
            gestorPartida.guardar(new PartidaGuardada(perfil, gremio, bestiario.getDerrotados(), ultimoGuardado));
            System.out.printf("[%s: en memoria y en %s]%n", etiqueta, gestorPartida.getArchivo().getPath());
        } catch (NotSerializableException e) {
            System.out.printf("[%s: solo en memoria; hay un objeto que no es Serializable: %s]%n",
                    etiqueta, e.getMessage());
        } catch (IOException e) {
            System.out.printf("[%s: solo en memoria; no se pudo escribir el archivo (%s)]%n",
                    etiqueta, e.getMessage());
        }
    }

    private String construirResumen() {
        StringBuilder contenido = new StringBuilder();
        contenido.append("=== PARTIDA RPG ===").append(System.lineSeparator());
        contenido.append("Victorias: ").append(perfil.getVictorias()).append(System.lineSeparator());
        contenido.append("Derrotas: ").append(perfil.getDerrotas()).append(System.lineSeparator());
        contenido.append("Logros: ").append(String.join(", ", perfil.getLogros())).append(System.lineSeparator());
        contenido.append("Gremio ").append(gremio.getNombre()).append(": ")
                .append(gremio.getMiembros().size()).append(" miembros").append(System.lineSeparator());
        for (Personaje miembro : gremio.getMiembros()) {
            contenido.append(String.format("  - %s (%s) vida %d/%d%n", miembro.getNombre(), miembro.getTipo(),
                    miembro.getVida(), miembro.getVidaMaxima()));
        }
        contenido.append("Enemigos derrotados: ").append(bestiario.resumenDerrotados()).append(System.lineSeparator());
        contenido.append(System.lineSeparator()).append("--- Ultimo combate ---").append(System.lineSeparator());
        contenido.append(registro.getTexto());
        return contenido.toString();
    }

    public String getUltimoGuardado() {
        synchronized (candado) {
            return ultimoGuardado;
        }
    }

    // ---- auto-guardado en segundo plano

    /** Arranca el hilo daemon que guarda cada cierto tiempo. */
    public void iniciarAutoGuardado() {
        if (hiloAutoGuardado != null && hiloAutoGuardado.isAlive()) {
            return;
        }
        hiloAutoGuardado = AutoGuardadoDaemon.iniciar(5000, this::autoguardar);
    }

    /**
     * Cierre ordenado: pide al auto-guardado que pare y espera lo justo para que no quede a medias.
     * Si esta dormido termina al instante; si justo estaba guardando, termina ese guardado primero.
     */
    public void cerrar() {
        if (hiloAutoGuardado == null) {
            return;
        }
        hiloAutoGuardado.interrupt();
        try {
            hiloAutoGuardado.join(ESPERA_CIERRE_MS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    // ---- historial de combates (texto) y archivos

    /** Agrega el registro del combate al final del historial (sin borrar los anteriores). */
    // TALLER ARCHIVOS: vuelve a escribir el historial de combates en un archivo de texto (sin borrar los anteriores).
    private void archivarRegistro() {
        try {
            registro.guardarEnArchivo(ArchivosPartida.archivoHistorial(), true);
        } catch (IOException e) {
            System.out.println("No se pudo guardar el historial de combates: " + e.getMessage());
        }
    }

    public void mostrarHistorial() {
        File archivo = ArchivosPartida.archivoHistorial();
        try {
            List<String> lineas = RegistroCombate.leerDeArchivo(archivo);
            if (lineas.isEmpty()) {
                System.out.println("El historial esta vacio.");
                return;
            }
            System.out.println("\n=== HISTORIAL DE COMBATES (" + archivo.getName() + ") ===");
            int desde = Math.max(0, lineas.size() - MAXIMO_LINEAS_HISTORIAL);
            if (desde > 0) {
                System.out.printf("(se muestran las ultimas %d de %d lineas)%n", MAXIMO_LINEAS_HISTORIAL, lineas.size());
            }
            lineas.subList(desde, lineas.size()).forEach(System.out::println);
        } catch (FileNotFoundException e) {
            System.out.println("Todavia no hay historial: juega un combate primero (no existe " + archivo.getPath() + ").");
        } catch (IOException e) {
            System.out.println("No se pudo leer el historial: " + e.getMessage());
        }
    }

    public void mostrarArchivos() {
        System.out.println("\n=== ARCHIVOS DEL JUEGO ===");
        System.out.println("Carpeta:   " + ArchivosPartida.describir(new File(ArchivosPartida.CARPETA)));
        System.out.println("Partida:   " + ArchivosPartida.describir(ArchivosPartida.archivoPartida()));
        System.out.println("Historial: " + ArchivosPartida.describir(ArchivosPartida.archivoHistorial()));
    }

    /** Avisa al arrancar si ya hay una partida guardada, usando File.exists() y File.length(). */
    public void avisarPartidaExistente() {
        File archivo = ArchivosPartida.archivoPartida();
        if (archivo.exists()) {
            System.out.printf("Hay una partida guardada (%s, %d bytes). Usa la opcion de cargar partida para continuarla.%n",
                    archivo.getName(), archivo.length());
        }
    }

    // TALLER ARCHIVOS: carga la partida serializada desde el archivo .dat y continua con el estado guardado.
    public void cargarPartida() {
        try {
            PartidaGuardada partida = gestorPartida.cargar();
            synchronized (candado) {
                perfil = partida.getPerfil();
                gremio = partida.getGremio();
                bestiario.restaurarDerrotados(partida.getDerrotados());
                ultimoGuardado = partida.getResumen();
                sesionActiva = true;
                System.out.printf("Partida cargada (guardada el %s).%n", formatearFecha(partida.getGuardadaEn()));
                System.out.printf("Victorias: %d | Derrotas: %d | Logros: %s%n",
                        perfil.getVictorias(), perfil.getDerrotas(), perfil.getLogros());
                System.out.printf("Gremio %s: %d miembros%n", gremio.getNombre(), gremio.getMiembros().size());
                for (Personaje miembro : gremio.getMiembros()) {
                    System.out.printf("  - %s (%s) vida %d/%d%n", miembro.getNombre(), miembro.getTipo(),
                            miembro.getVida(), miembro.getVidaMaxima());
                }
            }
        } catch (FileNotFoundException e) {
            System.out.println("No hay ninguna partida guardada todavia (no existe "
                    + gestorPartida.getArchivo().getPath() + ").");
        } catch (InvalidClassException e) {
            if (e.getMessage() != null && e.getMessage().startsWith("filter status")) {
                System.out.println("El archivo contiene objetos que el juego no permite y se rechazo por seguridad.");
            } else {
                System.out.println("La partida es de otra version del juego y no se puede cargar: " + e.getMessage());
            }
        } catch (StreamCorruptedException | EOFException e) {
            System.out.println("El archivo de partida esta danado o incompleto y no se puede cargar.");
        } catch (IOException e) {
            System.out.println("No se pudo leer la partida: " + e.getMessage());
        } catch (ClassNotFoundException e) {
            System.out.println("La partida usa una clase que ya no existe en el juego: " + e.getMessage());
        }
    }

    // TALLER ARCHIVOS: menu para borrar la partida guardada o el historial de combates.
    public void borrarArchivos() {
        System.out.printf("1. Partida guardada (%s)  2. Historial de combates (%s)  0. Cancelar%n",
                ArchivosPartida.NOMBRE_PARTIDA, ArchivosPartida.NOMBRE_HISTORIAL);
        int opcion = consola.leerEntero("Que borrar: ", 0, 2);
        if (opcion == 0) {
            return;
        }
        File archivo = opcion == 1 ? ArchivosPartida.archivoPartida() : ArchivosPartida.archivoHistorial();
        String respuesta = consola.leerLinea("Seguro que quieres borrar " + archivo.getName() + "? (s/n): ");
        if (!respuesta.equalsIgnoreCase("s")) {
            System.out.println("No se borro nada.");
            return;
        }
        try {
            if (opcion == 1) {
                // Files.deleteIfExists: false = no habia archivo; si no se puede borrar, lanza IOException con el motivo.
                boolean borrado = gestorPartida.borrar();
                System.out.println(borrado ? "Partida borrada." : "No habia ninguna partida guardada.");
            } else if (!archivo.exists()) {
                System.out.println("No hay historial que borrar.");
            } else if (ArchivosPartida.borrarConFile(archivo)) {
                // File.delete(): solo da true/false; false aqui significa que existe pero no se pudo borrar.
                System.out.println("Historial borrado.");
            } else {
                System.out.println("No se pudo borrar el historial (esta en uso o no hay permisos).");
            }
        } catch (IOException e) {
            System.out.println("No se pudo borrar " + archivo.getName() + ": " + e.getMessage());
        }
    }

    private static String formatearFecha(long milisegundos) {
        LocalDateTime fecha = LocalDateTime.ofInstant(Instant.ofEpochMilli(milisegundos), ZoneId.systemDefault());
        return fecha.toString().replace('T', ' ');
    }
}
