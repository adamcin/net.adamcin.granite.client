package net.adamcin.granite.client.pm;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The AbstractCrxPackageClient provides constants and concrete implementations for generic method logic and response
 * handling.
 */
public abstract class AbstractCrxPackageClient implements CrxPackageClient {
    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractCrxPackageClient.class);

    public static final ResponseProgressListener DEFAULT_LISTENER = new DefaultResponseProgressListener();

    public static final String SERVICE_BASE_PATH = "/crx/packmgr/service";
    public static final String HTML_SERVICE_PATH = SERVICE_BASE_PATH + "/console.html";
    public static final String JSON_SERVICE_PATH = SERVICE_BASE_PATH + "/exec.json";
    public static final String DEFAULT_BASE_URL = "http://localhost:4502";
    public static final String DEFAULT_USERNAME = "admin";
    public static final String DEFAULT_PASSWORD = "admin";
    public static final int MIN_AUTOSAVE = 1024;

    public static final String MIME_ZIP = "application/zip";

    public static final String KEY_CMD = "cmd";
    public static final String KEY_FORCE = "force";
    public static final String KEY_PACKAGE = "package";
    public static final String KEY_SUCCESS = "success";
    public static final String KEY_MESSAGE = "msg";
    public static final String KEY_PATH = "path";
    public static final String KEY_RECURSIVE = "recursive";
    public static final String KEY_AUTOSAVE = "autosave";
    public static final String KEY_ACHANDLING = "acHandling";

    public static final String CMD_CONTENTS = "contents";
    public static final String CMD_INSTALL = "install";
    public static final String CMD_UNINSTALL = "uninstall";
    public static final String CMD_UPLOAD = "upload";
    public static final String CMD_BUILD = "build";
    public static final String CMD_REWRAP = "rewrap";
    public static final String CMD_DRY_RUN = "dryrun";
    public static final String CMD_DELETE = "delete";
    public static final String CMD_REPLICATE = "replicate";

    private static final Pattern PATTERN_TITLE = Pattern.compile("^<body><h2>([^<]*)</h2>");
    private static final Pattern PATTERN_LOG = Pattern.compile("^([^<]*<br>)+");
    private static final Pattern PATTERN_MESSAGE = Pattern.compile("<span class=\"([^\"]*)\"><b>([^<]*)</b>&nbsp;([^<(]*)(\\([^)]*\\))?</span>");
    private static final Pattern PATTERN_SUCCESS = Pattern.compile("^</div><br>(.*) in (\\d+)ms\\.<br>");

    private String baseUrl = DEFAULT_BASE_URL;

    public void setBaseUrl(String baseUrl) {
        if (baseUrl == null) {
            throw new NullPointerException("baseUrl");
        }

        String _baseUrl = baseUrl;
        while (_baseUrl.endsWith("/")) {
            _baseUrl = _baseUrl.substring(0, _baseUrl.length() - 1);
        }
        this.baseUrl = _baseUrl;
    }

    public final String getBaseUrl() {
        return this.baseUrl;
    }

    protected final String getHtmlUrl() {
        return getBaseUrl() + HTML_SERVICE_PATH;
    }

    protected final String getHtmlUrl(PackId packageId) {
        if (packageId == null) {
            throw new NullPointerException("packageId");
        }
        return getHtmlUrl() + packageId.getInstallationPath() + ".zip";
    }

    protected final String getJsonUrl() {
        return getBaseUrl() + JSON_SERVICE_PATH;
    }

    protected final String getJsonUrl(PackId packageId) {
        if (packageId == null) {
            throw new NullPointerException("packageId");
        }
        return getJsonUrl() + packageId.getInstallationPath() + ".zip";
    }

    /**
     * The CRX PackageManagerServlet does not support GET requests. The only use for GET is to check service
     * availability. If anything other than 405 is returned, the service should be considered unavailable.
     * @param checkTimeout set to true to enforce a timeout
     * @param timeoutRemaining remaining timeout in milliseconds
     * @return either a throwable or a boolean
     */
    protected abstract Either<? extends Exception, Boolean> checkServiceAvailability(boolean checkTimeout, long timeoutRemaining);

    protected abstract ResponseBuilder getResponseBuilder();

    private static boolean handleStart(String line, ResponseProgressListener listener) {
        if (line.startsWith("<body>")) {
            Matcher titleMatcher = PATTERN_TITLE.matcher(line);
            if (titleMatcher.find()) {
                listener.onStart(titleMatcher.group(1));
                return true;
            }
        }
        return false;
    }

    private static DetailedResponse handleSuccess(String line, List<String> progressErrors) {
        if (line.startsWith("</div>")) {
            String message = "";
            long duration = -1L;
            Matcher successMatcher = PATTERN_SUCCESS.matcher(line);
            if (successMatcher.find()) {
                message = successMatcher.group(1);
                try {
                    duration = Long.valueOf(successMatcher.group(2));
                } catch (Exception e) { }
                return new DetailedResponseImpl(true, message, duration, progressErrors);
            }
        }
        return null;
    }

    private static DetailedResponse handleFailure(String line, StringBuilder failureBuilder, List<String> progressErrors) {
        if (line.startsWith("</pre>")) {
            return new DetailedResponseImpl(false, failureBuilder.toString().trim(), -1, progressErrors);
        } else {
            // assume line is part of stack trace
            failureBuilder.append(line).append(File.separator);
        }
        return null;
    }

    private static void handleLogs(String line, ResponseProgressListener listener) {
        if (!line.startsWith("<span")) {
            Matcher logMatcher = PATTERN_LOG.matcher(line);
            if (logMatcher.find()) {
                String logs = logMatcher.group(1);
                for (String log : logs.split("<br>")) {
                    if (!log.isEmpty()) {
                        listener.onLog(log);
                    }
                }
            }
        }
    }

    private static void handleMessage(String line, List<String> progressErrors, ResponseProgressListener listener) {
        Matcher messageMatcher = PATTERN_MESSAGE.matcher(line);
        if (messageMatcher.find()) {
            String action = messageMatcher.group(1);
            String path = messageMatcher.group(3);
            String error = messageMatcher.group(4);
            if ("E".equals(action)) {
                progressErrors.add(path + " " + error);
                listener.onError(path.trim(), error.substring(1, error.length()-1));
            } else if (action.length() == 1) {
                listener.onProgress(action, path.trim());
            } else {
                listener.onMessage(action);
            }
        }
    }

    private static boolean handleBeginFailure(String line) {
        return line.endsWith("<span class=\"error\">Error during processing.</span><br><code><pre>");
    }

    protected static DetailedResponse parseDetailedResponse(final int statusCode,
                                                                  final String statusText,
                                                                  final InputStream stream,
                                                                  final String charset,
                                                                  final ResponseProgressListener listener)
        throws IOException {

        if (statusCode == 400) {
            throw new IOException("Command not supported by service");
        } else if (statusCode / 100 != 2) {
            throw new IOException(Integer.toString(statusCode) + " " + statusText);
        } else {
            final ResponseProgressListener _listener = listener == null ? DEFAULT_LISTENER : listener;

            BufferedReader reader = null;
            try {
                reader = new BufferedReader(new InputStreamReader(stream, charset));
                boolean isFailure = false;
                boolean isStarted = false;
                final StringBuilder failureBuilder = new StringBuilder();
                final List<String> progressErrors = new ArrayList<String>();

                String line;
                while ((line = reader.readLine()) != null) {
                    if (isFailure) {

                        // handle failure end line
                        DetailedResponse response = handleFailure(line, failureBuilder, progressErrors);
                        if (response != null) {
                            return response;
                        }

                    } else {

                        if (!isStarted) {
                            // handle title line
                            if (handleStart(line, _listener)) {
                                isStarted = true;
                            }
                        }

                        if (isStarted) {
                            // handle success line
                            DetailedResponse response = handleSuccess(line, progressErrors);
                            if (response != null) {
                                return response;
                            }

                            // handle log statements
                            handleLogs(line, _listener);

                            // handle progress message
                            handleMessage(line, progressErrors, _listener);

                            if (handleBeginFailure(line)) {
                                isFailure = true;
                            }
                        }
                    }
                }

                // throw an exception if neither success or failure was returned
                throw new IOException("Failed to parse service response");

            } finally {
                if (reader != null) {
                    reader.close();
                }
            }
        }
    }

    protected static SimpleResponse parseSimpleResponse(final int statusCode,
                                                       final String statusText,
                                                       final InputStream stream,
                                                       final String charset)
            throws IOException {
        if (statusCode == 400) {
            throw new IOException("Command not supported by service");
        } else if (statusCode / 100 != 2) {
            throw new IOException(Integer.toString(statusCode) + " " + statusText);
        } else {
            try {
                JSONTokener tokener = new JSONTokener(new InputStreamReader(stream, charset));
                final JSONObject json = new JSONObject(tokener);

                final boolean success = json.has(KEY_SUCCESS) && json.getBoolean(KEY_SUCCESS);
                final String message = json.has(KEY_MESSAGE) ? json.getString(KEY_MESSAGE) : "";
                final String path = json.has(KEY_PATH) ? json.getString(KEY_PATH) : "";

                return new SimpleResponseImpl(success, message, path);
            } catch (JSONException e) {
                throw new IOException("Exception encountered while parsing response.", e);
            }
        }
    }

    protected static abstract class Either<T, U> {
        abstract boolean isLeft();
        T getLeft() { return null; }
        U getRight() { return null; }
    }

    static final class Left<T, U> extends Either<T, U> {
        final T left;

        private Left(final T left) {
            if (left == null) {
                throw new NullPointerException("left");
            }
            this.left = left;
        }

        @Override boolean isLeft() { return true; }
        @Override T getLeft() { return left; }
    }

    static final class Right<T, U> extends Either<T, U> {
        final U right;

        private Right(final U right) {
            if (right == null) {
                throw new NullPointerException("right");
            }
            this.right = right;
        }

        @Override boolean isLeft() { return false; }
        @Override U getRight() { return right; }
    }

    protected static <T, U> Either<T, U> left(T left, Class<U> right) {
        return new Left<T, U>(left);
    }

    protected static <T, U> Either<T, U> right(Class<T> left, U right) {
        return new Right<T, U>(right);
    }

    static class DetailedResponseImpl implements DetailedResponse {
        final boolean success;
        final String message;
        final long duration;
        final List<String> progressErrors;

        DetailedResponseImpl(boolean success, String message, long duration, List<String> progressErrors) {
            this.success = success;
            this.message = message;
            this.duration = duration;
            List<String> _progressErrors = progressErrors == null ? new ArrayList<String>() : progressErrors;
            this.progressErrors = Collections.unmodifiableList(_progressErrors);
        }

        @Override public long getDuration() {
            return duration;
        }

        @Override
        public boolean hasErrors() {
            return !success || (progressErrors != null && !progressErrors.isEmpty());
        }

        @Override public List<String> getProgressErrors() {
            return progressErrors;
        }

        @Override public boolean isSuccess() {
            return success;
        }

        @Override public String getMessage() {
            return message;
        }

        @Override public String toString() {
            return "{success:" + success +
                    ", msg:\"" + message +
                    "\", duration:\"" + duration +
                    "\", hasErrors:" + !progressErrors.isEmpty() + "}";
        }
    }

    static class SimpleResponseImpl implements SimpleResponse {
        final boolean success;
        final String message;
        final String path;

        SimpleResponseImpl(boolean success, String message, String path) {
            this.success = success;
            this.message = message;
            this.path = path == null ? "" : path;
        }

        @Override public boolean isSuccess() {
            return success;
        }

        @Override public String getMessage() {
            return message;
        }

        @Override public String getPath() {
            return path;
        }

        @Override public String toString() {
            return "{success:" + success + ", msg:\"" + message + "\", path:\"" + path + "\"}";
        }
    }

    protected static abstract class ResponseBuilder {
        protected abstract ResponseBuilder forPackId(PackId packId);
        protected abstract ResponseBuilder withParam(String name, String value);
        protected abstract ResponseBuilder withParam(String name, boolean value);
        protected abstract ResponseBuilder withParam(String name, int value);
        protected abstract ResponseBuilder withParam(String name, File value, String mimeType) throws IOException;
        protected abstract SimpleResponse getSimpleResponse() throws Exception;
        protected abstract DetailedResponse getDetailedResponse(ResponseProgressListener listener) throws Exception;
    }

    //-------------------------------------------------------------------------
    // CrxPackageClient method implementations
    //-------------------------------------------------------------------------

    /**
     * {@inheritDoc}
     */
    @Override public PackId identify(File file) throws IOException {
        return PackId.identifyPackage(file);
    }

    /**
     * {@inheritDoc}
     */
    @Override public final void waitForService(final long serviceTimeout) throws Exception {
        boolean checkTimeout = serviceTimeout >= 0L;
        int tries = 0;
        final long stop = System.currentTimeMillis() + serviceTimeout;
        Either<? extends Exception, Boolean> resp;
        do {
            if (checkTimeout && stop <= System.currentTimeMillis()) {
                throw new IOException("Service timeout exceeded.");
            }
            Thread.sleep(Math.min(5, tries) * 1000L);
            resp = checkServiceAvailability(checkTimeout, stop - System.currentTimeMillis());
            if (resp.isLeft()) {
                throw resp.getLeft();
            }
            tries++;
        } while (!resp.isLeft() && !resp.getRight());
    }

    /**
     * {@inheritDoc}
     */
    @Override public final boolean existsOnServer(PackId packageId) throws Exception {
        if (packageId == null) {
            throw new NullPointerException("packageId");
        }
        return getResponseBuilder().forPackId(packageId)
                .withParam(KEY_CMD, CMD_CONTENTS).getSimpleResponse().isSuccess();
    }

    /**
     * {@inheritDoc}
     */
    @Override public final SimpleResponse upload(File file, boolean force, PackId packageId) throws Exception {
        if (file == null) {
            throw new NullPointerException("file");
        }
        return getResponseBuilder().forPackId(packageId == null ? identify(file) : packageId)
                .withParam(KEY_CMD, CMD_UPLOAD)
                .withParam(KEY_PACKAGE, file, MIME_ZIP)
                .withParam(KEY_FORCE, force)
                .getSimpleResponse();
    }

    /**
     * {@inheritDoc}
     */
    @Override public final SimpleResponse delete(PackId packageId) throws Exception {
        if (packageId == null) {
            throw new NullPointerException("packageId");
        }
        return getResponseBuilder().forPackId(packageId)
                .withParam(KEY_CMD, CMD_DELETE)
                .getSimpleResponse();
    }

    /**
     * {@inheritDoc}
     */
    @Override public final SimpleResponse replicate(PackId packageId) throws Exception {
        if (packageId == null) {
            throw new NullPointerException("packageId");
        }
        return getResponseBuilder().forPackId(packageId)
                .withParam(KEY_CMD, CMD_REPLICATE)
                .getSimpleResponse();
    }

    /**
     * {@inheritDoc}
     */
    @Override public final DetailedResponse contents(PackId packageId) throws Exception {
        return this.contents(packageId, null);
    }

    /**
     * {@inheritDoc}
     */
    @Override public final DetailedResponse contents(PackId packageId, ResponseProgressListener listener) throws Exception {
        if (packageId == null) {
            throw new NullPointerException("packageId");
        }
        return getResponseBuilder().forPackId(packageId)
                .withParam(KEY_CMD, CMD_CONTENTS)
                .getDetailedResponse(listener);
    }

    /**
     * {@inheritDoc}
     */
    @Override public final DetailedResponse install(PackId packageId,
                                          boolean recursive,
                                          int autosave,
                                          ACHandling acHandling) throws Exception {
        return this.install(packageId, recursive, autosave, acHandling, null);
    }

    /**
     * {@inheritDoc}
     */
    @Override public final DetailedResponse install(PackId packageId,
                                          boolean recursive,
                                          int autosave,
                                          ACHandling acHandling,
                                          ResponseProgressListener listener) throws Exception {
        if (packageId == null) {
            throw new NullPointerException("packageId");
        }

        ResponseBuilder rb = getResponseBuilder().forPackId(packageId)
                .withParam(KEY_CMD, CMD_INSTALL)
                .withParam(KEY_RECURSIVE, recursive)
                .withParam(KEY_AUTOSAVE, Math.max(autosave, MIN_AUTOSAVE));

        if (acHandling != null) {
            rb.withParam(KEY_ACHANDLING, acHandling.name().toLowerCase());
        }

        return rb.getDetailedResponse(listener);
    }

    /**
     * {@inheritDoc}
     */
    @Override public final DetailedResponse dryRun(PackId packageId) throws Exception {
        return this.dryRun(packageId, null);
    }

    /**
     * {@inheritDoc}
     */
    @Override public final DetailedResponse dryRun(PackId packageId, ResponseProgressListener listener) throws Exception {
        if (packageId == null) {
            throw new NullPointerException("packageId");
        }

        return getResponseBuilder().forPackId(packageId)
                .withParam(KEY_CMD, CMD_DRY_RUN)
                .getDetailedResponse(listener);
    }

    /**
     * {@inheritDoc}
     */
    @Override public final DetailedResponse build(PackId packageId) throws Exception {
        return this.build(packageId, null);
    }

    /**
     * {@inheritDoc}
     */
    @Override public final DetailedResponse build(PackId packageId, ResponseProgressListener listener) throws Exception {
        if (packageId == null) {
            throw new NullPointerException("packageId");
        }

        return getResponseBuilder().forPackId(packageId)
                .withParam(KEY_CMD, CMD_BUILD)
                .getDetailedResponse(listener);
    }

    /**
     * {@inheritDoc}
     */
    @Override public final DetailedResponse rewrap(PackId packageId) throws Exception {
        return this.rewrap(packageId, null);
    }

    /**
     * {@inheritDoc}
     */
    @Override public final DetailedResponse rewrap(PackId packageId, ResponseProgressListener listener) throws Exception {
        if (packageId == null) {
            throw new NullPointerException("packageId");
        }

        return getResponseBuilder().forPackId(packageId)
                .withParam(KEY_CMD, CMD_REWRAP)
                .getDetailedResponse(listener);
    }

    /**
     * {@inheritDoc}
     */
    @Override public final DetailedResponse uninstall(PackId packageId) throws Exception {
        return this.uninstall(packageId, null);
    }

    /**
     * {@inheritDoc}
     */
    @Override public final DetailedResponse uninstall(PackId packageId, ResponseProgressListener listener) throws Exception {
        if (packageId == null) {
            throw new NullPointerException("packageId");
        }

        return getResponseBuilder().forPackId(packageId)
                .withParam(KEY_CMD, CMD_UNINSTALL)
                .getDetailedResponse(listener);
    }
}
