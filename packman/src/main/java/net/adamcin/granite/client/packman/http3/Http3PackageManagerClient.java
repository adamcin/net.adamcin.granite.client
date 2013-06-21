package net.adamcin.granite.client.packman.http3;

import net.adamcin.granite.client.packman.AbstractPackageManagerClient;
import net.adamcin.granite.client.packman.DetailedResponse;
import net.adamcin.granite.client.packman.PackId;
import net.adamcin.granite.client.packman.ResponseProgressListener;
import net.adamcin.granite.client.packman.SimpleResponse;
import net.adamcin.sshkey.commons.Signer;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethodBase;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.multipart.FilePart;
import org.apache.commons.httpclient.methods.multipart.MultipartRequestEntity;
import org.apache.commons.httpclient.methods.multipart.Part;
import org.apache.commons.httpclient.methods.multipart.StringPart;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class Http3PackageManagerClient extends AbstractPackageManagerClient {
    private static final Logger LOGGER = LoggerFactory.getLogger(Http3PackageManagerClient.class);

    public static final UsernamePasswordCredentials DEFAULT_CREDENTIALS =
            new UsernamePasswordCredentials(DEFAULT_USERNAME, DEFAULT_PASSWORD);

    private final HttpClient client;

    public Http3PackageManagerClient() {
        this(new HttpClient());
        getClient().getParams().setAuthenticationPreemptive(true);
        getClient().getState().setCredentials(AuthScope.ANY, DEFAULT_CREDENTIALS);
    }

    public Http3PackageManagerClient(final HttpClient client) {
        this.client = client;
    }

    public HttpClient getClient() {
        return this.client;
    }

    @Override
    protected Either<? extends Exception, Boolean> checkServiceAvailability(final boolean checkTimeout,
                                                                            final long timeoutRemaining) {

        final GetMethod request = new GetMethod(getJsonUrl());
        final int oldTimeout = getClient().getHttpConnectionManager().getParams().getConnectionTimeout();
        if (checkTimeout) {
            getClient().getHttpConnectionManager().getParams().setConnectionTimeout((int) timeoutRemaining);
            request.getParams().setSoTimeout((int) timeoutRemaining);
        }

        try {
            int status = getClient().executeMethod(request);

            if (status == 401) {
                throw new IOException("401 Unauthorized");
            } else {
                return right(Exception.class, status == 405);
            }
        } catch (IOException e) {
            return left(e, Boolean.class);
        } finally {
            request.releaseConnection();
            if (checkTimeout) {
                getClient().getHttpConnectionManager().getParams().setConnectionTimeout(oldTimeout);
            }
        }
    }

    public void setBasicCredentials(String username, String password) {
        getClient().getState().setCredentials(AuthScope.ANY,
                new UsernamePasswordCredentials(username, password));
    }

    @Override
    public boolean login(String username, String password) throws IOException {
        return false;
    }

    @Override
    public boolean login(String username, Signer signer) throws IOException {
        return false;
    }

    private SimpleResponse executeSimpleRequest(final HttpMethodBase request) throws IOException {
        int status = getClient().executeMethod(request);
        return parseSimpleResponse(status,
                request.getStatusText(),
                request.getResponseBodyAsStream(),
                request.getResponseCharSet());
    }

    private DetailedResponse executeDetailedRequest(final HttpMethodBase request, final ResponseProgressListener listener) throws IOException {
        int status = getClient().executeMethod(request);
        return parseDetailedResponse(status,
                request.getStatusText(),
                request.getResponseBodyAsStream(),
                request.getResponseCharSet(),
                listener);
    }

    @Override
    protected ResponseBuilder getResponseBuilder() {
        return new Http3ResponseBuilder();
    }

    class Http3ResponseBuilder extends ResponseBuilder {

        private PackId packId;
        private Map<String, StringPart> stringParams = new HashMap<String, StringPart>();
        private Map<String, FilePart> fileParams = new HashMap<String, FilePart>();

        @Override
        public ResponseBuilder forPackId(final PackId packId) {
            this.packId = packId;
            return this;
        }

        @Override
        public ResponseBuilder withParam(String name, String value) {
            this.stringParams.put(name, new StringPart(name, value));
            return this;
        }

        @Override
        public ResponseBuilder withParam(String name, boolean value) {
            return this.withParam(name, Boolean.toString(value));
        }

        @Override
        public ResponseBuilder withParam(String name, int value) {
            return this.withParam(name, Integer.toString(value));
        }

        @Override
        public ResponseBuilder withParam(String name, File value, String mimeType) throws IOException {
            this.fileParams.put(name, new FilePart(name, value, mimeType, null));
            return this;
        }

        @Override
        public SimpleResponse getSimpleResponse() throws Exception {
            PostMethod request = new PostMethod(getJsonUrl(this.packId));

            List<Part> parts = new ArrayList<Part>();

            for (Part part : this.stringParams.values()) {
                parts.add(part);
            }

            for (Part part : this.fileParams.values()) {
                parts.add(part);
            }

            request.setRequestEntity(new MultipartRequestEntity(parts.toArray(new Part[parts.size()]),
                    request.getParams()));

            try {
                return executeSimpleRequest(request);
            } finally {
                request.releaseConnection();
            }
        }

        @Override
        public DetailedResponse getDetailedResponse(final ResponseProgressListener listener) throws Exception {
            PostMethod request = new PostMethod(getHtmlUrl(this.packId));

            List<Part> parts = new ArrayList<Part>();

            for (Part part : this.stringParams.values()) {
                parts.add(part);
            }

            for (Part part : this.fileParams.values()) {
                parts.add(part);
            }

            request.setRequestEntity(new MultipartRequestEntity(parts.toArray(new Part[parts.size()]),
                    request.getParams()));

            try {
                return executeDetailedRequest(request, listener);
            } finally {
                request.releaseConnection();
            }

        }
    }
}
