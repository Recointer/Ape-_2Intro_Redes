/**
 * Representa un paquete de red simulado.
 *
 * Usa composición: un paquete está formado por cuatro cabeceras, una por cada
 * capa del modelo TCP/IP (acceso a red, internet, transporte y aplicación).
 * Además ofrece métodos de acceso rápido a los datos más usados en las estadísticas.
 */
public class Paquete {

    // Número de línea del archivo de donde se leyó el paquete (empieza en 1)
    private final int numeroLinea;

    // Una cabecera por cada capa del modelo TCP/IP
    private final Cabecera acceso;      // Capa de acceso a red: Ethernet (ETH)
    private final Cabecera internet;    // Capa de Internet: IP
    private final Cabecera transporte;  // Capa de transporte: TCP, UDP o ICMP
    private final Cabecera aplicacion;  // Capa de aplicación: APP

    public Paquete(int numeroLinea, Cabecera acceso, Cabecera internet,
                   Cabecera transporte, Cabecera aplicacion) {
        this.numeroLinea = numeroLinea;
        this.acceso = acceso;
        this.internet = internet;
        this.transporte = transporte;
        this.aplicacion = aplicacion;
    }

    // ----- Getters de las cabeceras -----
    public int getNumeroLinea() { return numeroLinea; }
    public Cabecera getAcceso() { return acceso; }
    public Cabecera getInternet() { return internet; }
    public Cabecera getTransporte() { return transporte; }
    public Cabecera getAplicacion() { return aplicacion; }

    // ----- Métodos de conveniencia para las estadísticas -----

    /** @return "TCP", "UDP" o "ICMP" según la cabecera de transporte */
    public String getTipoTransporte() { return transporte.getNombre(); }

    /** @return dirección IP de origen (campo src de la cabecera IP) */
    public String getIpOrigen() { return internet.get("src"); }

    /** @return dirección IP de destino (campo dst de la cabecera IP) */
    public String getIpDestino() { return internet.get("dst"); }

    /** @return protocolo de aplicación (HTTP, DNS, SMTP...) */
    public String getProtocoloAplicacion() { return aplicacion.get("proto"); }

    /**
     * @return el puerto destino, o null si el transporte no usa puertos.
     *         ICMP no tiene puertos (usa type/code/id), por eso puede devolver null.
     */
    public String getPuertoDestino() {
        return transporte.tiene("dport") ? transporte.get("dport") : null;
    }
}
