import javax.crypto.Cipher;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.security.spec.KeySpec;
import java.util.*;

public class SOSIntegracionSeguridad {

    // nombres de archivos que se guardan en la carpeta
    static final String DISCO = "disk.img";
    static final String RESPALDO = "disk.img.bak";
    static final String REF = "referencia_notas.bin";
    static final String ARCHIVO_LOG = "sos_tarea8.log";

    // variables del sistema
    static DatosDisco disco = new DatosDisco();
    static String usuarioActivo = null;
    static PrintWriter log;

    // clase para guardar todo el disco en un archivo
    static class DatosDisco implements Serializable {
        String firma = "SOS_FS_V6";
        Map<String, String> passwords = new HashMap<>();
        Map<String, String> salts = new HashMap<>();
        Map<String, Archivo> archivos = new HashMap<>();
        Map<String, Map<String, String>> permisos = new HashMap<>();
    }

    // clase que representa cada archivo guardado
    static class Archivo implements Serializable {
        String nombre;
        byte[] datos;
        boolean estaCifrado;

        Archivo(String nombre) {
            this.nombre = nombre;
            this.datos = new byte[0];
            this.estaCifrado = false;
        }
    }

    public static void main(String[] args) {
        try {
            log = new PrintWriter(new FileWriter(ARCHIVO_LOG, false), true);
            cargarDisco();

            if (args.length > 0 && args[0].equals("--demo")) {
                correrDemo();
            } else {
                iniciarConsola();
            }
        } catch (Exception e) {
            imprimir("error: " + e.getMessage());
        } finally {
            if (log != null) log.close();
        }
    }

    // carga el archivo disk.img o crea uno nuevo si no existe
    static void cargarDisco() {
        File f = new File(DISCO);
        if (!f.exists()) {
            disco = new DatosDisco();
            crearUsuario("docente", "prof2026");
            crearUsuario("Ana", "ana123");
            crearUsuario("Bruno", "bruno123");

            disco.archivos.put("notas", new Archivo("notas"));
            disco.permisos.put("notas", new HashMap<>());
            disco.permisos.get("notas").put("docente", "rwx");

            guardarDisco();
            imprimir("[init] se creo " + DISCO + " con usuarios docente, Ana y Bruno.");
        } else {
            try (ObjectInputStream in = new ObjectInputStream(new FileInputStream(f))) {
                disco = (DatosDisco) in.readObject();
                imprimir("[disco] archivo " + DISCO + " cargado bien.");
            } catch (Exception e) {
                imprimir("[error] no se pudo leer el disco: " + e.getMessage());
            }
        }
    }

