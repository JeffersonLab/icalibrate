package org.jlab.icalibrate.exception;

/**
 * The root of the application exception hierarchy.
 *
 * @author ryans
 */
public class AppException extends Exception {

    /**
     * Create an exception with only a message.
     *
     * @param message The message.
     */
    public AppException(String message) {
        super(message);
    }

    /**
     * Create an exception with a message and a cause.
     *
     * @param message The message
     * @param cause The cause
     */
    public AppException(String message, Throwable cause) {
        super(message, cause);
    }
}
