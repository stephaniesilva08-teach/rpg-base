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


public abstract class Personaje implements ICombatiente, Serializable {

// TALLER ARCHIVOS
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

    private Arma arma;                  
    private Gremio gremio;              
    private final Inventario<Pocion> inventario = new Inventario<>(CAPACIDAD_INVENTARIO); 
    private final Set<String> habilidades = new HashSet<>();
    private final Map<String, EstadoEfecto> efectos = new LinkedHashMap<>();

    
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

    

    
    public abstract String realizarAtaqueEspecial(Personaje objetivo) throws SinManaException;

    
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
        
        double factorMitigacion = PORCENTAJE_TOTAL / (PORCENTAJE_TOTAL + getDefensa());
        int danioFinal = (int) Math.round(danoRecibido * factorMitigacion);
        recibirDanio(Math.max(DANIO_MINIMO, danioFinal));
    }

    

    
    
    
    
    public synchronized void recibirDanio(int cantidad) {
        if (cantidad < 0) {
            throw new IllegalArgumentException("El danio no puede ser negativo");
        }
        setVida(vida - cantidad);
// TALLER CONCURRENCIA
    }

    
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

    

    public void aplicarEfecto(EstadoEfecto efecto) {
        aplicarEfecto(efecto, 1000);
    }

    
    public synchronized void aplicarEfecto(EstadoEfecto efecto, long intervaloMs) {
        if (!estaVivo()) {
            return;
        }
// TALLER CONCURRENCIA
        interrumpirHilo(efecto.getNombre());
        EstadoEfecto activo = efecto.copia();
        efectos.put(activo.getNombre(), activo);
        if (activo.esDanio() && estado == EstadoPersonaje.VIVO) {
            estado = EstadoPersonaje.ENVENENADO;
        }
        
        
        Thread hilo = new Thread(new EfectoContinuoTask(this, activo, intervaloMs),
                "efecto-" + activo.getNombre() + "-" + nombre);
        hilo.setDaemon(true);
        hilosEfectos.put(activo.getNombre(), hilo);
        hilo.start();
    }

    
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

    
    public synchronized void limpiarEfecto(EstadoEfecto efecto) {
        if (efectos.get(efecto.getNombre()) == efecto) {
            efectos.remove(efecto.getNombre());
            normalizarEstado();
        }
        hilosEfectos.remove(efecto.getNombre(), Thread.currentThread());
    }

    
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

    
    private void normalizarEstado() {
        if (estado == EstadoPersonaje.ENVENENADO && !tieneEfectoDanio()) {
            estado = EstadoPersonaje.VIVO;
        }
    }

    

    public synchronized void aturdir() {
        if (estaVivo()) {
            estado = EstadoPersonaje.ATURDIDO;
        }
    }

    
    public synchronized boolean consumirAturdimiento() {
        boolean estabaAturdido = estado == EstadoPersonaje.ATURDIDO;
        if (estabaAturdido) {
            estado = tieneEfectoDanio() ? EstadoPersonaje.ENVENENADO : EstadoPersonaje.VIVO;
        }
        return estabaAturdido;
    }

    

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

    
    public synchronized boolean aprenderHabilidad(String habilidad) {
        return habilidades.add(habilidad);
    }

    

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

    
    public synchronized Collection<EstadoEfecto> getEfectos() {
        return List.copyOf(efectos.values());
    }

    
    
    
    

    
    @Serial
    private synchronized void writeObject(ObjectOutputStream salida) throws IOException {
        salida.defaultWriteObject();
    }

    
    @Serial
    private void readObject(ObjectInputStream entrada) throws IOException, ClassNotFoundException {
        entrada.defaultReadObject();
        hilosEfectos = new HashMap<>();
        
        efectos.clear();
        if (estado == EstadoPersonaje.ENVENENADO) {
            estado = EstadoPersonaje.VIVO;
        }
    }
}
