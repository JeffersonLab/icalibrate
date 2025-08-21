package org.jlab.icalibrate.exception;

/**
 * An exception that arises during data loading due to unexpected or missing data.
 *
 * @author ryans
 */
public class MissingDataException extends AppException {

  /**
   * Create an exception with only a message.
   *
   * @param message The message.
   */
  public MissingDataException(String message) {
    super(message);
  }

  /**
   * Create an exception with a message and a cause.
   *
   * @param message The message
   * @param cause The cause
   */
  public MissingDataException(String message, Throwable cause) {
    super(message, cause);
  }
}
