import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

/**
 * Acumula los conteos de los paquetes procesados.
 *
 * Cada vez que se registra un paquete válido se actualizan los contadores.
 * Los conteos se guardan en mapas (clave -> cantidad de veces que aparece).
 */
public class Estadisticas {

    // Total de paquetes válidos registrados
    private int total;

    // TreeMap mantiene las claves ordenadas alfabéticamente (salida más legible)
    private final Map<String, Integer> porTransporte = new TreeMap<>();  // TCP, UDP, ICMP
    private final Map<String, Integer> porAplicacion = new TreeMap<>();  // HTTP, DNS, SMTP...
    private final Map<String, Integer> flagsTcp = new TreeMap<>();       // SYN, ACK, PSH-ACK...

    // HashMap basta aquí: solo se busca el elemento más frecuente, no se listan
    private final Map<String, Integer> ipOrigen = new HashMap<>();
    private final Map<String, Integer> ipDestino = new HashMap<>();
    private final Map<String, Integer> puertoDestino = new HashMap<>();

    /**
     * Suma un paquete a todos los contadores.
     *
     * @param p paquete ya validado
     */
    public void registrar(Paquete p) {
        total++;
        sumar(porTransporte, p.getTipoTransporte());
        sumar(porAplicacion, p.getProtocoloAplicacion());
        sumar(ipOrigen, p.getIpOrigen());
        sumar(ipDestino, p.getIpDestino());

        // ICMP no tiene puerto destino, así que solo se cuenta si existe
        if (p.getPuertoDestino() != null) sumar(puertoDestino, p.getPuertoDestino());

        // Los flags solo existen en TCP
        if (p.getTipoTransporte().equals("TCP")) sumar(flagsTcp, p.getTransporte().get("flags"));
    }

    /** Incrementa en 1 el contador de una clave (si no existe, empieza en 1). */
    private static void sumar(Map<String, Integer> mapa, String clave) {
        mapa.merge(clave, 1, Integer::sum);
    }

    /**
     * Busca el elemento que más veces aparece en un mapa de conteos.
     * Si hay empate, gana el menor en orden alfabético, para que el resultado
     * sea siempre el mismo (HashMap no garantiza el orden de recorrido).
     *
     * @param mapa conteos (clave -> cantidad)
     * @return texto "clave (n paquetes)", o "N/A" si el mapa está vacío
     */
    public static String masFrecuente(Map<String, Integer> mapa) {
        String mejor = null;
        int max = 0;
        for (Map.Entry<String, Integer> e : mapa.entrySet()) {
            boolean gana = e.getValue() > max
                    || (e.getValue() == max && mejor != null && e.getKey().compareTo(mejor) < 0);
            if (gana) {
                mejor = e.getKey();
                max = e.getValue();
            }
        }
        return mejor == null ? "N/A" : mejor + " (" + max + " paquetes)";
    }

    // ----- Getters usados por la clase Reporte -----
    public int getTotal() { return total; }

    /** @return cuántos paquetes hay del transporte indicado (0 si ninguno) */
    public int getTransporte(String t) { return porTransporte.getOrDefault(t, 0); }

    public Map<String, Integer> getPorAplicacion() { return porAplicacion; }
    public Map<String, Integer> getFlagsTcp() { return flagsTcp; }
    public Map<String, Integer> getIpOrigen() { return ipOrigen; }
    public Map<String, Integer> getIpDestino() { return ipDestino; }
    public Map<String, Integer> getPuertoDestino() { return puertoDestino; }
}
