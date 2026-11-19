# SOS-LinuxInspired — Simulador Educativo de Sistema Operativo

**Instituto Tecnológico Superior de Cajeme (ITESCA)**  
* **Materia:** Sistemas Operativos  
* **Docente:** Mtro. Manuel de Jesús Román Román  
* **Grupo:** WSO 02 | **Equipo:** 4  
* **Fecha de entrega:** 27 de noviembre de 2026  

---

## Integrantes y Reparto de Trabajo

| Integrante | Paquetes asignados | Responsabilidad principal |
| :--- | :--- | :--- |
| **Palomares Quintero Regina** | `sos.core`, `sos.cpu`, `sos.cli`, `sos.seguridad` | **Módulo 1:** Núcleo compartido (`PCB`, `Reloj`, `EstadoProceso`), Planificador de CPU (`FCFS`, `SJF`, `RR`, `Prioridades` y `MLFQ`), Shell estilo Linux y Seguridad (AES-256-GCM / Matriz de acceso). |
| **Guzmán Sánchez Vanessa** | `sos.io` | **Módulo 4:** Administración de Entrada/Salida, drivers simulados (`disco` y `terminal`), colas y planificación de cabezal (`FCFS`, `SCAN`, `C-SCAN`). |
| **López Ahumada Juan Pablo** | `sos.memoria` | **Módulo 2:** Administrador de Memoria, paginación, tabla de páginas por proceso y reemplazo (`FIFO`, `LRU`, `Óptimo`). |
| **Wong Reyes David** | `sos.fs` | **Módulo 3:** Sistema de Archivos persistente en `disk.img`, superbloque, bitmap, directorios jerárquicos y recuperación (`fsck`). |

---

## Bitácora de Avances del Repositorio

* **19 de noviembre de 2026:**  
  * Se creó el repositorio oficial `SOS-LinuxInspired` con la estructura modular de paquetes en Java (`core`, `cpu`, `memoria`, `fs`, `io`, `seguridad`, `cli`).
  * Se subieron las asignaciones base con código funcional correspondiente al **Tronco Común** (`EstadoProceso.java`, `Reloj.java`, `PCB.java`), la interfaz de consola estilo Linux (`ShellLinux.java`, `Main.java` y el lanzador `./sos`) y el **Módulo 1: Planificador de CPU** (`PlanificadorCPU.java`), desarrollados por **Regina Palomares**.
  * Se verificó la coincidencia exacta de los algoritmos `FCFS`, `SJF` y `Round Robin (q=2)` contra los valores teóricos del caso de control (`6.6`, `4.0` y `7.6`), además de incorporar el algoritmo extra `MLFQ`.

---

## Arquitectura del Proyecto

```text
SOS-LinuxInspired/
├── sos                         <-- Script ejecutable en terminal Linux (./sos)
├── README.md                   <-- Documentación técnica, acuerdos y bitácora
├── escenarios/                 <-- Archivos .txt con cargas de trabajo y demos
└── src/
    └── sos/
        ├── Main.java           <-- Punto de entrada único del simulador
        ├── core/               <-- Contratos compartidos (PCB, EstadoProceso, Reloj)
        ├── cpu/                <-- Módulo 1: Planificador de CPU (Unidad 2)
        ├── memoria/            <-- Módulo 2: Administrador de Memoria (Unidad 3)
        ├── fs/                 <-- Módulo 3: Sistema de Archivos (Unidad 5)
        ├── io/                 <-- Módulo 4: Entrada/Salida y Drivers (Unidad 4)
        ├── seguridad/          <-- Módulo 5: Autenticación, Permisos y Cifrado (Unidad 6)
        └── cli/                <-- Consola interactiva estilo GNU/Linux (ShellLinux)
