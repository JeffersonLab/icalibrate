package org.jlab.icalibrate.exception;

/**
 * An exception that arises during data gathering due to an FSD trip.
 * 
 * @author ryans
 */
public class FSDException extends AppException {
     /**
     * Create an exception with only a message.
     * 
     * @param message The message.
     */    
    public FSDException(String message) {
        super(message);
    }
    
    /**
     * Create an exception with a message and a cause.
     * 
     * @param message The message
     * @param cause The cause 
     */    
    public FSDException(String message, Throwable cause) {
        super(message, cause);
    }   
}
