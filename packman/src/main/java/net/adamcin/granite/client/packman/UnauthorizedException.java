package net.adamcin.granite.client.packman;

import java.io.IOException;

/**
 * Created with IntelliJ IDEA.
 * User: madamcin
 * Date: 7/16/13
 * Time: 2:03 PM
 * To change this template use File | Settings | File Templates.
 */
public final class UnauthorizedException extends IOException {

    public UnauthorizedException() {
    }

    public UnauthorizedException(String message) {
        super(message);
    }
}
