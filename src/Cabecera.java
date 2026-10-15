import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Representa la cabecera de una capa del modelo TCP/IP
 * (ETH, IP, TCP, UDP, ICMP o APP).
 *
 * Guarda el nombre de la cabecera y sus campos como pares clave-valor.
 * Ejemplo: IP{src=192.168.1.10,dst=8.8.8.8,ttl=64,proto=UDP}
 *   -> nombre = "IP", campos = {src=192.168.1.10, dst=8.8.8.8, ttl=64, proto=UDP}
 */
public class Cabecera {

    // Nombre de la cabecera tal como aparece en el archivo (ETH, IP, TCP...)
    private final String nombre;

    // Campos de la cabecera. Se usa LinkedHashMap para conservar el orden
    // original del archivo al mostrarlos en el reporte.
    private final Map<String, String> campos;

    /**
     * @param nombre nombre de la cabecera
     * @param campos campos clave-valor; se hace una copia para que el objeto
     *               no dependa del mapa original (encapsulamiento)
     */
    public Cabecera(String nombre, Map<String, String> campos) {
        this.nombre = nombre;
        this.campos = new LinkedHashMap<>(campos);
    }

    /** @return el nombre de la cabecera (ETH, IP, TCP, UDP, ICMP o APP) */
    public String getNombre() {
        return nombre;
    }

    /** @return vista de solo lectura de los campos; no se pueden modificar desde fuera */
    public Map<String, String> getCampos() {
        return Collections.unmodifiableMap(campos);
    }

    /**
     * Obtiene el valor de un campo.
     *
     * @param clave nombre del campo (por ejemplo "src" o "ttl")
     * @return el valor, o "-" si el campo no existe
     */
    public String get(String clave) {
        return campos.getOrDefault(clave, "-");
    }

    /**
     * @param clave nombre del campo
     * @return true si la cabecera contiene ese campo
     */
    public boolean tiene(String clave) {
        return campos.containsKey(clave);
    }
}
