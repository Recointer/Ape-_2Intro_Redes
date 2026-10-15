# Analizador de tráfico de red basado en el modelo TCP/IP

Práctica de **Introducción a Redes** (Nivel 3B). Aplicación de consola en Java que lee un archivo de texto con paquetes de red **simulados**, separa las cabeceras de cada capa del modelo TCP/IP, las muestra de forma ordenada y genera un resumen estadístico.

No captura tráfico real, no usa sockets y no usa librerías externas: solo la biblioteca estándar de Java.

---

## 1. Requisitos

- **JDK 17 o superior** (el código usa `switch` con flechas y `String.isBlank()`). Se probó con JDK 26.
- Comprobar la instalación:

```bash
java -version
javac -version
```

## 2. Estructura del proyecto

```
AnalizadorTrafico/
├── paquetes.txt          # dataset de entrada (un paquete por línea)
├── resultado.txt         # salida exportada (se genera al ejecutar)
├── README.md
├── src/                  # código fuente
│   ├── Main.java
│   ├── LectorArchivo.java
│   ├── AnalizadorPaquetes.java
│   ├── Paquete.java
│   ├── Cabecera.java
│   ├── Estadisticas.java
│   ├── Reporte.java
│   └── PaqueteInvalidoException.java
└── out/                  # clases compiladas (.class)
```

## 3. Cómo ponerlo en marcha

Desde la carpeta `AnalizadorTrafico`:

**Compilar**

```bash
javac -encoding UTF-8 -d out src/*.java
```

**Ejecutar (solo por pantalla)**

```bash
java -cp out Main paquetes.txt
```

**Ejecutar y exportar el análisis a un archivo**

```bash
java -cp out Main paquetes.txt resultado.txt
```

Argumentos:

| Posición | Significado | Valor por defecto |
|---|---|---|
| 1 | Archivo de entrada | `paquetes.txt` |
| 2 | Archivo de salida (opcional) | no exporta |

Si no pasas argumentos, `java -cp out Main` lee `paquetes.txt` del directorio actual.

## 4. Formato de entrada

Cada línea es un paquete con cuatro bloques separados por `|`:

```
ETH{src=..,dst=..,type=..}|IP{src=..,dst=..,ttl=..,proto=..}|<TRANSPORTE>{...}|APP{proto=..,data=..}
```

El bloque de transporte es uno de:

| Cabecera | Campos |
|---|---|
| `TCP` | `sport`, `dport`, `seq`, `ack`, `flags` |
| `UDP` | `sport`, `dport`, `length` |
| `ICMP` | `type`, `code`, `id`, `seq` |

Ejemplo real del dataset:

```
ETH{src=AA:BB:CC:DD:EE:01,dst=FF:EE:DD:CC:BB:01,type=IPv4}|IP{src=192.168.1.10,dst=93.184.216.34,ttl=64,proto=TCP}|TCP{sport=51514,dport=80,seq=1001,ack=2001,flags=PSH-ACK}|APP{proto=HTTP,data=GET_/index.html}
```

## 5. Cómo funciona el código

### Capas del modelo y cabeceras

| Capa TCP/IP | Cabecera | Qué se extrae |
|---|---|---|
| Acceso a red | `ETH` | MAC origen, MAC destino, tipo de trama |
| Internet | `IP` | IP origen, IP destino, TTL, protocolo |
| Transporte | `TCP` / `UDP` / `ICMP` | puertos, seq/ack, flags, length, type/code/id |
| Aplicación | `APP` | protocolo de aplicación y datos |

### Clases

| Clase | Responsabilidad |
|---|---|
| `Main` | Punto de entrada. Orquesta el flujo: leer → analizar línea a línea → acumular estadísticas → imprimir/exportar. No contiene lógica de parsing. |
| `LectorArchivo` | Lee el archivo y devuelve una `List<String>` con todas las líneas, en orden. |
| `AnalizadorPaquetes` | Convierte una línea de texto en un `Paquete`: divide por `\|`, reconoce cada cabecera con una expresión regular, extrae los pares `clave=valor` y **valida** los datos. Lanza `PaqueteInvalidoException` si algo está mal. |
| `Paquete` | Modelo de un paquete: compone cuatro `Cabecera` (acceso, internet, transporte, aplicación) y ofrece accesos cómodos (`getIpOrigen()`, `getPuertoDestino()`...). |
| `Cabecera` | Una cabecera: su nombre (`ETH`, `IP`…) y un `Map<String,String>` con sus campos. Es inmutable hacia fuera (encapsulamiento). |
| `Estadisticas` | Acumula los conteos con `Map`: por transporte, por protocolo de aplicación, IP origen/destino, puerto destino y flags TCP. Calcula el elemento más frecuente. |
| `Reporte` | Construye el texto de salida: detalle por paquete, líneas inválidas y resumen final. |
| `PaqueteInvalidoException` | Excepción propia para líneas mal formadas. |

### Flujo de ejecución

```
paquetes.txt ──► LectorArchivo ──► List<String>
                                       │  (por cada línea)
                                       ▼
                              AnalizadorPaquetes.analizar()
                               │                    │
                        Paquete válido      PaqueteInvalidoException
                               │                    │
                  Estadisticas.registrar()   se guarda en lista de errores
                  Reporte.detallePaquete()          │
                               └────────┬───────────┘
                                        ▼
                         Reporte (inválidas + resumen) ──► consola / archivo
```

