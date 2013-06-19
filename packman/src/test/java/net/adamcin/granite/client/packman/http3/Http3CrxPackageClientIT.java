package net.adamcin.granite.client.packman.http3;

import net.adamcin.granite.client.packman.AbstractCrxPackageClient;
import net.adamcin.granite.client.packman.AbstractCrxPackageClientITBase;

public class Http3CrxPackageClientIT extends AbstractCrxPackageClientITBase {

    @Override
    protected AbstractCrxPackageClient getClientImplementation() {
        return new Http3CrxPackageClient();
    }
}
