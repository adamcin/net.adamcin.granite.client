package net.adamcin.granite.client.packman;

import java.io.File;
import java.io.IOException;

public interface ShareClient {

    boolean login(String adobeId, String password) throws IOException;

    Iterable<ShareListResult> list(ShareListCriteria criteria) throws Exception;

    void download(ShareListResult result, File downloadToFile) throws Exception;
}
