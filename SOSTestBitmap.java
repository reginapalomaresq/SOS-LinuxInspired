import java.io.*;
import java.util.*;

public class SOSTestBitmap {

    static final int TAMANO_BLOQUE = 1024; // 1 KiB por bloque
    static final int TOTAL_BLOQUES = 16;   // Disco de 16 bloques (16 KiB)
    static final String DISK_FILE = "disk.img";

    // Estructuras del Sistema de Archivos
    static boolean[] bitmap = new boolean[TOTAL_BLOQUES];
    static Map<String, List<Integer>> tablaArchivos = new HashMap<>();

    public static void main(String[] args) {

        System.out.println("==================================================");
        System.out.println("   SOS - PRUEBAS DE BITMAP Y PERSISTENCIA (df)");
        System.out.println("==================================================");

        // 1. Inicialización y estado inicial del disco
        inicializarDiscoNuevo();
        ejecutarFsDf("Estado Inicial");

        // 2. Creación de archivos de prueba
        System.out.println("\n--- 1. CREACIÓN DE ARCHIVOS ---");
        crearArchivo("doc1.txt", 1500); // Requiere 2 bloques
        crearArchivo("doc2.txt", 3000); // Requiere 3 bloques
        ejecutarFsDf("Después de crear doc1.txt y doc2.txt");

        // 3. Prueba de Falta de Espacio (Evitar reserva parcial)
        System.out.println("\n--- 2. PRUEBA DE FALTA DE ESPACIO ---");
        // Quedan 11 bloques libres. Intentamos pedir 15 bloques.
        crearArchivo("grande.bin", 15 * TAMANO_BLOQUE); 
        ejecutarFsDf("Tras intentar crear archivo demasiado grande");

        // 4. Prueba de Borrado de Archivos
        System.out.println("\n--- 3. BORRADO DE ARCHIVO ---");
        borrarArchivo("doc1.txt");
        ejecutarFsDf("Tras borrar doc1.txt (Libera 2 bloques)");

        // 5. Prueba de Persistencia (Guardar y Reiniciar)
        System.out.println("\n--- 4. PERSISTENCIA Y REINICIO ---");
        guardarEstadoEnDisco();
        reiniciarSistema();
        ejecutarFsDf("Estado Recuperado tras Reiniciar SOS");

        // 6. Cálculos de Fragmentación Interna
        System.out.println("\n--- 5. CÁLCULOS DE FRAGMENTACIÓN INTERNA ---");
        calcularFragmentacion(500);   // Tamaño 1: 500 bytes
        calcularFragmentacion(1500);  // Tamaño 2: 1500 bytes
        calcularFragmentacion(3072);  // Tamaño 3: 3072 bytes
    }

    // Inicializa el sistema desde cero
    static void inicializarDiscoNuevo() {
        Arrays.fill(bitmap, false);
        tablaArchivos.clear();
        // Bloque 0 reservado para el Superbloque/Sistema
        bitmap[0] = true;
        tablaArchivos.put("SYSTEM_SUPERBLOCK", Arrays.asList(0));
    }

    // Algoritmo de creación de archivos con verificación de espacio completo
    static boolean crearArchivo(String nombre, int tamanoBytes) {
        int bloquesNecesarios = (int) Math.ceil((double) tamanoBytes / TAMANO_BLOQUE);
        if (tamanoBytes == 0) bloquesNecesarios = 1;

        System.out.println("\nSolicitando crear '" + nombre + "' (" + tamanoBytes 
                + " bytes -> " + bloquesNecesarios + " bloques necesarios)");

        // Contar bloques libres
        List<Integer> bloquesLibres = new ArrayList<>();
        for (int i = 0; i < TOTAL_BLOQUES; i++) {
            if (!bitmap[i]) {
                bloquesLibres.add(i);
            }
        }

        // Criterio: La falta de espacio no debe dejar reservas parciales
        if (bloquesLibres.size() < bloquesNecesarios) {
            System.out.println("[ERROR] Espacio insuficiente. Libres: " 
                    + bloquesLibres.size() + ", Requeridos: " + bloquesNecesarios);
            System.out.println("[CANCELADO] No se reservó ningún bloque.");
            return false;
        }

        // Asignación Atómica
        List<Integer> bloquesAsignados = new ArrayList<>();
        for (int i = 0; i < bloquesNecesarios; i++) {
            int bloque = bloquesLibres.get(i);
            bitmap[bloque] = true; // Reserva en el bitmap
            bloquesAsignados.add(bloque);
        }

        tablaArchivos.put(nombre, bloquesAsignados);
        System.out.println("[ÉXITO] Archivo '" + nombre + "' creado en bloques: " + bloquesAsignados);
        return true;
    }

