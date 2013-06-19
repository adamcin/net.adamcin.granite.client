package net.adamcin.granite.client.pm;

/**
 * The basic service response interface.
 */
public interface ServiceResponse {
    boolean isSuccess();
    String getMessage();
}
