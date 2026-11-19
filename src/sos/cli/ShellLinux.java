// Consola interactiva al estilo de la bash de inux 
// lee los comandos como help, whoami, pwd, cd, su, clear
package sos.cli;

import sos.cpu.PlanificadorCPU;
import java.util.Scanner;

public class ShellLinux {
    private static final String VERDE = "\u001B[32m";
    private static final String AZUL = "\u001B[34m";
    private static final String ROJO = "\u001B[31m";
    private static final String RESET = "\u001B[0m";

    private String usuarioActual = "regina";
    private String rutaActual = "/";
    private PlanificadorCPU planificador = new PlanificadorCPU();

    public void iniciar() {
        Scanner teclado = new Scanner(System.in);
        System.out.println(VERDE + "SOS Kernel 6.0.0-itesca (tty1) - Linux-Inspired Educational OS" + RESET);
        System.out.println("Escribe 'help' para ver los comandos o 'exit' para salir.");
        System.out.println("");

        while (true) {
            String simbolo = usuarioActual.equals("docente") ? "#" : "$";
            System.out.print(VERDE + usuarioActual + "@sos" + RESET + ":" + AZUL + rutaActual + RESET + simbolo + " ");

            if (!teclado.hasNextLine()) break;
            String linea = teclado.nextLine().trim();
            if (linea.isEmpty()) continue;

            if (linea.equals("exit") || linea.equals("shutdown")) {
                System.out.println("Sincronizando disk.img... Sistema detenido.");
                break;
            }

            procesarComando(linea);
        }
    }

    public void procesarComando(String linea) {
        String[] partes = linea.split("\\s+");
        String comando = partes[0];

        if (comando.equals("help")) {
            System.out.println("Comandos estilo Linux disponibles:");
            System.out.println("  sim plan-demo  -> Ejecuta y compara FCFS, SJF, RR y MLFQ vs Teoria");
            System.out.println("  whoami         -> Muestra el usuario actual");
            System.out.println("  pwd            -> Muestra la ruta actual");
            System.out.println("  cd <ruta>      -> Cambia de directorio");
            System.out.println("  su <usuario>   -> Cambia de usuario");
            System.out.println("  clear          -> Limpia la pantalla");
            System.out.println("  exit           -> Sale del simulador");
        } else if (comando.equals("whoami")) {
            System.out.println(usuarioActual);
        } else if (comando.equals("pwd")) {
            System.out.println(rutaActual);
        } else if (comando.equals("clear")) {
            System.out.print("\033[H\033[2J");
            System.out.flush();
        } else if (comando.equals("su")) {
            if (partes.length >= 2) {
                usuarioActual = partes[1];
                System.out.println("Sesion cambiada a: " + usuarioActual);
            } else {
                System.out.println("Uso: su <usuario>");
            }
        } else if (comando.equals("cd")) {
            if (partes.length < 2 || partes[1].equals("/")) {
                rutaActual = "/";
            } else if (partes[1].equals("..")) {
                rutaActual = "/";
            } else if (rutaActual.equals("/")) {
                rutaActual = "/" + partes[1];
            } else {
                rutaActual = rutaActual + "/" + partes[1];
            }
        } else if (comando.equals("sim")) {
            if (partes.length >= 2 && partes[1].equals("plan-demo")) {
                planificador.compararConTeoria();
            } else {
                System.out.println("Uso: sim plan-demo");
            }
        } else {
            System.out.println(ROJO + "sos: " + comando + ": orden no encontrada" + RESET);
        }
    }
}
