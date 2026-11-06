package com.rpg.entities;

import com.rpg.concurrency.EfectoContinuoTask;
import com.rpg.entities.enums.EstadoPersonaje;
import com.rpg.entities.enums.TipoPersonaje;
import com.rpg.exceptions.SinManaException;
import com.rpg.items.Inventario;
import com.rpg.items.Pocion;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;

/**
 * Personaje de combate.
 *
 * <p>Concurrencia: la vida, el estado y los efectos los tocan varios hilos a la vez (el turno del jugador, los
 * hilos de los efectos DoT/HoT, las semifinales y el auto-guardado). Por eso todo acceso a esas variables compartidas
 * va con {@code synchronized} sobre el propio personaje.</p>
 *
 * <p>Archivos: es Serializable. Los hilos y la escucha de efectos son {@code transient} porque describen lo que
 * esta corriendo en este momento, no el estado de la partida.</p>
 */
public abstract class Personaje implements ICombatiente, Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private static final int VIDA_BASE = 100;
    private static final int ATAQUE_BASE = 10;
    private static final int DEFENSA_BASE = 5;
    private static final int CAPACIDAD_INVENTARIO = 3;
    private static final int DANIO_MINIMO = 1;
    private static final double PORCENTAJE_TOTAL = 100.0;
    private static final long ESPERA_CANCELACION_MS = 1000L;

    private String nombre;
    private final TipoPersonaje tipo;
    private int vidaMaxima;
    private int vida;
    private final int ataqueBase;
    private final int defensaBase;
    private EstadoPersonaje estado = EstadoPersonaje.VIVO;

    private Arma arma;                  // asociacion 0..1
    private Gremio gremio;              // asociacion bidireccional 0..1
    private final Inventario<Pocion> inventario = new Inventario<>(CAPACIDAD_INVENTARIO); // composicion
    private final Set<String> habilidades = new HashSet<>();
    private final Map<String, EstadoEfecto> efectos = new LinkedHashMap<>();

    // transient: no se guardan. Despues de cargar una partida valen null / vacio hasta que el juego los vuelva a armar.
    private transient Map<String, Thread> hilosEfectos = new HashMap<>();
    private transient volatile Consumer<String> escuchaEfectos;

    protected Personaje(String nombre, TipoPersonaje tipo) {
        this(nombre, tipo, VIDA_BASE, ATAQUE_BASE, DEFENSA_BASE);
    }

    protected Personaje(String nombre, TipoPersonaje tipo, int vidaMaxima, int ataqueBase, int defensaBase) {
        validarNombre(nombre);
        if (vidaMaxima <= 0 || ataqueBase < 0 || defensaBase < 0) {
            throw new IllegalArgumentException("Estadisticas invalidas para " + nombre);
        }
        this.nombre = nombre;
        this.tipo = tipo;
        this.vidaMaxima = vidaMaxima;
        this.vida = vidaMaxima;
        this.ataqueBase = ataqueBase;
        this.defensaBase = defensaBase;
    }

    private static void validarNombre(String nombre) {
        if (nombre == null || nombre.isBlank()) {
            throw new IllegalArgumentException("El nombre no puede estar vacio");
        }
    }

    // ---- combate

    /**
     * Ataque especial propio de cada clase.
     * @return descripcion de lo ocurrido, para el registro de combate
     */
    public abstract String realizarAtaqueEspecial(Personaje objetivo) throws SinManaException;

    /** Hook que se ejecuta al comenzar el turno (por ejemplo, el mago regenera mana). */
    public void alIniciarTurno() {
    }

    public int calcularDanio() {
        double bruto = ataqueBase + tipo.getBonificadorAtaque();
        if (arma != null) {
            bruto += arma.getDanioEfectivo();
        }
        return (int) Math.round(bruto);
    }

    @Override
    public void defender(int danoRecibido) {
        // Con enteros, 100 / 123 daria 0: por eso se divide con un double
        double factorMitigacion = PORCENTAJE_TOTAL / (PORCENTAJE_TOTAL + getDefensa());
        int danioFinal = (int) Math.round(danoRecibido * factorMitigacion);
        recibirDanio(Math.max(DANIO_MINIMO, danioFinal));
    }

    // ---- vida

    // TALLER CONCURRENCIA (resuelto): leer la vida, calcular y escribirla son tres pasos. Si dos hilos los mezclan
    // (un veneno y un ataque a la vez) se pierde una actualizacion. synchronized los convierte en una sola operacion
    // indivisible. Se eligio sobre AtomicInteger porque setVida tambien acota la vida a 0..vidaMaxima y cambia el
    // estado a MUERTO: son VARIAS variables que deben cambiar juntas, y una variable atomica sola no lo garantiza.
    public synchronized void recibirDanio(int cantidad) {
        if (cantidad < 0) {
            throw new IllegalArgumentException("El danio no puede ser negativo");
        }
        setVida(vida - cantidad);
    }

    /**
     * INVESTIGACION OBLIGATORIA: ReentrantLock
     * <p>
     * Prefeririamos ReentrantLock frente a synchronized cuando necesitemos:
     * 1. Adquisicion no bloqueante mediante tryLock() o con limite de tiempo tryLock(timeout).
     * 2. Soporte para interrupcion de hilos en espera mediante lockInterruptibly().
     * 3. Politicas de equidad (fairness) para asignar el turno al hilo que mas tiempo lleva esperando.
     * 4. Multiples variables de condicion independientes (Condition) asociadas al mismo cerrojo.
     * <p>
     * IMPORTANTE: unlock() debe colocarse siempre en el bloque finally; de lo contrario,
     * si ocurre una excepcion durante el calculo, el cerrojo nunca se liberara (deadlock permanente).
     */
    public void recibirDanioConLock(int cantidad) {
        if (cantidad < 0) {
            throw new IllegalArgumentException("El danio no puede ser negativo");
        }
        ReentrantLock vidaLock = new ReentrantLock();
        vidaLock.lock();
        try {
            synchronized (this) {
                setVida(vida - cantidad);
            }
        } finally {
            vidaLock.unlock();
        }
    }

    /** @return vida realmente recuperada */
    public synchronized int curar(int cantidad) {
        if (cantidad < 0) {
            throw new IllegalArgumentException("La curacion no puede ser negativa");
        }
        if (!estaVivo()) {
            return 0;
        }
        int vidaAntes = vida;
        setVida(vida + cantidad);
        return vida - vidaAntes;
    }

    public synchronized void restaurarVida() {
        interrumpirTodosLosEfectos();
        efectos.clear();
        estado = EstadoPersonaje.VIVO;
        vida = vidaMaxima;
    }

    @Override
    public synchronized boolean estaVivo() {
        return estado != EstadoPersonaje.MUERTO;
    }

    // ---- efectos DoT / HoT: cada efecto corre en su propio hilo (EfectoContinuoTask)

    public void aplicarEfecto(EstadoEfecto efecto) {
        aplicarEfecto(efecto, 1000);
    }

    /** Aplica el efecto y lanza su hilo. Si ya habia uno con el mismo nombre, se cancela y se reemplaza. */
    public synchronized void aplicarEfecto(EstadoEfecto efecto, long intervaloMs) {
        if (!estaVivo()) {
            return;
        }
        interrumpirHilo(efecto.getNombre());
        EstadoEfecto activo = efecto.copia();
        efectos.put(activo.getNombre(), activo);
        if (activo.esDanio() && estado == EstadoPersonaje.VIVO) {
            estado = EstadoPersonaje.ENVENENADO;
        }
        // Un Thread nuevo por efecto (start() solo se puede llamar una vez por Thread). Es daemon para que un
        // efecto a medias nunca impida cerrar el juego.
        Thread hilo = new Thread(new EfectoContinuoTask(this, activo, intervaloMs),
                "efecto-" + activo.getNombre() + "-" + nombre);
        hilo.setDaemon(true);
        hilosEfectos.put(activo.getNombre(), hilo);
        hilo.start();
    }

    /** Quita un efecto y cancela su hilo (antidoto, purificacion). */
    public synchronized boolean quitarEfecto(String nombreEfecto) {
        interrumpirHilo(nombreEfecto);
        if (efectos.remove(nombreEfecto) == null) {
            return false;
        }
        normalizarEstado();
        return true;
    }

    public synchronized void quitarEfectosDanio() {
        for (String nombreEfecto : new ArrayList<>(efectos.keySet())) {
            if (efectos.get(nombreEfecto).esDanio()) {
                quitarEfecto(nombreEfecto);
            }
        }
    }

    /**
     * Cancela todos los efectos con interrupt() y espera a que sus hilos terminen (fin de combate).
     * No es synchronized a proposito: los hilos necesitan el candado del personaje para limpiarse, y si esperaramos
     * con el candado tomado nos quedariamos bloqueados los dos (interbloqueo).
     */
    public void cancelarEfectos() {
        List<Thread> hilos;
        synchronized (this) {
            hilos = new ArrayList<>(hilosEfectos.values());
            interrumpirTodosLosEfectos();
        }
        for (Thread hilo : hilos) {
            try {
                hilo.join(ESPERA_CANCELACION_MS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
        }
    }

    /**
     * Un tick del efecto, de forma atomica: comprueba que sigue activo, aplica dano o curacion y gasta un tick.
     * @return mensaje para el registro, o null si el efecto ya no esta activo (o el personaje murio)
     */
    public synchronized String aplicarTickEfecto(EstadoEfecto efecto) {
        if (efectos.get(efecto.getNombre()) != efecto || !estaVivo()) {
            return null;
        }
        String mensaje;
        if (efecto.esDanio()) {
            recibirDanio(efecto.getValorPorTick());
            mensaje = String.format("%s sufre %d de dano por %s", nombre, efecto.getValorPorTick(), efecto.getNombre());
        } else {
            int curado = curar(efecto.getValorPorTick());
            mensaje = String.format("%s recupera %d de vida por %s", nombre, curado, efecto.getNombre());
        }
        efecto.consumirTurno();
        return mensaje;
    }

    /** Quita del personaje ESTE efecto (si sigue siendo el activo) y arregla el estado. Se puede llamar varias veces. */
    public synchronized void limpiarEfecto(EstadoEfecto efecto) {
        if (efectos.get(efecto.getNombre()) == efecto) {
            efectos.remove(efecto.getNombre());
            normalizarEstado();
        }
        hilosEfectos.remove(efecto.getNombre(), Thread.currentThread());
    }

    /** Los mensajes de los efectos salen por aqui (en el juego, hacia el RegistroCombate). */
    public void setEscuchaEfectos(Consumer<String> escucha) {
        this.escuchaEfectos = escucha;
    }

    public boolean tieneEscuchaEfectos() {
        return escuchaEfectos != null;
    }

    public void notificarEfecto(String mensaje) {
        Consumer<String> escucha = escuchaEfectos;
        if (escucha != null) {
            escucha.accept(mensaje);
        } else {
            System.out.println(mensaje);
        }
    }

    private void interrumpirHilo(String nombreEfecto) {
        Thread hilo = hilosEfectos.remove(nombreEfecto);
        if (hilo != null) {
            hilo.interrupt();
        }
    }

    private void interrumpirTodosLosEfectos() {
        for (Thread hilo : hilosEfectos.values()) {
            hilo.interrupt();
        }
        hilosEfectos.clear();
    }

    private boolean tieneEfectoDanio() {
        for (EstadoEfecto efecto : efectos.values()) {
            if (efecto.esDanio()) {
                return true;
            }
        }
        return false;
    }

    /** Si ya no queda ningun DoT, un personaje ENVENENADO vuelve a estar VIVO. */
    private void normalizarEstado() {
        if (estado == EstadoPersonaje.ENVENENADO && !tieneEfectoDanio()) {
            estado = EstadoPersonaje.VIVO;
        }
    }

    // ---- aturdimiento

    public synchronized void aturdir() {
        if (estaVivo()) {
            estado = EstadoPersonaje.ATURDIDO;
        }
    }

    /** @return true si estaba aturdido (y por tanto pierde este turno) */
    public synchronized boolean consumirAturdimiento() {
        boolean estabaAturdido = estado == EstadoPersonaje.ATURDIDO;
        if (estabaAturdido) {
            estado = tieneEfectoDanio() ? EstadoPersonaje.ENVENENADO : EstadoPersonaje.VIVO;
        }
        return estabaAturdido;
    }

    // ---- inventario, arma, gremio, habilidades

    public String usarPocion(Pocion pocion) {
        int curado = curar(pocion.getCuracionReal());
        if (pocion.getEfecto() != null) {
            aplicarEfecto(pocion.getEfecto());
        }
        if (pocion.isPurifica()) {
            quitarEfectosDanio();
        }
        return String.format("%s usa %s (+%d de vida)", nombre, pocion.getNombre(), curado);
    }

    public void equiparArma(Arma nuevaArma) {
        this.arma = nuevaArma;
    }

    public void desequiparArma() {
        this.arma = null;
    }

    public void unirseAGremio(Gremio nuevoGremio) {
        if (nuevoGremio == null || nuevoGremio == gremio) {
            return;
        }
        if (gremio != null) {
            gremio.removerMiembro(this);
        }
        gremio = nuevoGremio;
        nuevoGremio.agregarMiembro(this);
    }

    public void abandonarGremio() {
        Gremio anterior = gremio;
        if (anterior == null) {
            return;
        }
        gremio = null;
        anterior.removerMiembro(this);
    }

    /** @return false si la habilidad ya estaba aprendida (el Set rechaza duplicados) */
    public synchronized boolean aprenderHabilidad(String habilidad) {
        return habilidades.add(habilidad);
    }

    // ---- getters y setters validados

    public synchronized void setVida(int nuevaVida) {
        vida = Math.max(0, Math.min(nuevaVida, vidaMaxima));
        if (vida == 0) {
            estado = EstadoPersonaje.MUERTO;
        }
    }

    public void setNombre(String nombre) {
        validarNombre(nombre);
        this.nombre = nombre;
    }

    public synchronized void setVidaMaxima(int vidaMaxima) {
        if (vidaMaxima <= 0) {
            throw new IllegalArgumentException("La vida maxima debe ser positiva");
        }
        this.vidaMaxima = vidaMaxima;
        this.vida = Math.min(vida, vidaMaxima);
    }

    public int getDefensa() {
        return defensaBase + tipo.getBonificadorDefensa();
    }

    public String getNombre() {
        return nombre;
    }

    public TipoPersonaje getTipo() {
        return tipo;
    }

    public synchronized int getVida() {
        return vida;
    }

    public synchronized int getVidaMaxima() {
        return vidaMaxima;
    }

    public synchronized EstadoPersonaje getEstado() {
        return estado;
    }

    public Arma getArma() {
        return arma;
    }

    public Gremio getGremio() {
        return gremio;
    }

    public Inventario<Pocion> getInventario() {
        return inventario;
    }

    public synchronized Set<String> getHabilidades() {
        return Collections.unmodifiableSet(new HashSet<>(habilidades));
    }

    /** Copia de los efectos activos: los hilos de los efectos pueden quitarlos mientras alguien recorre la coleccion. */
    public synchronized Collection<EstadoEfecto> getEfectos() {
        return List.copyOf(efectos.values());
    }

    // ---- serializacion

    /** synchronized: mientras se escribe el personaje, ningun hilo de efecto puede cambiar su vida, estado o efectos. */
    @Serial
    private synchronized void writeObject(ObjectOutputStream salida) throws IOException {
        salida.defaultWriteObject();
    }

    /** Los campos transient llegan vacios: se rearma lo que hace falta y se limpia lo que dependia de hilos. */
    @Serial
    private void readObject(ObjectInputStream entrada) throws IOException, ClassNotFoundException {
        entrada.defaultReadObject();
        hilosEfectos = new HashMap<>();
        // Un efecto guardado sin su hilo quedaria congelado para siempre: los efectos pertenecen al combate en curso.
        efectos.clear();
        if (estado == EstadoPersonaje.ENVENENADO) {
            estado = EstadoPersonaje.VIVO;
        }
    }
}
