// Es el unico punto de entrada al SOS
// Si se ehecuta sin argumentos, se abre la terminal tipo linux
//si le pasas un comando directo lo ejecuta y termina.

package sos;

import sos.cli.ShellLinux;

public class Main {
    public static void main(String[] args) {
        ShellLinux shell = new ShellLinux();

        if (args.length > 0) {
            String comandoCompleto = String.join(" ", args);
            shell.procesarComando(comandoCompleto);
        } else {
            shell.iniciar();
        }
    }
}