    // guarda los cambios en disk.img
    static void guardarDisco() {
        try (ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(DISCO))) {
            out.writeObject(disco);
        } catch (IOException e) {
            imprimir("[error] no se pudo guardar en disco: " + e.getMessage());
        }
    }

    // guarda un usuario nuevo con salt y hash sha-256
    static void crearUsuario(String user, String pass) {
        try {
            byte[] salt = new byte[16];
            new SecureRandom().nextBytes(salt);
            disco.salts.put(user, Base64.getEncoder().encodeToString(salt));
            disco.passwords.put(user, sacarHashPass(pass, salt));
        } catch (Exception e) {
            imprimir("error creando usuario");
        }
    }

    // convierte la contraseña a hash para no guardarla en texto normal
    static String sacarHashPass(String pass, byte[] salt) throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        md.update(salt);
        byte[] hash = md.digest(pass.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(hash);
    }

    // inicia sesion revisando usuario y contraseña
    static boolean login(String user, String pass) {
        try {
            if (!disco.passwords.containsKey(user)) {
                imprimir("[auth] no existe el usuario " + user);
                return false;
            }
            byte[] salt = Base64.getDecoder().decode(disco.salts.get(user));
            String hashIntento = sacarHashPass(pass, salt);

            if (hashIntento.equals(disco.passwords.get(user))) {
                usuarioActivo = user;
                imprimir("[auth] sesion iniciada como: " + user);
                return true;
            } else {
                imprimir("[auth] contraseña incorrecta para " + user);
                return false;
            }
        } catch (Exception e) {
            imprimir("[auth] fallo al iniciar sesion");
            return false;
        }
    }

    // asigna permisos en la matriz de acceso
    static void darPermiso(String user, String archivo, String perm) {
        if (!"docente".equals(usuarioActivo)) {
            imprimir("[permisos] denegado: solo el docente puede dar permisos.");
            return;
        }
        if (!disco.archivos.containsKey(archivo)) {
            imprimir("[error] no existe el archivo " + archivo);
            return;
        }
        disco.permisos.get(archivo).put(user, perm);
        guardarDisco();
        imprimir("[permisos] se asigno '" + perm + "' a " + user + " en el archivo " + archivo);
    }

    // checa si el usuario activo tiene permiso r o w
    static boolean tienePermiso(String archivo, char letra) {
        if (usuarioActivo == null) return false;
        Map<String, String> lista = disco.permisos.get(archivo);
        if (lista == null) return false;
        String p = lista.getOrDefault(usuarioActivo, "");
        return p.indexOf(letra) != -1;
    }

    // escribe texto dentro del archivo
    static void escribir(String archivo, String texto) {
        if (!disco.archivos.containsKey(archivo)) {
            imprimir("[fs] no existe el archivo " + archivo);
            return;
        }
        if (!tienePermiso(archivo, 'w')) {
            imprimir("[fs] denegado: " + usuarioActivo + " no tiene permiso de escritura en " + archivo);
            return;
        }
        Archivo a = disco.archivos.get(archivo);
        a.datos = texto.getBytes(StandardCharsets.UTF_8);
        a.estaCifrado = false;
        guardarDisco();
        imprimir("[fs] " + usuarioActivo + " escribio " + a.datos.length + " bytes en " + archivo);
    }

    // lee el contenido del archivo (si esta cifrado muestra puro texto raro en base64)
    static byte[] leer(String archivo) {
        if (!disco.archivos.containsKey(archivo)) {
            imprimir("[fs] no existe el archivo " + archivo);
            return null;
        }
        if (!tienePermiso(archivo, 'r')) {
            imprimir("[fs] denegado: " + usuarioActivo + " no tiene permiso de lectura en " + archivo);
            return null;
        }
        Archivo a = disco.archivos.get(archivo);
        imprimir("[fs] " + usuarioActivo + " leyendo '" + archivo + "' (cifrado=" + a.estaCifrado + "):");
        if (a.estaCifrado) {
            imprimir("   contenido cifrado: " + Base64.getEncoder().encodeToString(a.datos));
        } else {
            imprimir("   contenido: " + new String(a.datos, StandardCharsets.UTF_8));
        }
        return a.datos;
    }

    // genera una llave aes-256 a partir de la clave de texto
    static SecretKeySpec hacerLlave(String clave, byte[] salt) throws Exception {
        SecretKeyFactory f = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
        KeySpec spec = new PBEKeySpec(clave.toCharArray(), salt, 65536, 256);
        byte[] bytesLlave = f.generateSecret(spec).getEncoded();
        return new SecretKeySpec(bytesLlave, "AES");
    }

    // cifra el archivo usando aes-gcm
    static void cifrar(String archivo, String clave) {
        if (!disco.archivos.containsKey(archivo)) {
            imprimir("[seguridad] no existe el archivo.");
            return;
        }
        if (!tienePermiso(archivo, 'w')) {
            imprimir("[seguridad] denegado: ocupas permiso de escritura para cifrar.");
            return;
        }
        Archivo a = disco.archivos.get(archivo);
        if (a.estaCifrado) {
            imprimir("[seguridad] el archivo ya estaba cifrado.");
            return;
        }
        try {
            SecureRandom rand = new SecureRandom();
            byte[] salt = new byte[16];
            byte[] iv = new byte[12];
            rand.nextBytes(salt);
            rand.nextBytes(iv);

            SecretKeySpec llave = hacerLlave(clave, salt);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, llave, new GCMParameterSpec(128, iv));
            cipher.updateAAD(archivo.getBytes(StandardCharsets.UTF_8));

            byte[] cifrado = cipher.doFinal(a.datos);

            // juntamos salt (16) + iv (12) + datos cifrados
            ByteArrayOutputStream junto = new ByteArrayOutputStream();
            junto.write(salt);
            junto.write(iv);
            junto.write(cifrado);

            a.datos = junto.toByteArray();
            a.estaCifrado = true;
            guardarDisco();
            imprimir("[seguridad] archivo '" + archivo + "' cifrado bien (" + a.datos.length + " bytes).");
        } catch (Exception e) {
            imprimir("[seguridad] error al cifrar: " + e.getMessage());
        }
    }

    // descifra el archivo y revisa que no este alterado ni tenga clave mala
    static void descifrar(String archivo, String clave) {
        if (!disco.archivos.containsKey(archivo)) {
            imprimir("[seguridad] no existe el archivo.");
            return;
        }
        if (!tienePermiso(archivo, 'r')) {
            imprimir("[seguridad] denegado: ocupas permiso de lectura para descifrar.");
            return;
        }
        Archivo a = disco.archivos.get(archivo);
        if (!a.estaCifrado) {
            imprimir("[seguridad] el archivo no esta cifrado.");
            return;
        }
        try {
            byte[] todo = a.datos;
            byte[] salt = Arrays.copyOfRange(todo, 0, 16);
            byte[] iv = Arrays.copyOfRange(todo, 16, 28);
            byte[] cifrado = Arrays.copyOfRange(todo, 28, todo.length);

            SecretKeySpec llave = hacerLlave(clave, salt);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, llave, new GCMParameterSpec(128, iv));
            cipher.updateAAD(archivo.getBytes(StandardCharsets.UTF_8));

            byte[] original = cipher.doFinal(cifrado);

            a.datos = original;
            a.estaCifrado = false;
            guardarDisco();
            imprimir("[seguridad] archivo '" + archivo + "' descifrado correctamente.");
        } catch (javax.crypto.AEADBadTagException e) {
            imprimir("[alerta aead] clave incorrecta o el archivo fue alterado. no se descifro nada.");
        } catch (Exception e) {
            imprimir("[seguridad] fallo al descifrar: " + e.getMessage());
        }
    }

    // imprime en pantalla y guarda en el archivo log
    static void imprimir(String texto) {
        System.out.println(texto);
        if (log != null) log.println(texto);
    }

    // saca el sha-256 de un arreglo de bytes para comparar
    static String sha256(byte[] datos) throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        byte[] digest = md.digest(datos);
        StringBuilder sb = new StringBuilder();
        for (byte b : digest) sb.append(String.format("%02x", b));
        return sb.toString();
    }

    // corre todos los pasos de la practica automaticamente
    static void correrDemo() throws Exception {
        Files.deleteIfExists(Paths.get(DISCO));
        Files.deleteIfExists(Paths.get(RESPALDO));
        Files.deleteIfExists(Paths.get(REF));
        cargarDisco();

        imprimir("\n--- inicio de demostracion tarea 8 ---");

        // 1. docente da permisos
        imprimir("\n1. docente entra y da permisos:");
        login("docente", "prof2026");
        darPermiso("Ana", "notas", "rw");
        darPermiso("Bruno", "notas", "r");

        // 2. ana escribe y guarda referencia
        imprimir("\n2. ana escribe en notas y guardamos copia de referencia:");
        login("Ana", "ana123");
        String textoPrueba = "calificaciones de sistemas operativos\nana: 100\nbruno: 95\n";
        escribir("notas", textoPrueba);
        byte[] refBytes = textoPrueba.getBytes(StandardCharsets.UTF_8);
        Files.write(Paths.get(REF), refBytes);
        String hashAntes = sha256(refBytes);
        imprimir("[ref] guardado en " + REF + " | sha-256: " + hashAntes);

        // 3. ana cifra y lee para ver que sale cifrado
        imprimir("\n3. ana cifra el archivo y hace respaldo:");
        cifrar("notas", "claveAna123");
        Files.copy(Paths.get(DISCO), Paths.get(RESPALDO), StandardCopyOption.REPLACE_EXISTING);
        imprimir("[respaldo] copia guardada en " + RESPALDO);
        leer("notas");

        // 4. reiniciamos y checamos que siga guardado todo
        imprimir("\n4. reiniciando sistema y cargando disk.img de nuevo:");
        usuarioActivo = null;
        disco = null;
        cargarDisco();
        login("Ana", "ana123");
        imprimir("[check] permisos guardados en notas: " + disco.permisos.get("notas"));
        imprimir("[check] sigue cifrado?: " + disco.archivos.get("notas").estaCifrado);
        leer("notas");

        // 5. bruno intenta escribir y leer sin clave
        imprimir("\n5. bruno intenta escribir y leer:");
        login("Bruno", "bruno123");
        escribir("notas", "bruno quiere cambiar su calificacion");
        leer("notas");

        // 6. pruebas aead (clave mala y byte alterado)
        imprimir("\n6. pruebas de clave mala y archivo alterado:");
        login("Ana", "ana123");
        byte[] copiaCifrada = disco.archivos.get("notas").datos.clone();

        imprimir("intentando con clave mala:");
        descifrar("notas", "claveEquivocada");
        imprimir("[check] sigue cifrado?: " + disco.archivos.get("notas").estaCifrado);

        imprimir("alterando 1 byte del archivo cifrado:");
        disco.archivos.get("notas").datos[35] ^= 1;
        descifrar("notas", "claveAna123");
        imprimir("[check] sigue cifrado?: " + disco.archivos.get("notas").estaCifrado);

        // regresamos el archivo sin alterar para descifrarlo bien
        disco.archivos.get("notas").datos = copiaCifrada;
        guardarDisco();

        // 7. ana descifra con la clave buena y comparamos bytes
        imprimir("\n7. ana descifra con la clave correcta y comparamos:");
        descifrar("notas", "claveAna123");
        byte[] recuperados = leer("notas");
        byte[] originales = Files.readAllBytes(Paths.get(REF));
        String hashDespues = sha256(recuperados);

        imprimir("\n--- comparacion final ---");
        imprimir("bytes antes  : " + originales.length + " | sha-256: " + hashAntes);
        imprimir("bytes despues: " + recuperados.length + " | sha-256: " + hashDespues);
        imprimir("son exactamente iguales?: " + Arrays.equals(originales, recuperados));
    }

    // consola interactiva por si se quiere probar comando por comando
    static void iniciarConsola() {
        Scanner sc = new Scanner(System.in);
        System.out.println("consola sos lista. escribe 'demo' para correr todo o 'help' para comandos.");
        while (true) {
            System.out.print("sos" + (usuarioActivo != null ? "(" + usuarioActivo + ")" : "") + "> ");
            if (!sc.hasNextLine()) break;
            String linea = sc.nextLine().trim();
            if (linea.isEmpty()) continue;
            if (linea.equals("exit")) break;

            String[] p = linea.split("\\s+", 4);
            try {
                switch (p[0]) {
                    case "help":
                        System.out.println("comandos:");
                        System.out.println("  auth login <usuario> <pass>");
                        System.out.println("  sec permiso <usuario> <archivo> <rw|r>");
                        System.out.println("  fs write <archivo> <texto>");
                        System.out.println("  fs read <archivo>");
                        System.out.println("  sec cifrar <archivo> <clave>");
                        System.out.println("  sec descifrar <archivo> <clave>");
                        System.out.println("  demo");
                        break;
                    case "auth":
                        if (p.length >= 4 && p[1].equals("login")) login(p[2], p[3]);
                        break;
                    case "sec":
                        if (p.length >= 4 && p[1].equals("permiso")) {
                            String[] sub = p[3].split("\\s+", 2);
                            if (sub.length == 2) darPermiso(p[2], sub[0], sub[1]);
                        } else if (p.length >= 4 && p[1].equals("cifrar")) {
                            cifrar(p[2], p[3]);
                        } else if (p.length >= 4 && p[1].equals("descifrar")) {
                            descifrar(p[2], p[3]);
                        }
                        break;
                    case "fs":
                        if (p.length >= 4 && p[1].equals("write")) escribir(p[2], p[3]);
                        else if (p.length >= 3 && p[1].equals("read")) leer(p[2]);
                        break;
                    case "demo":
                        correrDemo();
                        break;
                    default:
                        System.out.println("comando no valido, escribe 'help'.");
                }
            } catch (Exception e) {
                imprimir("error: " + e.getMessage());
            }
        }
    }
}