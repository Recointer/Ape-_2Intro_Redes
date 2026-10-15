import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Interpreta una línea de texto y la convierte en un objeto Paquete.
 *
 * Una línea válida tiene esta forma (4 bloques separados por '|'):
 *   ETH{...}|IP{...}|TCP{...} o UDP{...} o ICMP{...}|APP{...}
 *
 * Además de separar los bloques y extraer los pares clave=valor, valida los datos
 * (formato de MAC e IP, rangos de TTL y puertos, campos obligatorios, coherencia
 * entre el protocolo de IP y la cabecera de transporte). Si algo falla lanza
 * PaqueteInvalidoException.
 */
public class AnalizadorPaquetes {

    // Un bloque tiene la forma NOMBRE{contenido}. Grupo 1 = nombre, grupo 2 = contenido.
    private static final Pattern BLOQUE = Pattern.compile("^([A-Z]+)\\{(.*)\\}$");

    // Dirección MAC: seis pares hexadecimales separados por ':' (AA:BB:CC:DD:EE:01)
    private static final Pattern MAC = Pattern.compile("^([0-9A-Fa-f]{2}:){5}[0-9A-Fa-f]{2}$");

    // Protocolos de transporte aceptados
    private static final Set<String> TRANSPORTES = Set.of("TCP", "UDP", "ICMP");

    /**
     * Analiza una línea del archivo.
     *
     * @param numeroLinea número de línea (para guardarlo en el paquete)
     * @param linea       texto de la línea
     * @return el paquete ya interpretado
     * @throws PaqueteInvalidoException si la línea está vacía o mal formada
     */
    public Paquete analizar(int numeroLinea, String linea) throws PaqueteInvalidoException {
        // 1) Las líneas vacías no son paquetes
        if (linea == null || linea.isBlank()) {
            throw new PaqueteInvalidoException("linea vacia");
        }

        // 2) Separar por '|'. El -1 conserva los bloques vacíos del final para detectarlos.
        String[] bloques = linea.trim().split("\\|", -1);
        if (bloques.length != 4) {
            throw new PaqueteInvalidoException("se esperaban 4 bloques y hay " + bloques.length);
        }

        // 3) Extraer cada cabecera en el orden esperado: ETH, IP, transporte y APP.
        //    Para el transporte se pasa null porque puede ser TCP, UDP o ICMP.
        Cabecera eth = extraerCabecera(bloques[0], "ETH", "src", "dst", "type");
        Cabecera ip = extraerCabecera(bloques[1], "IP", "src", "dst", "ttl", "proto");
        Cabecera transporte = extraerCabecera(bloques[2], null);
        Cabecera app = extraerCabecera(bloques[3], "APP", "proto", "data");

        // 4) Validar el contenido de las cabeceras de acceso e internet
        validarMac(eth.get("src"));
        validarMac(eth.get("dst"));
        validarIp(ip.get("src"));
        validarIp(ip.get("dst"));
        validarRango(ip.get("ttl"), 255, "ttl");

        // 5) Validar la capa de transporte
        String tipo = transporte.getNombre();
        if (!TRANSPORTES.contains(tipo)) {
            throw new PaqueteInvalidoException("cabecera de transporte desconocida: " + tipo);
        }
        // El campo proto de IP debe coincidir con la cabecera que realmente viene
        if (!tipo.equals(ip.get("proto"))) {
            throw new PaqueteInvalidoException("proto de IP (" + ip.get("proto")
                    + ") no coincide con la cabecera " + tipo);
        }
        // Cada protocolo de transporte exige campos distintos
        switch (tipo) {
            case "TCP" -> exigir(transporte, "sport", "dport", "seq", "ack", "flags");
            case "UDP" -> exigir(transporte, "sport", "dport", "length");
            default -> exigir(transporte, "type", "code", "id", "seq"); // ICMP
        }
        // Los puertos (si existen) deben estar entre 0 y 65535
        for (String k : new String[]{"sport", "dport"}) {
            if (transporte.tiene(k)) validarRango(transporte.get(k), 65535, k);
        }

        return new Paquete(numeroLinea, eth, ip, transporte, app);
    }

    /**
     * Convierte un bloque de texto (por ejemplo "IP{src=1.1.1.1,ttl=64}") en una Cabecera.
     *
     * @param texto          el bloque completo
     * @param nombreEsperado nombre que debe tener (ETH, IP, APP) o null si cualquiera sirve
     * @param obligatorios   campos que deben estar presentes
     */
    private Cabecera extraerCabecera(String texto, String nombreEsperado, String... obligatorios)
            throws PaqueteInvalidoException {
        // Comprobar la forma NOMBRE{...}
        Matcher m = BLOQUE.matcher(texto.trim());
        if (!m.matches()) {
            throw new PaqueteInvalidoException("bloque mal formado: \"" + texto + "\"");
        }
        String nombre = m.group(1);
        if (nombreEsperado != null && !nombre.equals(nombreEsperado)) {
            throw new PaqueteInvalidoException("se esperaba " + nombreEsperado + " y se encontro " + nombre);
        }

        // Separar el contenido por comas y cada par por el primer '='.
        // Se usa el primer '=' para que un valor pueda contener otros '='.
        Map<String, String> campos = new LinkedHashMap<>();
        for (String par : m.group(2).split(",")) {
            int i = par.indexOf('=');
            if (i <= 0) {
                throw new PaqueteInvalidoException("par clave=valor invalido en " + nombre + ": \"" + par + "\"");
            }
            campos.put(par.substring(0, i).trim(), par.substring(i + 1).trim());
        }

        Cabecera c = new Cabecera(nombre, campos);
        exigir(c, obligatorios);
        return c;
    }

    /** Verifica que la cabecera tenga todos los campos indicados. */
    private void exigir(Cabecera c, String... claves) throws PaqueteInvalidoException {
        for (String k : claves) {
            if (!c.tiene(k)) {
                throw new PaqueteInvalidoException("falta el campo '" + k + "' en " + c.getNombre());
            }
        }
    }

    /** Valida que la MAC tenga el formato XX:XX:XX:XX:XX:XX. */
    private void validarMac(String mac) throws PaqueteInvalidoException {
        if (!MAC.matcher(mac).matches()) throw new PaqueteInvalidoException("MAC invalida: " + mac);
    }

    /** Valida una IPv4: cuatro octetos numéricos entre 0 y 255 separados por puntos. */
    private void validarIp(String ip) throws PaqueteInvalidoException {
        String[] partes = ip.split("\\.", -1);
        if (partes.length != 4) throw new PaqueteInvalidoException("IP invalida: " + ip);
        for (String octeto : partes) {
            validarRango(octeto, 255, "octeto de IP " + ip);
        }
    }

    /**
     * Valida que un texto sea un entero entre 0 y max (ambos incluidos).
     * Se usa para TTL (máx. 255), octetos de IP (255) y puertos (65535).
     */
    private void validarRango(String valor, int max, String campo) throws PaqueteInvalidoException {
        try {
            int v = Integer.parseInt(valor);
            if (v < 0 || v > max) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            throw new PaqueteInvalidoException(campo + " fuera de rango o no numerico: " + valor);
        }
    }
}
