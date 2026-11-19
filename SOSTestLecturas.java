import java.util.Arrays;

public class SOSTestLecturas {

    static final int TAMANO_BLOQUE = 1024;
    static final int TOTAL_BLOQUES_ARCHIVO = 3;
    static final int TAMANO_ARCHIVO =
            TAMANO_BLOQUE * TOTAL_BLOQUES_ARCHIVO;

    // Bloques fisicos correspondientes a los bloques logicos 0, 1 y 2
    static final int[] BLOQUES_FISICOS = {4, 9, 12};

    static final byte[] ARCHIVO = new byte[TAMANO_ARCHIVO];

    // Cursor del archivo
    static int cursor = 0;

    public static void main(String[] args) {

        inicializarArchivo();

        System.out.println("==================================");
        System.out.println(" SOS - PRUEBAS DE LECTURA");
        System.out.println("==================================");
        System.out.println("Tamano del archivo: "
                + TAMANO_ARCHIVO + " bytes");
        System.out.println("Tamano de bloque: "
                + TAMANO_BLOQUE + " bytes");
        System.out.println("Mapa fisico: 0->4, 1->9, 2->12");

        pruebaSecuencial();
        pruebaDirecta();
        pruebaLimites();
    }

    static void inicializarArchivo() {
        Arrays.fill(ARCHIVO, (byte) 'A');

        Arrays.fill(
                ARCHIVO,
                TAMANO_BLOQUE,
                TAMANO_BLOQUE * 2,
                (byte) 'B'
        );

        Arrays.fill(
                ARCHIVO,
                TAMANO_BLOQUE * 2,
                TAMANO_ARCHIVO,
                (byte) 'C'
        );
    }

    static void pruebaSecuencial() {
        System.out.println("\n--- LECTURA SECUENCIAL ---");

        cursor = 1000;
        int cantidad = 50;

        System.out.println("Cursor inicial: " + cursor);

        byte[] datos = leerSecuencial(cantidad);

        System.out.println("Bytes leidos: " + datos.length);
        System.out.println("Contenido: " + new String(datos));
        System.out.println("Cursor final: " + cursor);
    }

    static byte[] leerSecuencial(int cantidad) {
        validarLectura(cursor, cantidad);

        int inicio = cursor;
        byte[] resultado = new byte[cantidad];
        int bytesCopiados = 0;

        while (bytesCopiados < cantidad) {
            int offsetActual = cursor;
            int bloqueLogico = offsetActual / TAMANO_BLOQUE;
            int desplazamiento = offsetActual % TAMANO_BLOQUE;

            int disponibles =
                    TAMANO_BLOQUE - desplazamiento;

            int restantes = cantidad - bytesCopiados;
            int porLeer = Math.min(disponibles, restantes);

            int bloqueFisico = BLOQUES_FISICOS[bloqueLogico];

            System.out.println(
                    "Bloque logico " + bloqueLogico
                    + " -> fisico " + bloqueFisico
                    + ", desplazamiento " + desplazamiento
                    + ", bytes " + porLeer
            );

            System.arraycopy(
                    ARCHIVO, cursor,
                    resultado, bytesCopiados,
                    porLeer
            );

            cursor += porLeer;
            bytesCopiados += porLeer;
        }

        System.out.println("Rango logico leido: "
                + inicio + " a " + (cursor - 1));

        return resultado;
    }

    static void pruebaDirecta() {
        System.out.println("\n--- LECTURA DIRECTA ---");

        int offset = 2050;
        int cantidad = 20;

        System.out.println("Offset solicitado: " + offset);
        System.out.println("Cursor anterior: " + cursor);

        byte[] datos = leerDirecto(offset, cantidad);

        System.out.println("Bytes leidos: " + datos.length);
        System.out.println("Contenido: " + new String(datos));
        System.out.println("Cursor final: " + cursor);
    }

    static byte[] leerDirecto(int offset, int cantidad) {
        validarLectura(offset, cantidad);

        // La lectura directa posiciona el cursor en el offset indicado.
        cursor = offset;

        return leerSecuencial(cantidad);
    }

    static void validarLectura(int offset, int cantidad) {
        if (offset < 0) {
            throw new IllegalArgumentException(
                    "Error: el offset no puede ser negativo."
            );
        }

        if (cantidad < 0) {
            throw new IllegalArgumentException(
                    "Error: la cantidad no puede ser negativa."
            );
        }

        if (offset > TAMANO_ARCHIVO
                || cantidad > TAMANO_ARCHIVO - offset) {
            throw new IllegalArgumentException(
                    "Error: lectura fuera de los limites del archivo."
            );
        }
    }

    static void pruebaLimites() {
        System.out.println("\n--- VALIDACION DE LIMITES ---");

        probarLimite(-1, 10);
        probarLimite(TAMANO_ARCHIVO - 5, 10);
        probarLimite(TAMANO_ARCHIVO, 0);
    }

    static void probarLimite(int offset, int cantidad) {
        try {
            validarLectura(offset, cantidad);
            System.out.println(
                    "Offset " + offset + ", cantidad " + cantidad
                    + ": lectura valida."
            );
        } catch (IllegalArgumentException e) {
            System.out.println(
                    "Offset " + offset + ", cantidad " + cantidad
                    + ": " + e.getMessage()
            );
        }
    }
}

