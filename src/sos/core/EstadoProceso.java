// Representa los 5 estados del ciclo de vida de un proceso 
// Tema visto en la unidad 2

// Nuevo: El proceso acaba de llegar al sistema pero aun no esa en la cola de listos
// Listo: El proceso esta en la cola de listos esperando a ser ejecutado
// Ejecutando: El proceso esta siendo ejecutado por la CPU
// Bloqueado: El proceso esta esperando a que ocurra un evento para poder continuar su ejecucion
// Terminado: El proceso ha terminado su ejecucion y ha sido eliminado del sistema

package sos.core;
public enum EstadoProceso {
    NUEVO, LISTO, EJECUTANDO, BLOQUEADO, TERMINADO;
}   