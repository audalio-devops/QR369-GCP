package br.com.ia369.prospecting_service.exception;

/**
 * Exceção lançada quando a instância Web da Z-API está desconectada ou inacessível.
 */
public class ZApiDisconnectedException extends RuntimeException {

    public ZApiDisconnectedException(String message) {
        super(message);
    }

    public ZApiDisconnectedException(String message, Throwable cause) {
        super(message, cause);
    }
}
