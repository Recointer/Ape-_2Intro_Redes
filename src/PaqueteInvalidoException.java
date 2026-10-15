/**
 * Excepción propia del proyecto.
 *
 * Se lanza cuando una línea del archivo de paquetes no cumple la estructura
 * esperada (línea vacía, bloque mal formado, campo faltante, IP/MAC inválida, etc.).
 * Al ser una excepción verificada, obliga a quien analiza una línea a decidir qué
 * hacer con ella; en este caso Main la registra y continúa con la siguiente línea,
 * de modo que el programa nunca termina de forma abrupta.
 */
public class PaqueteInvalidoException extends Exception {

    /**
     * @param mensaje descripción del motivo por el cual la línea es inválida
     */
    public PaqueteInvalidoException(String mensaje) {
        super(mensaje);
    }
}
