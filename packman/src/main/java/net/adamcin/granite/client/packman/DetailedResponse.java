package net.adamcin.granite.client.packman;

import java.util.List;

/**
 * A more detailed {@link ServiceResponse} based on the HTML service representation
 */
public interface DetailedResponse extends ServiceResponse {

    /**
     * Server-side duration in milliseconds for a successful execution.
     * @return duration in milliseconds if successful, {@code -1L} if not successful or not provided.
     */
    long getDuration();

    /**
     * Convenience method indicating that the operation did not complete perfectly
     * @return true if not successful or if progress errors were recorded
     */
    boolean hasErrors();

    /**
     * Lists the progress errors. Does not include a failure message.
     * @return List of progress errors, which may be empty, but never null.
     */
    List<String> getProgressErrors();
}
