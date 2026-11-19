// Es el motor de planificación de la CPU. Se encarga de tomar lista de los procesos, avanzar el reloj 
// y administra las colas de procesos. También es el que ejectura los algoritmos obligatorios: FCFS, RoundRobin, SJF y Prioridades
// además usa el algoritmo extra MLFQ.
// Construye el diagrama de Gantt.

package sos.cpu;

import sos.core.EstadoProceso;
import sos.core.PCB;
import sos.core.Reloj;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

public class PlanificadorCPU {

    public static class ResultadoPlanificacion {
        public String algoritmo;
        public List<PCB> listaTerminados;
        public String textoGantt;
        public double promEspera;
        public double promRetorno;
        public double promRespuesta;
        public double porcentajeUsoCpu;
        public int tTotal;
    }

    public ResultadoPlanificacion ejecutar(List<PCB> cargaOriginal, String algoritmo, int quantum) {
        Reloj.reiniciar();

        List<PCB> listaProcesos = new ArrayList<>();
        for (int i = 0; i < cargaOriginal.size(); i++) {
            listaProcesos.add(new PCB(cargaOriginal.get(i)));
        }

        Queue<PCB> colaListos = new LinkedList<>();
        Queue<PCB> colaMlfq0 = new LinkedList<>();
        Queue<PCB> colaMlfq1 = new LinkedList<>();
        Queue<PCB> colaMlfq2 = new LinkedList<>();

        List<PCB> listaTerminados = new ArrayList<>();
        PCB enCPU = null;
        int tUsadoQuantum = 0;
        int tCpuOcupado = 0;

        String textoGantt = "";
        String ultimoEnGantt = "";
        String algo = algoritmo.toLowerCase();

        while (listaTerminados.size() < listaProcesos.size()) {
            int tActual = Reloj.getTiempo();

            for (int i = 0; i < listaProcesos.size(); i++) {
                PCB p = listaProcesos.get(i);
                if (p.estado == EstadoProceso.NUEVO && p.tiempoLlegada == tActual) {
                    p.estado = EstadoProceso.LISTO;
                    if (algo.equals("mlfq")) {
                        colaMlfq0.add(p);
                    } else {
                        colaListos.add(p);
                    }
                }
            }

            if (algo.equals("prioridades") && enCPU != null && !colaListos.isEmpty()) {
                PCB masPrioritario = buscarMenorPrioridad(colaListos);
                if (masPrioritario.prioridad < enCPU.prioridad) {
                    enCPU.estado = EstadoProceso.LISTO;
                    colaListos.add(enCPU);
                    enCPU = null;
                    tUsadoQuantum = 0;
                }
            }

            if (enCPU == null) {
                if (algo.equals("fcfs") || algo.equals("rr")) {
                    enCPU = colaListos.poll();
                } else if (algo.equals("sjf")) {
                    if (!colaListos.isEmpty()) {
                        enCPU = buscarMasCorto(colaListos);
                        colaListos.remove(enCPU);
                    }
                } else if (algo.equals("prioridades")) {
                    if (!colaListos.isEmpty()) {
                        enCPU = buscarMenorPrioridad(colaListos);
                        colaListos.remove(enCPU);
                    }
                } else if (algo.equals("mlfq")) {
                    if (!colaMlfq0.isEmpty()) {
                        enCPU = colaMlfq0.poll();
                    } else if (!colaMlfq1.isEmpty()) {
                        enCPU = colaMlfq1.poll();
                    } else if (!colaMlfq2.isEmpty()) {
                        enCPU = colaMlfq2.poll();
                    }
                }

                if (enCPU != null) {
                    enCPU.estado = EstadoProceso.EJECUTANDO;
                    if (enCPU.t1 == -1) {
                        enCPU.t1 = tActual;
                    }
                    tUsadoQuantum = 0;
                }
            }

            String nombreActual;
            if (enCPU != null) {
                nombreActual = enCPU.IdProceso;
            } else {
                nombreActual = "IDLE";
            }

            if (!nombreActual.equals(ultimoEnGantt)) {
                textoGantt = textoGantt + "[" + tActual + "] " + nombreActual + " ";
                ultimoEnGantt = nombreActual;
            }

            for (int i = 0; i < listaProcesos.size(); i++) {
                PCB p = listaProcesos.get(i);
                if (p.estado == EstadoProceso.LISTO) {
                    p.tColaListo++;
                }
            }

            Reloj.avanzar();
            int tSiguiente = Reloj.getTiempo();

            if (enCPU != null) {
                enCPU.tRestante--;
                tUsadoQuantum++;
                tCpuOcupado++;

                if (enCPU.tRestante == 0) {
                    enCPU.estado = EstadoProceso.TERMINADO;
                    enCPU.tFin = tSiguiente;
                    listaTerminados.add(enCPU);
                    enCPU = null;
                    tUsadoQuantum = 0;
                } else if (algo.equals("rr") && tUsadoQuantum >= quantum) {
                    enCPU.estado = EstadoProceso.LISTO;
                    colaListos.add(enCPU);
                    enCPU = null;
                    tUsadoQuantum = 0;
                } else if (algo.equals("mlfq")) {
                    int limiteQ = 9999;
                    if (enCPU.UbiCola == 0) limiteQ = 2;
                    else if (enCPU.UbiCola == 1) limiteQ = 4;

                    if (tUsadoQuantum >= limiteQ) {
                        enCPU.estado = EstadoProceso.LISTO;
                        if (enCPU.UbiCola < 2) {
                            enCPU.UbiCola++;
                        }
                        if (enCPU.UbiCola == 1) colaMlfq1.add(enCPU);
                        else colaMlfq2.add(enCPU);
                        enCPU = null;
                        tUsadoQuantum = 0;
                    }
                }
            }
        }

        textoGantt = textoGantt + "[" + Reloj.getTiempo() + "]";

        ResultadoPlanificacion res = new ResultadoPlanificacion();
        res.algoritmo = algoritmo.toUpperCase();
        res.listaTerminados = listaTerminados;
        res.textoGantt = textoGantt;
        res.tTotal = Reloj.getTiempo();

        double sumaEspera = 0;
        double sumaRetorno = 0;
        double sumaRespuesta = 0;

        for (int i = 0; i < listaTerminados.size(); i++) {
            PCB p = listaTerminados.get(i);
            sumaEspera = sumaEspera + p.tColaListo;
            sumaRetorno = sumaRetorno + p.getTiempoRetorno();
            sumaRespuesta = sumaRespuesta + p.getTiempoRespuesta();
        }

        int total = listaTerminados.size();
        res.promEspera = sumaEspera / total;
        res.promRetorno = sumaRetorno / total;
        res.promRespuesta = sumaRespuesta / total;
        res.porcentajeUsoCpu = ((double) tCpuOcupado / res.tTotal) * 100.0;

        return res;
    }

