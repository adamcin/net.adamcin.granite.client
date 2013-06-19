package net.adamcin.granite.client.pm.async;

import net.adamcin.granite.client.pm.AbstractCrxPackageClient;
import net.adamcin.granite.client.pm.AbstractCrxPackageClientITBase;

public class AsyncCrxPackageClientIT
        extends AbstractCrxPackageClientITBase
{

    @Override
    protected AbstractCrxPackageClient getClientImplementation() {
        return new AsyncCrxPackageClient();
    }
}
