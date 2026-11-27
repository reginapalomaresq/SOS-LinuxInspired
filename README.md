# SOS-LinuxInspired — Simulador Educativo de Sistema Operativo

**Instituto Tecnológico Superior de Cajeme (ITESCA)**  
- **Materia:** Sistemas Operativos  
- **Docente:** Mtro. Manuel de Jesús Román Román  
- **Grupo:** WSO 02 | **Equipo:** 4  
- **Fecha de entrega:** 27 de noviembre de 2026  

---

## Descripción General

**SOS** (*Simple Operating System*) es un simulador educativo de sistema operativo desarrollado en Java con una interfaz de línea de comandos (CLI) inspirada en GNU/Linux. El proyecto integra en un solo ejecutable (`sos.jar`) los conceptos fundamentales de las **6 unidades de la materia**, operando sobre un núcleo compartido (`PCB`, `Reloj`) y persistencia real en disco mediante `disk.img` y bitácora del sistema en `sos.log`.

---

## Integrantes y Distribución Real del Trabajo

| Integrante | Módulos y Unidades Cubiertas | Paquetes / Entregables | Responsabilidades y Trabajo Realizado |
| :--- | :--- | :--- | :--- |
| **Regina Palomares** | **Arquitectura e Integración Total**<br>**Módulo 1:** Núcleo y Planificación de CPU *(Unidad 2)*<br>**Módulo 5:** Seguridad, Criptografía e Interfaz CLI *(Unidades 1 y 6)*<br>**QA, Correcciones Técnicas y Documentación** | `sos.Main`<br>`sos.core`<br>`sos.cpu`<br>`sos.seguridad`<br>`sos.cli`<br>`sos.jar`<br>`Manual de Uso.pdf`<br>`README.md` | • Diseño de la arquitectura modular del proyecto y administración del repositorio en GitHub.<br>• Desarrollo del tronco común compartido (`PCB`, `EstadoProceso`, `Reloj`) y la clase principal (`Main.java`).<br>• Implementación del planificador de CPU (**FCFS, SJF, Round Robin, Prioridades y MLFQ**) integrado con operaciones de E/S (`tEntradaSalida`).<br>• Desarrollo completo del módulo de seguridad: autenticación obligatoria al arranque con **SHA-256 + salt**, Matriz de Control de Acceso (`rwx`) y cifrado autenticado **AES-256-GCM** con PBKDF2.<br>• Creación de la consola interactiva tipo Linux (`ShellLinux`), bitácora persistente (`sos.log`), comandos parametrizados y ejecución por lotes (`sim run`).<br>• Integración y depuración global de todos los subsistemas (corrección de puntero circular FIFO y tablas de páginas por PID, renombrado recursivo en `mv`, corrupción/reparación física en `fsck` y buffers de drivers en `GestorIO`), empaquetado de `sos.jar` y redacción de toda la documentación técnica. |
| **Juan Pablo López** | **Módulo 2:** Administración de Memoria Virtual *(Unidad 3)* | `sos.memoria` | • Base lógica de paginación virtual sobre marcos físicos.<br>• Algoritmos de reemplazo de páginas (**FIFO, LRU y Óptimo**) con conteo de aciertos y fallos. |
| **Vanessa Guzmán** | **Módulo 3:** Administración de Entrada / Salida *(Unidad 4)* | `sos.io` | • Base lógica de dispositivos simulados (`/dev/sda`, `/dev/tty0`) e interrupciones.<br>• Algoritmos de planificación de cabezal de disco (**FCFS, SCAN y C-SCAN**). |
| **David Wong** | **Módulo 4:** Sistema de Archivos Persistente *(Unidad 5)* | `sos.fs` | • Base lógica de directorios jerárquicos, inodos y mapa de bits (*bitmap*) de 32 bloques.<br>• Persistencia en `disk.img`, fragmentación interna y recuperación ante fallos (`fsck`). |

---

## Arquitectura del Proyecto

```text
SOS-LinuxInspired/
├── README.md                   <-- Documentación técnica y bitácora del proyecto
├── sos.jar                     <-- Paquete ejecutable compilado
├── disk.img                    <-- Imagen de disco virtual generada automáticamente
├── sos.log                     <-- Bitácora persistente de auditoría y comandos ejecutados
└── src/
    └── sos/
        ├── Main.java           <-- Punto de entrada único del sistema
        ├── core/               <-- Estructuras base (PCB, EstadoProceso, Reloj)
        ├── cpu/                <-- Planificador de CPU (FCFS, SJF, RR, Prioridades, MLFQ)
        ├── memoria/            <-- Gestor de Memoria Virtual (FIFO, LRU, Óptimo + Tablas por PID)
        ├── io/                 <-- Gestor de E/S, Drivers con Buffers y Disco (FCFS, SCAN, C-SCAN)
        ├── fs/                 <-- Sistema de Archivos, Bitmap, Inodos, Journal y fsck
        ├── seguridad/          <-- Autenticación SHA-256, ACL (rwx) y Cifrado AES-256-GCM
        └── cli/                <-- Intérprete de comandos estilo GNU/Linux (ShellLinux)
