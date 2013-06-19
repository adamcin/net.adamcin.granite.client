package net.adamcin.granite.client.packman.http4;

import net.adamcin.granite.client.packman.AbstractCrxPackageClient;
import net.adamcin.granite.client.packman.AbstractCrxPackageClientITBase;

public class Http4CrxPackageClientIT extends AbstractCrxPackageClientITBase {

    @Override
    protected AbstractCrxPackageClient getClientImplementation() {
        return new Http4CrxPackageClient();
    }
}
