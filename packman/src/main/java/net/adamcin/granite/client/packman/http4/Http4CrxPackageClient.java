package net.adamcin.granite.client.packman.http4;

import net.adamcin.granite.client.packman.AbstractCrxPackageClient;
import net.adamcin.granite.client.packman.DetailedResponse;
import net.adamcin.granite.client.packman.PackId;
import net.adamcin.granite.client.packman.ResponseProgressListener;
import net.adamcin.granite.client.packman.SimpleResponse;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.AuthCache;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.protocol.ClientContext;
import org.apache.http.client.utils.URIUtils;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.AbstractHttpClient;
import org.apache.http.impl.client.BasicAuthCache;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

public final class Http4CrxPackageClient extends AbstractCrxPackageClient {
    private static final Logger LOGGER = LoggerFactory.getLogger(Http4CrxPackageClient.class);

    public static final UsernamePasswordCredentials DEFAULT_CREDENTIALS =
            new UsernamePasswordCredentials(DEFAULT_USERNAME, DEFAULT_PASSWORD);

    private static final ResponseHandler<SimpleResponse> SIMPLE_RESPONSE_HANDLER =
            new ResponseHandler<SimpleResponse>() {
                public SimpleResponse handleResponse(final HttpResponse response)
                        throws ClientProtocolException, IOException {
                    StatusLine statusLine = response.getStatusLine();
                    return parseSimpleResponse(
                            statusLine.getStatusCode(),
                            statusLine.getReasonPhrase(),
                            response.getEntity().getContent(),
                            getResponseEncoding(response));
                }
            };

    private static final ResponseHandler<HttpResponse> AUTHORIZED_RESPONSE_HANDLER =
            new ResponseHandler<HttpResponse>() {
                public HttpResponse handleResponse(final HttpResponse response)
                        throws ClientProtocolException, IOException {
                    if (response.getStatusLine().getStatusCode() == 401) {
                        throw new IOException("401 Unauthorized");
                    } else {
                        return response;
                    }
                }
            };

    private final AbstractHttpClient client;
    private HttpContext httpContext = new BasicHttpContext();
    private final AuthCache preemptAuthCache = new BasicAuthCache();

    public Http4CrxPackageClient() {
        this(new DefaultHttpClient());
        getClient().getCredentialsProvider().setCredentials(AuthScope.ANY, DEFAULT_CREDENTIALS);
        httpContext.setAttribute(ClientContext.AUTH_CACHE, preemptAuthCache);
        try {
            preemptAuthCache.put(URIUtils.extractHost(new URI(DEFAULT_BASE_URL)), new BasicScheme());
        } catch (URISyntaxException e) {
            // shouldn't happen since we are parsing a valid constant, right?
        }
    }

    public Http4CrxPackageClient(AbstractHttpClient client) {
        this.client = client;
    }

    public AbstractHttpClient getClient() {
        return client;
    }

    public HttpContext getHttpContext() {
        return httpContext;
    }

    public void setHttpContext(HttpContext httpContext) {
        this.httpContext = httpContext;
    }

    private static String getResponseEncoding(HttpResponse response) {
        Header encoding = response.getFirstHeader("Content-Encoding");

        if (encoding != null) {
            return encoding.getValue();
        } else {
            Header contentType = response.getFirstHeader("Content-Type");
            if (contentType != null) {
                String _contentType = contentType.getValue();
                int charsetBegin = _contentType.toLowerCase().indexOf(";charset=");
                if (charsetBegin >= 0) {
                    return _contentType.substring(charsetBegin + ";charset=".length());
                }
            }
        }

        return "UTF-8";
    }

    @Override
    public void setBaseUrl(String baseUrl) {
        super.setBaseUrl(baseUrl);
        try {
            this.preemptAuthCache.put(URIUtils.extractHost(new URI(baseUrl)), new BasicScheme());
        } catch (URISyntaxException e) {
            LOGGER.warn("[setBaseUrl] failed to parse URL for setup of preemptive authentication", e);
        }
    }

