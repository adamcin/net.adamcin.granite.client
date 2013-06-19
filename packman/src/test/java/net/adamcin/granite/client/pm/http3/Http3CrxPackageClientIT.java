package net.adamcin.granite.client.pm.http3;

import net.adamcin.granite.client.pm.AbstractCrxPackageClient;
import net.adamcin.granite.client.pm.AbstractCrxPackageClientITBase;

public class Http3CrxPackageClientIT extends AbstractCrxPackageClientITBase {

    @Override
    protected AbstractCrxPackageClient getClientImplementation() {
        return new Http3CrxPackageClient();
    }
}
