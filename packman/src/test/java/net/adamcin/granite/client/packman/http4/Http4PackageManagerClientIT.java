package net.adamcin.granite.client.packman.http4;

import net.adamcin.granite.client.packman.AbstractPackageManagerClient;
import net.adamcin.granite.client.packman.AbstractPackageManagerClientITBase;

public class Http4PackageManagerClientIT extends AbstractPackageManagerClientITBase {

    @Override
    protected AbstractPackageManagerClient getClientImplementation() {
        return new Http4PackageManagerClient();
    }
}
