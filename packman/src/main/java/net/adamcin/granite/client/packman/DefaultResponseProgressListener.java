package net.adamcin.granite.client.packman;

/**
 * Default do-nothing implementation of {@link ResponseProgressListener}
 */
public class DefaultResponseProgressListener implements ResponseProgressListener {

    /**
     * {@inheritDoc}
     */
    public void onStart(String title) { }

    /**
     * {@inheritDoc}
     */
    public void onLog(String message) { }

    /**
     * {@inheritDoc}
     */
    public void onMessage(String message) { }

    /**
     * {@inheritDoc}
     */
    public void onProgress(String action, String path) { }

    /**
     * {@inheritDoc}
     */
    public void onError(String path, String error) { }
}
