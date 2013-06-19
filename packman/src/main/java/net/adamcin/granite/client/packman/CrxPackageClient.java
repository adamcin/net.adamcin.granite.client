package net.adamcin.granite.client.packman;

import java.io.File;
import java.io.IOException;

/**
 * This is the Public API for a CRX Package Manager Console client. It is intended to be used for implementation of
 * higher level deployment management workflows, and therefore it does not expose any connection details.
 */
public interface CrxPackageClient {

    /**
     * Identify a CRX package based on its metadata
     * @param file a {@link File} representing the package
     * @return a {@link PackId} object if the file represents a package, or {@code null} otherwise
     * @throws IOException if the file can not be read, or it is not a zip file
     */
    PackId identify(File file) throws IOException;

    /**
     * Wait for service availability. Use this method between installing a package and any calling any other POST-based
     * service operation
     * @param serviceTimeout the amount of time to wait for service availability
     * @throws Exception on timeout, interruption, or IOException
     */
    void waitForService(long serviceTimeout) throws Exception;

    /**
     * Checks if a package with the specified packageId has already been uploaded to the server. This does not indicate
     * whether the package has already been installed.
     * @param packageId
     * @return {@code true} if a package exists, {@code false} otherwise
     * @throws Exception
     */
    boolean existsOnServer(PackId packageId) throws Exception;

    /**
     * Upload a package to the server. Does not install the package once uploaded.
     * @param file the package file to be uploaded
     * @param force set to {@code true} for the uploaded file to replace an existing package on the server that has the
     *              same id. If {@code false}
     * @param packageId optional {@link PackId} providing the installation path. If {@code null}, the {@code file} will
     *                  be identified and that {@link PackId} will be used
     * @return standard simple service response
     * @throws Exception
     */
    SimpleResponse upload(File file, boolean force, PackId packageId) throws Exception;

    /**
     * Delete a package from the server. Does not uninstall the package.
     * @param packageId {@link PackId} representing package to be deleted
     * @return standard simple service response
     * @throws Exception
     */
    SimpleResponse delete(PackId packageId) throws Exception;

    /**
     * Replicates the package using the server's default replication agents
     * @param packageId {@link PackId} representing package to be replicated
     * @return simple service response
     * @throws Exception
     */
    SimpleResponse replicate(PackId packageId) throws Exception;

    DetailedResponse contents(PackId packageId) throws Exception;

    DetailedResponse contents(PackId packageId, ResponseProgressListener listener) throws Exception;

    /**
     * Install a package that has already been uploaded to the server.
     * @param packageId {@link PackId} representing package to be installed
     * @param recursive set to {@code true} to also install subpackages
     * @param autosave number of changes between session saves.
     * @param acHandling Access Control Handling value {@link ACHandling}. Unspecified if {@code null}.
     * @return detailed service response
     * @throws Exception
     */
    DetailedResponse install(PackId packageId, boolean recursive, int autosave, ACHandling acHandling) throws Exception;

     /**
     * Install a package that has already been uploaded to the server.
     * @param packageId {@link PackId} representing package to be installed
     * @param recursive set to {@code true} to also install subpackages
     * @param autosave number of changes between session saves.
     * @param acHandling Access Control Handling value {@link ACHandling}. Unspecified if {@code null}.
     * @param listener response progress listener
     * @return detailed service response
     * @throws Exception
     */
    DetailedResponse install(PackId packageId, boolean recursive, int autosave,
                 ACHandling acHandling, ResponseProgressListener listener) throws Exception;

    /**
     * Performs a dryRun of an installation of the specified package
     * @param packageId
     * @return
     * @throws Exception
     */
    DetailedResponse dryRun(PackId packageId) throws Exception;

    /**
     *
     * @param packageId
     * @param listener
     * @return
     * @throws Exception
     */
    DetailedResponse dryRun(PackId packageId, ResponseProgressListener listener) throws Exception;

    DetailedResponse build(PackId packageId) throws Exception;

    DetailedResponse build(PackId packageId, ResponseProgressListener listener) throws Exception;

    DetailedResponse rewrap(PackId packageId) throws Exception;

    DetailedResponse rewrap(PackId packageId, ResponseProgressListener listener) throws Exception;

    DetailedResponse uninstall(PackId packageId) throws Exception;

    DetailedResponse uninstall(PackId packageId, ResponseProgressListener listener) throws Exception;
}
