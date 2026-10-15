import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * Se encarga únicamente de leer el archivo de texto.
 *
 * No interpreta el contenido: solo entrega las líneas tal como están (incluidas
 * las vacías) para que otra clase las analice. Así la lectura queda separada del
 * análisis (separación de responsabilidades).
 */
public class LectorArchivo {

    // Ruta del archivo que se va a leer
    private final Path ruta;

    /**
     * @param ruta ruta del archivo de paquetes (relativa o absoluta)
     */
    public LectorArchivo(String ruta) {
        this.ruta = Paths.get(ruta);
    }

    /**
     * Lee todo el archivo, línea por línea.
     *
     * El índice de cada elemento + 1 corresponde al número de línea en el archivo,
     * lo que permite reportar con exactitud cuál línea tiene un error.
     *
     * @return lista con todas las líneas del archivo, en orden
     * @throws IOException si el archivo no existe o no se puede leer
     */
    public List<String> leerLineas() throws IOException {
        List<String> lineas = new ArrayList<>();
        // try-with-resources: el lector se cierra solo al terminar, incluso si hay error
        try (BufferedReader br = Files.newBufferedReader(ruta, StandardCharsets.UTF_8)) {
            String linea;
            while ((linea = br.readLine()) != null) {
                lineas.add(linea);
            }
        }
        return lineas;
    }
}
