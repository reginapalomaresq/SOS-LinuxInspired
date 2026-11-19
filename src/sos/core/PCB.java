// Es el cloque de control de procesos, guarda la información de cada proceso 
// y calcula el tiempo de ejecución de cada proceso. 

package sos.core;

import java.util.ArrayList;
import java.util.List;

public class PCB {
    public String IdProceso;
    public int prioridad;
    public EstadoProceso estado;
    public int tiempoLlegada;
    public int tTotalTerminar;
    public int tRestante;
    public int tEntradaSalida;
    public int memoNecesaria;
    public List<Integer> memoPags = new ArrayList<>();

    public int t1 = -1;
    public int tFin = -1;
    public int tColaListo = 0;
    public int UbiCola = 0;

    public PCB(String IdProceso, int llegada, int cpu, int io, int prioridad, int memoria) {
        this.IdProceso = IdProceso;
        this.tiempoLlegada = llegada;
        this.tTotalTerminar = cpu;
        this.tRestante = cpu;
        this.tEntradaSalida = io;
        this.prioridad = prioridad;
        this.memoNecesaria = memoria;
        this.estado = EstadoProceso.NUEVO;
    }

    public PCB(PCB otro) {
        this(otro.IdProceso, otro.tiempoLlegada, otro.tTotalTerminar, otro.tEntradaSalida, otro.prioridad, otro.memoNecesaria);
        this.memoPags = new ArrayList<>(otro.memoPags);
    }

    public int getTiempoRetorno() {
        return tFin - tiempoLlegada;
    }

    public int getTiempoRespuesta() {
        return t1 - tiempoLlegada;
    }
}