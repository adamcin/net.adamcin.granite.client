package net.adamcin.granite.client.packman;

/**
 * A simple {@link ServiceResponse} interface based on the service's JSON representation
 */
public interface SimpleResponse extends ServiceResponse {

    /**
     * The path associated with the operation
     * @return a path if specified in the response, or an empty string otherwise.
     */
    String getPath();
}
