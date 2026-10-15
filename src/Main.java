import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * Clase principal del programa.
 *
 * Solo coordina el flujo; la lógica está en las demás clases:
 *   LectorArchivo -> AnalizadorPaquetes -> Estadisticas -> Reporte
 *
 * Uso: java Main [archivo_entrada] [archivo_salida]
 *   - archivo_entrada: por defecto "paquetes.txt"
 *   - archivo_salida:  opcional; si se indica, se exporta el análisis a ese archivo
 */
public class Main {

    public static void main(String[] args) {
        // Argumentos opcionales de la línea de comandos
        String entrada = args.length > 0 ? args[0] : "paquetes.txt";
        String salida = args.length > 1 ? args[1] : null;

        // 1) Leer todas las líneas del archivo. Si no se puede leer, se informa y se termina
        //    de forma controlada (sin lanzar una excepción sin atrapar).
        List<String> lineas;
        try {
            lineas = new LectorArchivo(entrada).leerLineas();
        } catch (IOException e) {
            System.err.println("No se pudo leer '" + entrada + "': " + e.getMessage());
            return;
        }

        // Objetos que colaboran en el análisis
        AnalizadorPaquetes analizador = new AnalizadorPaquetes();
        Estadisticas stats = new Estadisticas();
        Reporte reporte = new Reporte();
        StringBuilder out = new StringBuilder();   // aquí se acumula todo el texto de salida
        List<String> errores = new ArrayList<>();  // líneas que no se pudieron procesar

        // 2) Procesar cada línea de forma independiente
        for (int i = 0; i < lineas.size(); i++) {
            int numero = i + 1; // las líneas del archivo se numeran desde 1
            try {
                Paquete p = analizador.analizar(numero, lineas.get(i));
                stats.registrar(p);                                            // contar
                out.append(reporte.detallePaquete(p)).append(System.lineSeparator()); // mostrar
            } catch (PaqueteInvalidoException e) {
                // Línea mal formada: se anota el motivo y se sigue con la siguiente
                errores.add("Linea " + numero + ": " + e.getMessage());
            }
        }

        // 3) Agregar las líneas inválidas (si las hay) y el resumen estadístico
        if (!errores.isEmpty()) {
            out.append(reporte.lineasInvalidas(errores));
        }
        out.append(reporte.resumen(stats));

        // 4) Mostrar en consola. Se fuerza UTF-8 para evitar caracteres raros en Windows.
        PrintStream consola = new PrintStream(System.out, true, StandardCharsets.UTF_8);
        consola.print(out);

        // 5) Exportar a archivo si el usuario indicó un segundo argumento
        if (salida != null) {
            try {
                Files.writeString(Paths.get(salida), out.toString(), StandardCharsets.UTF_8);
                consola.println("Analisis exportado a " + salida);
            } catch (IOException e) {
                System.err.println("No se pudo exportar: " + e.getMessage());
            }
        }
    }
}