    private PCB buscarMasCorto(Queue<PCB> cola) {
        PCB mejor = null;
        for (PCB p : cola) {
            if (mejor == null || p.tRestante < mejor.tRestante) {
                mejor = p;
            }
        }
        return mejor;
    }

    private PCB buscarMenorPrioridad(Queue<PCB> cola) {
        PCB mejor = null;
        for (PCB p : cola) {
            if (mejor == null || p.prioridad < mejor.prioridad) {
                mejor = p;
            }
        }
        return mejor;
    }

    public void imprimirReporte(ResultadoPlanificacion res) {
        System.out.println("PLANIFICACION CPU: " + res.algoritmo + " ");
        System.out.println("Gantt: " + res.textoGantt);
        System.out.println("ID | Llegada | CPU | Prio | Inicio | Fin | Espera | Retorno | Respuesta");

        for (int i = 0; i < res.listaTerminados.size(); i++) {
            PCB p = res.listaTerminados.get(i);
            System.out.println(
                p.IdProceso + " | " +
                p.tiempoLlegada + " | " +
                p.tTotalTerminar + " | " +
                p.prioridad + " | " +
                p.t1 + " | " +
                p.tFin + " | " +
                p.tColaListo + " | " +
                p.getTiempoRetorno() + " | " +
                p.getTiempoRespuesta()
            );
        }

        System.out.println("Espera promedio: " + res.promEspera);
        System.out.println("Retorno promedio: " + res.promRetorno);
        System.out.println("Respuesta promedio: " + res.promRespuesta);
        System.out.println("Uso de CPU: " + res.porcentajeUsoCpu);
    }

    public void compararConTeoria() {
        List<PCB> cargaControl = new ArrayList<>();
        cargaControl.add(new PCB("P1", 0, 5, 0, 3, 8192));
        cargaControl.add(new PCB("P2", 0, 3, 0, 1, 8192));
        cargaControl.add(new PCB("P3", 0, 1, 0, 4, 8192));
        cargaControl.add(new PCB("P4", 0, 2, 0, 2, 8192));
        cargaControl.add(new PCB("P5", 0, 4, 0, 2, 8192));

        ResultadoPlanificacion rFcfs = ejecutar(cargaControl, "fcfs", 2);
        ResultadoPlanificacion rSjf  = ejecutar(cargaControl, "sjf", 2);
        ResultadoPlanificacion rRr   = ejecutar(cargaControl, "rr", 2);
        ResultadoPlanificacion rMlfq = ejecutar(cargaControl, "mlfq", 2);

        imprimirReporte(rFcfs);
        imprimirReporte(rSjf);
        imprimirReporte(rRr);
        imprimirReporte(rMlfq);

        System.out.println("");
        System.out.println("COMPARACION CON TEORIA (CASO DE CONTROL)");
        System.out.println("FCFS -> Simulado: " + rFcfs.promEspera + " vs Teorico: 6.6");
        System.out.println("SJF  -> Simulado: " + rSjf.promEspera + " vs Teorico: 4.0");
        System.out.println("RR   -> Simulado: " + rRr.promEspera + " vs Teorico: 7.6");
        System.out.println("MLFQ (Extra) -> Espera: " + rMlfq.promEspera + " vs Retorno: " + rMlfq.promRetorno);
    }
}