package net.adamcin.granite.client.pm.http4;

import net.adamcin.granite.client.pm.AbstractCrxPackageClient;
import net.adamcin.granite.client.pm.AbstractCrxPackageClientITBase;

public class Http4CrxPackageClientIT extends AbstractCrxPackageClientITBase {

    @Override
    protected AbstractCrxPackageClient getClientImplementation() {
        return new Http4CrxPackageClient();
    }
}
