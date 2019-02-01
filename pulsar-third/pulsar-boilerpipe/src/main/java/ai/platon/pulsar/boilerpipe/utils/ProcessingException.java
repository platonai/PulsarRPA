package ai.platon.pulsar.boilerpipe.utils;

/**
 * Exception for signaling failure in the processing pipeline.
 */
public class ProcessingException extends Exception {
  private static final long serialVersionUID = 1L;

  public ProcessingException() {
    super();
  }

  public ProcessingException(String message, Throwable cause) {
    super(message, cause);
  }

  public ProcessingException(String message) {
    super(message);
  }

  public ProcessingException(Throwable cause) {
    super(cause);
  }
}
