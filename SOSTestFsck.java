import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Arrays;

public class SOSTestFsck {

    static final int TAMANO_BLOQUE = 1024;
    static final int TOTAL_BLOQUES = 16;
    static final String DISK_FILE = "disk.img";
    static final String BACKUP_FILE = "disk.img.bak";

    // guardamos en memoria la estructura del disco (16 kib)
    static byte[][] disco = new byte[TOTAL_BLOQUES][TAMANO_BLOQUE];

    public static void main(String[] args) throws Exception {


        System.out.println("   sos - prueba de fsck y recuperacion");
        System.out.println("~.~.~.~.~.~.~.~.~.~.~.~.~.~.~.~.~.~.~.~.~");


        // 1. preparamos el archivo con un texto y sacamos su hash inicial
        System.out.println("\n--- paso 1: creando archivo y calculando checksum ---");
        inicializarDisco();
        String contenidoOriginal = "contenido critico de prueba para sos fsck.";
        escribirArchivoVirt("archivo.txt", contenidoOriginal.getBytes(StandardCharsets.UTF_8));
        
        String checksumOriginal = calcularChecksum(contenidoOriginal.getBytes(StandardCharsets.UTF_8));
        System.out.println("archivo 'archivo.txt' creado correctamente.");
        System.out.println("checksum sha-256 original: " + checksumOriginal);
        guardarEnArchivoFisico(DISK_FILE);

        // 2. sacamos una copia de respaldo por si algo sale mal
        System.out.println("\n--- paso 2: haciendo el respaldo del disco ---");
        copiarArchivoFisico(DISK_FILE, BACKUP_FILE);
        System.out.println("respaldo guardado en '" + BACKUP_FILE + "'.");

        // 3. rompecabezas controlado: alteramos la firma del superbloque
        System.out.println("\n--- paso 3: provocando corrupcion controlada ---");
        corromperSuperbloque();
        System.out.println("listo, cambiamos la firma 'SOS_FS' a 'BAD_FS' en el bloque 0.");
        guardarEnArchivoFisico(DISK_FILE);

        // 4. corremos la revision del sistema de archivos
        System.out.println("\n--- paso 4: ejecutando fsck ---");
        boolean estadoFsck = ejecutarFsck();

        // 5. si fallo, restauramos desde el respaldo y checamos si los datos estan intactos
        System.out.println("\n--- paso 5: restaurando y verificando despues de reiniciar ---");
        if (!estadoFsck) {
            System.out.println("[fsck] el disco esta dañado. jalando la copia desde '" + BACKUP_FILE + "'...");
            copiarArchivoFisico(BACKUP_FILE, DISK_FILE);
            cargarDesdeArchivoFisico(DISK_FILE);
            System.out.println("sistema de archivos restaurado bien.");
        }

        byte[] datosRecuperados = leerArchivoVirt("archivo.txt");
        String checksumRecuperado = calcularChecksum(datosRecuperados);

        System.out.println("texto recuperado: " + new String(datosRecuperados, StandardCharsets.UTF_8));
        System.out.println("checksum sha-256 recuperado: " + checksumRecuperado);

        // comprobamos que el archivo sea exactamente igual al original
        if (checksumOriginal.equals(checksumRecuperado)) {
            System.out.println("\n[exito] todo bien: el checksum coincide byte por byte.");
        } else {
            System.out.println("\n[error] las sumas no coinciden, algo salio mal.");
        }
    }

    // llena de ceros el disco y le pone la firma al superbloque
    static void inicializarDisco() {
        for (int i = 0; i < TOTAL_BLOQUES; i++) {
            Arrays.fill(disco[i], (byte) 0);
        }
        // le metemos la firma mágica al inicio del bloque 0
        byte[] firma = "SOS_FS".getBytes(StandardCharsets.UTF_8);
        System.arraycopy(firma, 0, disco[0], 0, firma.length);
    }

    // guarda el nombre en el bloque 1 y los datos reales en el bloque 2
    static void escribirArchivoVirt(String nombre, byte[] datos) {
        byte[] nombreBytes = nombre.getBytes(StandardCharsets.UTF_8);
        System.arraycopy(nombreBytes, 0, disco[1], 0, nombreBytes.length);
        System.arraycopy(datos, 0, disco[2], 0, datos.length);
    }

    // lee los bytes del bloque 2 hasta topadores con un byte nulo
    static byte[] leerArchivoVirt(String nombre) {
        int longitud = 0;
        while (longitud < TAMANO_BLOQUE && disco[2][longitud] != 0) {
            longitud++;
        }
        byte[] datos = new byte[longitud];
        System.arraycopy(disco[2], 0, datos, 0, longitud);
        return datos;
    }

    // cambia la firma del bloque 0 para simular que se corrompio la cabecera
    static void corromperSuperbloque() {
        byte[] firmaMal = "BAD_FS".getBytes(StandardCharsets.UTF_8);
        System.arraycopy(firmaMal, 0, disco[0], 0, firmaMal.length);
    }

    // revisa la firma en el superbloque para ver si todo esta en orden
    static boolean ejecutarFsck() {
        System.out.println("[fsck] escaneando la estructura de disk.img...");
        byte[] firmaActual = new byte[6];
        System.arraycopy(disco[0], 0, firmaActual, 0, 6);
        String firmaStr = new String(firmaActual, StandardCharsets.UTF_8);

        System.out.println("[fsck] checando el superbloque... se encontro la firma: '" + firmaStr + "'");

        if (!"SOS_FS".equals(firmaStr)) {
            System.out.println("[fsck error] la firma del superbloque no coincide, esta corrupta.");
            System.out.println("[fsck] modo conservador: no tocamos nada para no borrar datos, restauraremos el respaldo.");
            return false;
        }

        System.out.println("[fsck] el disco se ve consistente.");
        return true;
    }

    // saca la firma sha-256 en formato hexadecimal
    static String calcularChecksum(byte[] datos) throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        byte[] hash = md.digest(datos);
        StringBuilder sb = new StringBuilder();
        for (byte b : hash) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    // escribe el array del disco a un archivo fisico .img
    static void guardarEnArchivoFisico(String ruta) throws IOException {
        try (FileOutputStream fos = new FileOutputStream(ruta)) {
            for (int i = 0; i < TOTAL_BLOQUES; i++) {
                fos.write(disco[i]);
            }
        }
    }

    // lee un archivo .img y lo carga a la matriz del disco
    static void cargarDesdeArchivoFisico(String ruta) throws IOException {
        try (FileInputStream fis = new FileInputStream(ruta)) {
            for (int i = 0; i < TOTAL_BLOQUES; i++) {
                fis.read(disco[i]);
            }
        }
    }

    // hace una copia de un archivo a otro en disco duro
    static void copiarArchivoFisico(String origen, String destino) throws IOException {
        try (InputStream in = new FileInputStream(origen);
             OutputStream out = new FileOutputStream(destino)) {
            byte[] buffer = new byte[1024];
            int length;
            while ((length = in.read(buffer)) > 0) {
                out.write(buffer, 0, length);
            }
        }
    }
}