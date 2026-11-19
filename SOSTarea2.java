
import java.io.RandomAccessFile;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class SOSSimulador {

    private static final String DISK_NAME = "disk.img";
    private static final int BLOCK_SIZE = 1024;
    private static final int DISK_SIZE = BLOCK_SIZE * 1024;

    // Zona reservada para el archivo virtual /docs/a.txt
    private static final int FILE_START = 5 * BLOCK_SIZE;
    private static final int MAX_FILE_SIZE = BLOCK_SIZE;

    // Tabla de archivos abiertos
    private static Map<Integer, ArchivoAbierto> tablaAbiertos =
            new HashMap<>();

    // Representa un archivo abierto
    static class ArchivoAbierto {
        String ruta;
        String modo;
        int cursor;
        int inicio;
        int tamano;

        ArchivoAbierto(String ruta, String modo,
                       int inicio, int tamano) {
            this.ruta = ruta;
            this.modo = modo;
            this.inicio = inicio;
            this.tamano = tamano;
            this.cursor = 0;
        }
    }

    public static void main(String[] args) {
        try {
            inicializarDisco();

            // Preparar el archivo virtual con contenido inicial
            prepararArchivo();

            System.out.println("\n=== TABLA DE ARCHIVOS ABIERTOS ===");

            int descriptor = abrir("/docs/a.txt", "r+");

            mostrarTabla();

            System.out.println("\n=== TRAZA DE OPERACIONES ===");

            // Primera lectura
            String lectura1 = leer(descriptor, 9);
            System.out.println("1. Primera lectura: " + lectura1);
            mostrarTabla();

            // Segunda lectura
            String lectura2 = leer(descriptor, 19);
            System.out.println("2. Segunda lectura: " + lectura2);
            mostrarTabla();

            // Ir al final y escribir
            irAlFinal(descriptor);
            escribir(descriptor, "Fin.\n");
            System.out.println("3. Se escribio 'Fin.' al final.");
            mostrarTabla();

            // Mostrar el contenido antes de cerrar
            System.out.println("\nContenido antes de cerrar:");
            System.out.println(leerArchivoReal());

            // Cerrar el archivo
            cerrar(descriptor);
            System.out.println("\n4. Archivo cerrado.");
            mostrarTabla();

            // Verificar que los datos persisten
            System.out.println("\n=== VERIFICACION EN DISK.IMG ===");
            System.out.println("Contenido guardado:");
            System.out.println(leerArchivoReal());
            System.out.println("Tamano final: "
                    + obtenerTamanoReal() + " bytes.");

        } catch (Exception e) {
            System.out.println("ERROR: " + e.getMessage());
        }
    }

    // Crea el disco si no existe
    public static void inicializarDisco() throws IOException {
        File archivoDisco = new File(DISK_NAME);

        try (RandomAccessFile disk =
                     new RandomAccessFile(archivoDisco, "rw")) {

            if (!archivoDisco.exists() || disk.length() < DISK_SIZE) {
                disk.setLength(DISK_SIZE);
            }
        }

        System.out.println("Disco listo: " + DISK_NAME);
    }

    // Prepara el contenido inicial del archivo virtual
    public static void prepararArchivo() throws IOException {
        byte[] datos = "Hola SOS\nArchivo de prueba.\n"
                .getBytes(StandardCharsets.UTF_8);

        if (datos.length > MAX_FILE_SIZE) {
            throw new IOException("El archivo es demasiado grande.");
        }

        try (RandomAccessFile disk =
                     new RandomAccessFile(DISK_NAME, "rw")) {

            disk.seek(FILE_START);
            disk.write(datos);

            // Limpiar el resto de la zona reservada
            byte[] vacio = new byte[MAX_FILE_SIZE - datos.length];
            disk.write(vacio);
        }

        System.out.println("Archivo virtual preparado: /docs/a.txt");
    }

    // Abre el archivo y devuelve un descriptor
    public static int abrir(String ruta, String modo)
            throws IOException {

        if (!ruta.equals("/docs/a.txt")) {
            throw new IOException("Archivo no encontrado: " + ruta);
        }

        if (!modo.equals("r") && !modo.equals("r+")
                && !modo.equals("w") && !modo.equals("w+")
                && !modo.equals("a") && !modo.equals("a+")) {
            throw new IOException("Modo de apertura no valido.");
        }

        if (modo.equals("w") || modo.equals("w+")) {
            try (RandomAccessFile disk =
                         new RandomAccessFile(DISK_NAME, "rw")) {
                disk.seek(FILE_START);
                disk.write(new byte[MAX_FILE_SIZE]);
            }
        }

        int tamano = obtenerTamanoReal();
        int descriptor = 3;

        while (tablaAbiertos.containsKey(descriptor)) {
            descriptor++;
        }

        ArchivoAbierto archivo = new ArchivoAbierto(
                ruta, modo, FILE_START, tamano);

        if (modo.equals("a") || modo.equals("a+")) {
            archivo.cursor = tamano;
        }

        tablaAbiertos.put(descriptor, archivo);

        System.out.println("Archivo abierto: " + ruta
                + ", descriptor " + descriptor
                + ", modo " + modo);

        return descriptor;
    }

    // Lee una cantidad de bytes desde el cursor actual
    public static String leer(int descriptor, int cantidad)
            throws IOException {

        ArchivoAbierto archivo = buscar(descriptor);

        if (!archivo.modo.equals("r")
                && !archivo.modo.equals("r+")
                && !archivo.modo.equals("w+")
                && !archivo.modo.equals("a+")) {
            throw new IOException(
                    "El archivo no permite lectura.");
        }

        if (cantidad < 0) {
            throw new IOException("Cantidad de lectura no valida.");
        }

        int disponibles = archivo.tamano - archivo.cursor;
        int bytesLeer = Math.min(cantidad, Math.max(0, disponibles));

        byte[] buffer = new byte[bytesLeer];

        try (RandomAccessFile disk =
                     new RandomAccessFile(DISK_NAME, "r")) {
            disk.seek(archivo.inicio + archivo.cursor);
            disk.readFully(buffer);
        }

        archivo.cursor += bytesLeer;

        return new String(buffer, StandardCharsets.UTF_8);
    }

    // Escribe en la posicion actual o al final, segun el modo
    public static void escribir(int descriptor, String texto)
            throws IOException {

        ArchivoAbierto archivo = buscar(descriptor);

        if (archivo.modo.equals("r")) {
            throw new IOException(
                    "No se puede escribir en modo de solo lectura.");
        }

        byte[] datos = texto.getBytes(StandardCharsets.UTF_8);

        if (archivo.modo.equals("a") || archivo.modo.equals("a+")) {
            archivo.cursor = archivo.tamano;
        }

        if (archivo.cursor + datos.length > MAX_FILE_SIZE) {
            throw new IOException("No hay espacio para escribir.");
        }

        try (RandomAccessFile disk =
                     new RandomAccessFile(DISK_NAME, "rw")) {
            disk.seek(archivo.inicio + archivo.cursor);
            disk.write(datos);
        }

        archivo.cursor += datos.length;

        if (archivo.cursor > archivo.tamano) {
            archivo.tamano = archivo.cursor;
        }
    }

    // Coloca el cursor al final del archivo
    public static void irAlFinal(int descriptor)
            throws IOException {

        ArchivoAbierto archivo = buscar(descriptor);
        archivo.cursor = archivo.tamano;
    }

    // Cierra el archivo y libera su descriptor
    public static void cerrar(int descriptor) throws IOException {
        if (!tablaAbiertos.containsKey(descriptor)) {
            throw new IOException("Descriptor no valido.");
        }

        tablaAbiertos.remove(descriptor);
        System.out.println("Descriptor " + descriptor + " liberado.");
    }

    // Busca una entrada en la tabla
    public static ArchivoAbierto buscar(int descriptor)
            throws IOException {

        ArchivoAbierto archivo = tablaAbiertos.get(descriptor);

        if (archivo == null) {
            throw new IOException("Descriptor no abierto: " + descriptor);
        }

        return archivo;
    }

    // Muestra la tabla de archivos abiertos
    public static void mostrarTabla() {
        System.out.println("\nDescriptor | Ruta          | Modo | Cursor | Tamano");

        if (tablaAbiertos.isEmpty()) {
            System.out.println("(No hay archivos abiertos)");
            return;
        }

        for (Map.Entry<Integer, ArchivoAbierto> entrada
                : tablaAbiertos.entrySet()) {

            ArchivoAbierto archivo = entrada.getValue();

            System.out.println(
                    entrada.getKey() + "          | "
                    + archivo.ruta + " | "
                    + archivo.modo + "   | "
                    + archivo.cursor + "      | "
                    + archivo.tamano);
        }
    }

    // Lee los datos reales del archivo almacenado en el disco
    public static String leerArchivoReal() throws IOException {
        int tamano = obtenerTamanoReal();
        byte[] datos = new byte[tamano];

        try (RandomAccessFile disk =
                     new RandomAccessFile(DISK_NAME, "r")) {
            disk.seek(FILE_START);
            disk.readFully(datos);
        }

        return new String(datos, StandardCharsets.UTF_8);
    }

    // Calcula el tamaño a partir del salto de linea final
    public static int obtenerTamanoReal() throws IOException {
        byte[] datos = new byte[MAX_FILE_SIZE];

        try (RandomAccessFile disk =
                     new RandomAccessFile(DISK_NAME, "r")) {
            disk.seek(FILE_START);
            disk.readFully(datos);
        }

        int ultimo = datos.length - 1;

        while (ultimo >= 0 && datos[ultimo] == 0) {
            ultimo--;
        }

        return ultimo + 1;
    }
}
