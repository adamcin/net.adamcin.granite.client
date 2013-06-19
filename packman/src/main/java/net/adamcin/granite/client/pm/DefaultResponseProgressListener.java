package net.adamcin.granite.client.pm;

/**
 * Default do-nothing implementation of {@link ResponseProgressListener}
 */
public class DefaultResponseProgressListener implements ResponseProgressListener {

    /**
     * {@inheritDoc}
     */
    @Override public void onStart(String title) { }

    /**
     * {@inheritDoc}
     */
    @Override public void onLog(String message) { }

    /**
     * {@inheritDoc}
     */
    @Override public void onMessage(String message) { }

    /**
     * {@inheritDoc}
     */
    @Override public void onProgress(String action, String path) { }

    /**
     * {@inheritDoc}
     */
    @Override public void onError(String path, String error) { }
}
