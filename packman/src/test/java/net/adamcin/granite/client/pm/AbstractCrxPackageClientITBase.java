package net.adamcin.granite.client.pm;

import net.adamcin.commons.testing.junit.TestBody;
import net.adamcin.granite.client.pm.AbstractCrxPackageClient;
import net.adamcin.granite.client.pm.PackId;
import org.apache.commons.io.IOUtils;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;

import static org.junit.Assert.*;

public abstract class AbstractCrxPackageClientITBase {
    public final Logger LOGGER = LoggerFactory.getLogger(getClass());

    protected abstract AbstractCrxPackageClient getClientImplementation();

    public void generateTestPackage(File packageFile) throws IOException {
        InputStream testPack = null;
        OutputStream identOs = null;
        try {
            testPack = getClass().getResourceAsStream("/test-packmgr-client-1.0.zip");
            if (packageFile.getParentFile().isDirectory()
                    || packageFile.getParentFile().mkdirs()) {

                identOs = new FileOutputStream(packageFile);
                IOUtils.copy(testPack, identOs);
            }
        } finally {
            IOUtils.closeQuietly(testPack);
            IOUtils.closeQuietly(identOs);
        }
    }

    @Test
    public void testIdentifyPackage() {
        TestBody.test(new PackmgrClientTestBody() {
            @Override protected void execute() throws Exception {
                File nonExist = new File("target/non-exist-package.zip");
                boolean fileNotFoundThrown = false;

                try {
                    client.identify(nonExist);
                } catch (FileNotFoundException e) {
                    fileNotFoundThrown = true;
                }

                assertTrue("identify throws correct exception for non-existent file", fileNotFoundThrown);

                File identifiable = new File("target/identifiable-package.zip");
                generateTestPackage(identifiable);

                PackId id = client.identify(identifiable);

                assertNotNull("id should not be null", id);

                assertEquals("group should be test-packmgr", "test-packmgr", id.getGroup());
                assertEquals("name should be test-packmgr-client", "test-packmgr-client", id.getName());
                assertEquals("version should be 1.0", "1.0", id.getVersion());
                assertEquals("installationPath should be /etc/packages/test-packmgr/test-packmgr-client-1.0", "/etc/packages/test-packmgr/test-packmgr-client-1.0", id.getInstallationPath());
            }
        });
    }

    @Test
    public void testWaitForService() {
        TestBody.test(new PackmgrClientTestBody() {
            @Override protected void execute() throws Exception {
                boolean ex = false;
                try {
                    client.waitForService(5000);
                } catch (Exception e) {
                    LOGGER.debug("Exception: " + e.getMessage());
                    ex = true;
                }

                assertFalse("Exception should not be thrown for baseUrl: " + client.getBaseUrl(), ex);

                ex = false;
                client.setBaseUrl("http://www.google.com");

                long stop = System.currentTimeMillis() + 5000L;
                try {
                    client.waitForService(5000L);
                } catch (Exception e) {
                    LOGGER.debug("Exception: " + e.getMessage());
                    ex = true;
                    assertTrue("Waited long enough", System.currentTimeMillis() > stop);
                }

                assertTrue("Exception should be thrown for baseUrl: " + client.getBaseUrl(), ex);
            }
        });
    }

    @Test
    public void testExistsOnServer() {
        TestBody.test(new PackmgrClientTestBody() {
            @Override protected void execute() throws Exception {
                File file = new File("target/test-packmgr-client-1.0.zip");

                generateTestPackage(file);

                PackId id = client.identify(file);
                if (client.existsOnServer(id)) {
                    LOGGER.info("deleting: {}", client.delete(id));
                }

                assertFalse("package should not exist on server", client.existsOnServer(id));

                LOGGER.info("uploading: {}", client.upload(file, true, id));

                assertTrue("package should exist on server", client.existsOnServer(id));

                LOGGER.info("deleting: {}", client.delete(id));

                assertFalse("package should not exist on server", client.existsOnServer(id));
            }
        });
    }

    abstract class PackmgrClientTestBody extends TestBody {
        AbstractCrxPackageClient client = getClientImplementation();
    }
}