### Validaciones que realiza

- La línea no está vacía y tiene exactamente 4 bloques.
- Cada bloque tiene la forma `NOMBRE{...}` y los pares son `clave=valor`.
- Los bloques están en orden `ETH`, `IP`, transporte, `APP`.
- MAC con formato `XX:XX:XX:XX:XX:XX`; IP con 4 octetos entre 0 y 255.
- TTL entre 0 y 255; puertos entre 0 y 65535.
- El campo `proto` de `IP` coincide con la cabecera de transporte (`TCP`, `UDP` o `ICMP`).
- Están presentes los campos obligatorios de cada cabecera.

### Principios de POO aplicados

- **Encapsulamiento:** atributos `private final`, mapa de campos no modificable desde fuera.
- **Composición:** un `Paquete` se compone de cuatro `Cabecera`.
- **Separación de responsabilidades:** leer, analizar, contar y presentar están en clases distintas.
- **Colecciones:** `List` para las líneas, `Map` (`LinkedHashMap`, `HashMap`, `TreeMap`) para campos y conteos.

## 6. Qué muestra el programa

Por cada paquete válido:

```
=== Paquete (linea 1) ===
[Acceso a red - Ethernet]
  MAC origen : AA:BB:CC:DD:EE:01
  ...
[Internet - IP]
  ...
[Transporte - TCP]
  ...
[Aplicacion]
  Protocolo: HTTP
  Datos    : GET_/index.html
```

Al final, las líneas no procesadas y el resumen: total de paquetes, cantidad de TCP/UDP/ICMP, paquetes por protocolo de aplicación, conteo de flags TCP, IP origen y destino más frecuentes y puerto destino más frecuente.

### Resultados con el `paquetes.txt` entregado

| Dato | Valor |
|---|---|
| Líneas en el archivo | 174 |
| Paquetes procesados | 160 |
| Líneas vacías reportadas | 14 |
| TCP / UDP / ICMP | 104 / 42 / 14 |
| IP origen más frecuente | `1.1.1.1` (7) |
| IP destino más frecuente | `93.184.216.34` (14) |
| Puerto destino más frecuente | `25` (18) |

## 7. Preguntas frecuentes

**¿Por qué dice que hay 14 líneas "no procesadas"? ¿Es un error?**
No. El dataset contiene líneas en blanco (11, 22, 33, 45…). El enunciado exige reportar las líneas mal formadas sin terminar el programa, y eso es lo que se hace. Los 160 paquetes restantes se procesan normalmente.

**¿Qué pasa si una línea está mal escrita?**
Se captura la `PaqueteInvalidoException`, se anota `Linea N: motivo` y se continúa con la siguiente. Al final aparecen todas en la sección "LINEAS NO PROCESADAS".

**Me sale `Error: Could not find or load main class Main`.**
Primero compila (`javac -encoding UTF-8 -d out src/*.java`) y ejecuta desde la carpeta raíz con `-cp out`. Verifica que existe `out/Main.class`.

**Me sale `No se pudo leer 'paquetes.txt'`.**
Ejecuta desde la carpeta donde está el archivo, o pasa la ruta completa como primer argumento.

**`javac` no se reconoce como comando.**
Falta el JDK o no está en el `PATH`. Instala un JDK (17 o superior) y reabre la terminal.

**Se ven mal las tildes o caracteres raros en la consola.**
El programa escribe en UTF-8. Los textos de salida están sin tildes a propósito para evitar el problema; el archivo `resultado.txt` se guarda en UTF-8.

**¿Por qué el puerto destino más frecuente no cuenta los paquetes ICMP?**
ICMP no usa puertos (trabaja con `type`/`code`/`id`), por lo que solo cuentan TCP y UDP.

**¿Por qué los datos aparecen como `GET_/index.html` con guion bajo?**
Así vienen en el dataset. El programa no separa por `_`: toma todo lo que hay después de `data=` como un solo valor.

**¿Qué pasa si hay un empate en "el más frecuente"?**
Se elige el valor menor en orden alfabético, para que el resultado sea siempre el mismo.

**¿Puedo usar otro archivo de paquetes?**
Sí, pásalo como primer argumento. Mientras respete el formato de la sección 4, funcionará.

**¿Cómo agrego un nuevo protocolo de transporte?**
En `AnalizadorPaquetes`: añádelo al `Set TRANSPORTES` y define sus campos obligatorios en el `switch`. Las estadísticas por transporte lo contarán automáticamente; para mostrarlo con su propia línea en el resumen, añade una línea en `Reporte.resumen()`.

**¿Cómo agrego una estadística nueva?**
Crea un `Map` en `Estadisticas`, súmalo en `registrar()` y muéstralo en `Reporte.resumen()`.

**¿Qué funcionalidades opcionales incluye?**
Conteo de flags TCP y exportación a archivo. No incluye menú interactivo ni filtros por IP/protocolo (son mejoras sugeridas, no obligatorias).

## 8. Restricciones de la práctica cumplidas

- Solo Java, interfaz por consola.
- Sin librerías externas de parsing, sin sockets, sin captura de tráfico real.
- Solución modular con POO (no está todo en `main`).
- Análisis únicamente sobre el dataset entregado.
