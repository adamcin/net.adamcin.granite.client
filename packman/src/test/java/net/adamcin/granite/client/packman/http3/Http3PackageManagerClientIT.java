package net.adamcin.granite.client.packman.http3;

import net.adamcin.granite.client.packman.AbstractPackageManagerClient;
import net.adamcin.granite.client.packman.AbstractPackageManagerClientITBase;

public class Http3PackageManagerClientIT extends AbstractPackageManagerClientITBase {

    @Override
    protected AbstractPackageManagerClient getClientImplementation() {
        return new Http3PackageManagerClient();
    }
}
