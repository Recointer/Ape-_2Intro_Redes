import java.util.List;
import java.util.Map;

/**
 * Genera el texto que se muestra al usuario.
 *
 * Solo construye cadenas de texto (no imprime ni escribe archivos); Main decide
 * si ese texto va a la consola, a un archivo o a ambos.
 * Se usa String.format con %n para que el salto de línea sea el del sistema operativo.
 */
public class Reporte {

    /**
     * Detalle de un paquete, organizado por capas del modelo TCP/IP.
     *
     * @param p paquete a mostrar
     * @return texto con las cuatro capas del paquete
     */
    public String detallePaquete(Paquete p) {
        Cabecera eth = p.getAcceso();
        Cabecera ip = p.getInternet();
        Cabecera t = p.getTransporte();
        Cabecera app = p.getAplicacion();

        StringBuilder sb = new StringBuilder();
        sb.append(String.format("=== Paquete (linea %d) ===%n", p.getNumeroLinea()));

        // Capa de acceso a red
        sb.append(String.format("[Acceso a red - Ethernet]%n"));
        sb.append(String.format("  MAC origen : %s%n  MAC destino: %s%n  Tipo trama : %s%n",
                eth.get("src"), eth.get("dst"), eth.get("type")));

        // Capa de Internet
        sb.append(String.format("[Internet - IP]%n"));
        sb.append(String.format("  IP origen : %s%n  IP destino: %s%n  TTL       : %s%n  Protocolo : %s%n",
                ip.get("src"), ip.get("dst"), ip.get("ttl"), ip.get("proto")));

        // Capa de transporte: se recorren todos los campos porque cambian según el
        // protocolo (TCP: puertos/seq/ack/flags, UDP: puertos/length, ICMP: type/code/id/seq)
        sb.append(String.format("[Transporte - %s]%n", t.getNombre()));
        for (Map.Entry<String, String> e : t.getCampos().entrySet()) {
            sb.append(String.format("  %-6s: %s%n", e.getKey(), e.getValue()));
        }

        // Capa de aplicación
        sb.append(String.format("[Aplicacion]%n  Protocolo: %s%n  Datos    : %s%n",
                app.get("proto"), app.get("data")));
        return sb.toString();
    }

    /**
     * Lista de las líneas que no se pudieron procesar y el motivo de cada una.
     *
     * @param errores mensajes ya formateados ("Linea N: motivo")
     */
    public String lineasInvalidas(List<String> errores) {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("=== LINEAS NO PROCESADAS (%d) ===%n", errores.size()));
        for (String e : errores) {
            sb.append("  ").append(e).append(System.lineSeparator());
        }
        return sb.toString();
    }

    /**
     * Resumen estadístico final con los datos que pide la práctica.
     *
     * @param s estadísticas acumuladas de todos los paquetes
     */
    public String resumen(Estadisticas s) {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("%n=== RESUMEN ESTADISTICO ===%n"));

        // Total y cantidad por protocolo de transporte
        sb.append(String.format("Total de paquetes procesados: %d%n", s.getTotal()));
        sb.append(String.format("Paquetes TCP : %d%nPaquetes UDP : %d%nPaquetes ICMP: %d%n",
                s.getTransporte("TCP"), s.getTransporte("UDP"), s.getTransporte("ICMP")));

        // Cantidad por protocolo de aplicación
        sb.append(String.format("Paquetes por protocolo de aplicacion:%n"));
        for (Map.Entry<String, Integer> e : s.getPorAplicacion().entrySet()) {
            sb.append(String.format("  %-6s: %d%n", e.getKey(), e.getValue()));
        }

        // Conteo de flags TCP (funcionalidad adicional)
        sb.append(String.format("Conteo de flags TCP:%n"));
        for (Map.Entry<String, Integer> e : s.getFlagsTcp().entrySet()) {
            sb.append(String.format("  %-8s: %d%n", e.getKey(), e.getValue()));
        }

        // Elementos más frecuentes
        sb.append(String.format("IP origen mas frecuente   : %s%n", Estadisticas.masFrecuente(s.getIpOrigen())));
        sb.append(String.format("IP destino mas frecuente  : %s%n", Estadisticas.masFrecuente(s.getIpDestino())));
        sb.append(String.format("Puerto destino mas frecuente (TCP/UDP): %s%n",
                Estadisticas.masFrecuente(s.getPuertoDestino())));
        return sb.toString();
    }
}
