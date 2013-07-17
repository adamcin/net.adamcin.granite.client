package net.adamcin.granite.client.packman;

/**
 * Created with IntelliJ IDEA.
 * User: madamcin
 * Date: 7/16/13
 * Time: 2:03 PM
 * To change this template use File | Settings | File Templates.
 */
public final class UnauthorizedException extends Exception {

    public UnauthorizedException() {
    }

    public UnauthorizedException(String message) {
        super(message);
    }
}
