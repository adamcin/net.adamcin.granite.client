package net.adamcin.granite.client.packman.async;

import net.adamcin.granite.client.packman.AbstractCrxPackageClient;
import net.adamcin.granite.client.packman.AbstractCrxPackageClientITBase;

public class AsyncCrxPackageClientIT
        extends AbstractCrxPackageClientITBase
{

    @Override
    protected AbstractCrxPackageClient getClientImplementation() {
        AsyncCrxPackageClient client = new AsyncCrxPackageClient();
        client.setRealm(AsyncCrxPackageClient.DEFAULT_REALM);
        return client;
    }
}
