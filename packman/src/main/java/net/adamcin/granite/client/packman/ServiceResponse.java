package net.adamcin.granite.client.packman;

/**
 * The basic service response interface.
 */
public interface ServiceResponse {
    boolean isSuccess();
    String getMessage();
}
