// Reloj del sistema operativo
// TODOS LOS MÓDULOS DEBEN DE LEER LA MISMA REFERENCIA DE TIEMPO PARA QUE LAS TRAZAS DE CPU, 
// MEMORIA Y DISCO SEAN COHERENTES. Debe de funcionar por pasos discretos o ticks sin usar Thread.sleep().

package sos.core;
public class Reloj {
    private static int tiempo = 0;

    public static int getTiempo() {
        return tiempo;
    }

    public static void avanzar() {
        tiempo++;
    }
    public static void reiniciar() {
        tiempo = 0;
    }
}