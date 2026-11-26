# SOS-LinuxInspired — Simulador Educativo de Sistema Operativo

**Instituto Tecnológico Superior de Cajeme (ITESCA)**  
* **Materia:** Sistemas Operativos  
* **Docente:** Mtro. Manuel de Jesús Román Román  
* **Grupo:** WSO 02 | **Equipo:** 4  
* **Fecha de entrega:** 27 de noviembre de 2026  

---

## Descripción General

**SOS** (*Simple Operating System*) es un simulador educativo de sistema operativo desarrollado en Java con una interfaz de línea de comandos (CLI) inspirada en GNU/Linux. El proyecto integra en un solo ejecutable (`sos.jar`) los conceptos fundamentales de las **6 unidades de la materia**, operando sobre un núcleo compartido (`PCB`, `Reloj`) y persistencia real en disco mediante `disk.img`.

---

## Integrantes y Distribución de Módulos (Temario)

El desarrollo del proyecto se dividió en **5 módulos funcionales** que cubren todas las unidades de la asignatura, asignados equitativamente entre los miembros del equipo:

| Integrante | Módulo / Unidad de la Materia | Paquetes en Java | Responsabilidades Técnicas |
| :--- | :--- | :--- | :--- |
| **Regina Palomares** | **Módulo 1:** Núcleo y Planificación de CPU *(Unidad 2)*<br>**Módulo 5:** Seguridad e Integración *(Unidades 1 y 6)* | `sos.core`<br>`sos.cpu`<br>`sos.seguridad`<br>`sos.cli` | • Implementación del ciclo de vida de procesos (`PCB` con 5 estados) y reloj global.<br>• Algoritmos de planificación (**FCFS, SJF, Round Robin y MLFQ**) con métricas de espera y retorno.<br>• Autenticación de usuarios (**SHA-256 + salt**), Matriz de Control de Acceso (`rwx`) y cifrado de archivos (**AES-256-GCM** con PBKDF2).<br>• Desarrollo de la consola interactiva tipo Linux (`ShellLinux`) y ejecución por lotes (`sim run`). |
| **Juan Pablo López** | **Módulo 2:** Administración de Memoria Virtual *(Unidad 3)* | `sos.memoria` | • Gestión de memoria física dividida en marcos y tablas de páginas por proceso.<br>• Implementación y comparación de algoritmos de reemplazo de páginas (**FIFO, LRU y Óptimo**).<br>• Cálculo de aciertos, fallos de página y tasa porcentual de fallos. |
| **Vanessa Guzmán** | **Módulo 3:** Administración de Entrada / Salida *(Unidad 4)* | `sos.io` | • Simulación de controladores de dispositivos (`/dev/sda` y `/dev/tty0`) y colas de peticiones.<br>• Manejo de interrupciones síncronas y asíncronas.<br>• Planificación de cabezal de disco (**FCFS, SCAN y C-SCAN**) midiendo el recorrido total en cilindros. |
| **David Wong** | **Módulo 4:** Sistema de Archivos Persistente *(Unidad 5)* | `sos.fs` | • Persistencia real en disco virtual (`disk.img`) con bloques de 512 bytes.<br>• Estructura jerárquica de directorios, inodos y asignación de espacio mediante *bitmap*.<br>• Cálculo de fragmentación interna, simulación de daños (`corrupt`) y auditoría/reparación con `fsck`. |

---

## Arquitectura del Proyecto

```text
SOS-LinuxInspired/
├── README.md                   <-- Documentación técnica y reparto de módulos
├── sos.jar                     <-- Paquete ejecutable del simulador
├── disk.img                    <-- Imagen de disco virtual generada automáticamente
└── src/
    └── sos/
        ├── Main.java           <-- Punto de entrada único del sistema
        ├── core/               <-- Estructuras base (PCB, EstadoProceso, Reloj)
        ├── cpu/                <-- Planificador de CPU (FCFS, SJF, RR, MLFQ)
        ├── memoria/            <-- Gestor de Memoria Virtual (FIFO, LRU, Óptimo)
        ├── io/                 <-- Gestor de E/S y Disco (FCFS, SCAN, C-SCAN)
        ├── fs/                 <-- Sistema de Archivos, Bitmap, Inodos y fsck
        ├── seguridad/          <-- Autenticación SHA-256, ACL y Cifrado AES-256-GCM
        └── cli/                <-- Intérprete de comandos estilo GNU/Linux