    // Liberación de bloques
    static boolean borrarArchivo(String nombre) {
        if (!tablaArchivos.containsKey(nombre)) {
            System.out.println("[ERROR] El archivo '" + nombre + "' no existe.");
            return false;
        }

        List<Integer> bloques = tablaArchivos.get(nombre);
        for (int bloque : bloques) {
            bitmap[bloque] = false; // Libera en el bitmap
        }
        tablaArchivos.remove(nombre);

        System.out.println("[ÉXITO] Archivo '" + nombre + "' borrado. Bloques liberados: " + bloques);
        return true;
    }

    // Comando 'fs df'
    static void ejecutarFsDf(String etiqueta) {
        int bloquesOcupados = 0;
        StringBuilder sbBitmap = new StringBuilder();

        for (int i = 0; i < TOTAL_BLOQUES; i++) {
            if (bitmap[i]) {
                bloquesOcupados++;
                sbBitmap.append("[O]");
            } else {
                sbBitmap.append("[L]");
            }
        }

        int bloquesLibres = TOTAL_BLOQUES - bloquesOcupados;
        int espacioTotal = TOTAL_BLOQUES * TAMANO_BLOQUE;
        int espacioUsado = bloquesOcupados * TAMANO_BLOQUE;
        int espacioLibre = bloquesLibres * TAMANO_BLOQUE;
        double porcentajeUso = ((double) bloquesOcupados / TOTAL_BLOQUES) * 100;

        System.out.println("\n>>> Command: fs df (" + etiqueta + ")");
        System.out.println("Bitmap (O=Ocupado, L=Libre): " + sbBitmap.toString());
        System.out.printf("Bloques totales: %d | Ocupados: %d | Libres: %d\n", 
                TOTAL_BLOQUES, bloquesOcupados, bloquesLibres);
        System.out.printf("Capacidad total: %d B | Usado: %d B | Libre: %d B (%.1f%% uso)\n", 
                espacioTotal, espacioUsado, espacioLibre, porcentajeUso);
    }

    // Guardar estado del bitmap y tabla de archivos en archivo físico (Persistencia)
    static void guardarEstadoEnDisco() {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(DISK_FILE))) {
            oos.writeObject(bitmap);
            oos.writeObject(tablaArchivos);
            System.out.println("[DISCO] Estado del sistema de archivos guardado en '" + DISK_FILE + "'.");
        } catch (IOException e) {
            System.out.println("[ERROR] No se pudo guardar el estado en disco: " + e.getMessage());
        }
    }

    // Simula el reinicio del sistema limpiando la memoria y recargando desde disk.img
    static void reiniciarSistema() {
        System.out.println("\n--- REINICIANDO SIMULADOR SOS ---");
        // Limpiamos la memoria RAM simulada
        Arrays.fill(bitmap, false);
        tablaArchivos.clear();

        // Cargar desde disco
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(DISK_FILE))) {
            bitmap = (boolean[]) ois.readObject();
            @SuppressWarnings("unchecked")
            Map<String, List<Integer>> tablaCargada = (Map<String, List<Integer>>) ois.readObject();
            tablaArchivos = tablaCargada;
            System.out.println("[DISCO] Estado cargado exitosamente desde '" + DISK_FILE + "'.");
        } catch (Exception e) {
            System.out.println("[ERROR] Fallo al recuperar estado de disco: " + e.getMessage());
        }
    }

    // Cálculo y reporte de fragmentación interna
    static void calcularFragmentacion(int tamanoBytes) {
        int bloquesNecesarios = (int) Math.ceil((double) tamanoBytes / TAMANO_BLOQUE);
        if (tamanoBytes == 0) bloquesNecesarios = 1;

        int espacioAsignado = bloquesNecesarios * TAMANO_BLOQUE;
        int fragmentacionInterna = espacioAsignado - tamanoBytes;
        double porcentajePerdido = ((double) fragmentacionInterna / espacioAsignado) * 100;

        System.out.printf("Tamaño: %4d B | Bloques: %d | Espacio Asignado: %4d B | Frag. Interna: %4d B (%5.1f%% desperdiciado)\n",
                tamanoBytes, bloquesNecesarios, espacioAsignado, fragmentacionInterna, porcentajePerdido);
    }
}