    public void setBasicCredentials(String username, String password) {
        getClient().getCredentialsProvider().setCredentials(AuthScope.ANY,
                new UsernamePasswordCredentials(username, password));
    }

    @Override
    protected Either<? extends Exception, Boolean> checkServiceAvailability(boolean checkTimeout,
                                                                            long timeoutRemaining) {
        HttpUriRequest request = new HttpGet(getJsonUrl());

        if (checkTimeout) {
            HttpConnectionParams.setConnectionTimeout(request.getParams(), (int) timeoutRemaining);
            HttpConnectionParams.setSoTimeout(request.getParams(), (int) timeoutRemaining);
        }

        try {
            HttpResponse response = getClient().execute(request, AUTHORIZED_RESPONSE_HANDLER, getHttpContext());
            return right(Exception.class, response.getStatusLine().getStatusCode() == 405);
        } catch (Exception e) {
            return left(e, Boolean.class);
        }
    }

    private SimpleResponse executeSimpleRequest(HttpUriRequest request) throws Exception {
        return getClient().execute(request, SIMPLE_RESPONSE_HANDLER, getHttpContext());
    }

    private DetailedResponse executeDetailedRequest(final HttpUriRequest request, final ResponseProgressListener listener) throws Exception {
        return getClient().execute(request, new ResponseHandler<DetailedResponse>() {
                public DetailedResponse handleResponse(final HttpResponse response)
                        throws ClientProtocolException, IOException {
                    StatusLine statusLine = response.getStatusLine();
                    return parseDetailedResponse(
                            statusLine.getStatusCode(),
                            statusLine.getReasonPhrase(),
                            response.getEntity().getContent(),
                            getResponseEncoding(response),
                            listener);
                }
            }, getHttpContext());
    }

    @Override
    protected ResponseBuilder getResponseBuilder() {
        return new Http4ResponseBuilder();
    }

    class Http4ResponseBuilder extends ResponseBuilder {

        private PackId packId;
        private Map<String, StringBody> stringParams = new HashMap<String, StringBody>();
        private Map<String, FileBody> fileParams = new HashMap<String, FileBody>();

        @Override
        public ResponseBuilder forPackId(final PackId packId) {
            this.packId = packId;
            return this;
        }

        @Override
        public ResponseBuilder withParam(String name, String value) {
            try {
                this.stringParams.put(name, new StringBody(value));
            } catch (UnsupportedEncodingException e) { /* shouldn't happen */ }
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
            this.fileParams.put(name, new FileBody(value, mimeType));
            return this;
        }

        @Override
        public SimpleResponse getSimpleResponse() throws Exception {
            HttpPost request = new HttpPost(getJsonUrl(this.packId));

            MultipartEntity entity = new MultipartEntity();

            for (Map.Entry<String, StringBody> param : this.stringParams.entrySet()) {
                entity.addPart(param.getKey(), param.getValue());
            }

            for (Map.Entry<String, FileBody> param : this.fileParams.entrySet()) {
                entity.addPart(param.getKey(), param.getValue());
            }

            request.setEntity(entity);

            return executeSimpleRequest(request);
        }

        @Override
        public DetailedResponse getDetailedResponse(final ResponseProgressListener listener) throws Exception {
            HttpPost request = new HttpPost(getHtmlUrl(this.packId));

            MultipartEntity entity = new MultipartEntity();

            for (Map.Entry<String, StringBody> param : this.stringParams.entrySet()) {
                entity.addPart(param.getKey(), param.getValue());
            }

            for (Map.Entry<String, FileBody> param : this.fileParams.entrySet()) {
                entity.addPart(param.getKey(), param.getValue());
            }

            request.setEntity(entity);

            return executeDetailedRequest(request, listener);
        }
    }
}
