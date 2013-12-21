package net.adamcin.granite.client.packman;

import java.io.File;

/**
 *
 */
public interface DownloadResponse {
    Long getLength();
    File getContent();
}
