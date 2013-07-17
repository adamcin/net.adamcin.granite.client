package net.adamcin.granite.client.packman.async;

import net.adamcin.granite.client.packman.AbstractPackageManagerClient;
import net.adamcin.granite.client.packman.AbstractPackageManagerClientITBase;

public class AsyncPackageManagerClientIT
        extends AbstractPackageManagerClientITBase
{

    @Override
    protected AbstractPackageManagerClient getClientImplementation() {
        AsyncPackageManagerClient client = new AsyncPackageManagerClient();
        return client;
    }
}
