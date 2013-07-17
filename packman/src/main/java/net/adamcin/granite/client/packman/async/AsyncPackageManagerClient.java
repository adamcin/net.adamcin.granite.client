package net.adamcin.granite.client.packman.async;

import com.ning.http.client.*;
import com.ning.http.multipart.FilePart;
import net.adamcin.granite.client.packman.AbstractPackageManagerClient;
import net.adamcin.granite.client.packman.DetailedResponse;
import net.adamcin.granite.client.packman.PackId;
import net.adamcin.granite.client.packman.ResponseProgressListener;
import net.adamcin.granite.client.packman.SimpleResponse;
import net.adamcin.sshkey.clientauth.async.AsyncUtil;
import net.adamcin.sshkey.api.Signer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public final class AsyncPackageManagerClient extends AbstractPackageManagerClient {
    private static final Logger LOGGER = LoggerFactory.getLogger(AsyncPackageManagerClient.class);

    private static final AsyncCompletionHandler<SimpleResponse> SIMPLE_RESPONSE_HANDLER =
            new AsyncCompletionHandler<SimpleResponse>() {
                @Override public SimpleResponse onCompleted(Response response) throws Exception {
                    return AbstractPackageManagerClient.parseSimpleResponse(
                            response.getStatusCode(),
                            response.getStatusText(),
                            response.getResponseBodyAsStream(),
                            getResponseEncoding(response)
                    );
                }
            };

    private final AsyncCompletionHandler<Response> AUTHORIZED_RESPONSE_HANDLER =
            new AuthorizedResponseHandler<Response>() {
                @Override protected Response onAuthorized(Response response) throws Exception {
                    return response;
                }
            };

    private final AsyncHttpClient client;

    private final List<Cookie> cookies = new ArrayList<Cookie>();

    public AsyncPackageManagerClient() {
        this(new AsyncHttpClient());
    }

    public AsyncPackageManagerClient(final AsyncHttpClient client) {
        if (client == null) {
            throw new NullPointerException("client cannot be null");
        }
        this.client = client;
    }

    public AsyncHttpClient getClient() {
        return this.client;
    }

    private void setCookies(Collection<Cookie> cookies) {
        this.cookies.clear();

        if (cookies != null) {
            this.cookies.addAll(cookies);
        }
    }

    @Override
    public boolean login(String username, String password) throws IOException {
        Request request = getClient().preparePost(getBaseUrl() + "/crx/j_security_check")
                .addParameter("j_username", username)
                .addParameter("j_password", password)
                .addParameter("j_validate", "true")
                .addParameter("_charset_", "utf-8").build();
        try {
            Response response = getClient().executeRequest(request).get(60000L, TimeUnit.MILLISECONDS);

            if (response.getStatusCode() == 200) {
                this.setCookies(response.getCookies());
                return true;
            }
        } catch (Exception e) {
            throw new IOException("Failed to login with provided credentials");
        }

        return false;
    }

    @Override
    public boolean login(String username, Signer signer) throws IOException {

        Response response = AsyncUtil.login(getJsonUrl(),
                                            signer, username, getClient(), true, 60000L);

        if (response.getStatusCode() == 405) {
            this.setCookies(response.getCookies());
            return true;
        } else {
            return false;
        }
    }

    private ListenableFuture<Response> executeRequest(Request request) throws IOException {
        return this.client.executeRequest(request, AUTHORIZED_RESPONSE_HANDLER);
    }

    private SimpleResponse executeSimpleRequest(Request request)
            throws IOException, InterruptedException, ExecutionException {

        LOGGER.error("[executeSimpleRequest] url: {}", request.getUrl());
        return this.client.executeRequest(request, SIMPLE_RESPONSE_HANDLER).get();
    }

    private DetailedResponse executeDetailedRequest(final Request request, final ResponseProgressListener listener)
        throws IOException, InterruptedException, ExecutionException {

        return this.client.executeRequest(request, new AsyncCompletionHandler<DetailedResponse>(){
            @Override public DetailedResponse onCompleted(Response response) throws Exception {
                return AbstractPackageManagerClient.parseDetailedResponse(
                        response.getStatusCode(),
                        response.getStatusText(),
                        response.getResponseBodyAsStream(),
                        getResponseEncoding(response),
                        listener
                );
            }
        }).get();
    }

    private AsyncHttpClient.BoundRequestBuilder addCookies(AsyncHttpClient.BoundRequestBuilder builder) {
        if (builder != null) {
            for (Cookie cookie : this.cookies) {
                builder.addCookie(cookie);
            }
        }
        return builder;
    }

    /**
     * {@inheritDoc}
     */
    protected final Either<? extends Exception, Boolean> checkServiceAvailability(final boolean checkTimeout,
                                                                                  final long timeoutRemaining) {
        final Request request = this.addCookies(this.client.prepareGet(getJsonUrl())).build();

        try {
            final ListenableFuture<Response> future = executeRequest(request);

            Response response = null;
            if (checkTimeout) {
                response = future.get(timeoutRemaining, TimeUnit.MILLISECONDS);
            } else {
                response = future.get();
            }

            return right(Exception.class, response.getStatusCode() == 405);
        } catch (TimeoutException e) {
            return left(new IOException("Service timeout exceeded."), Boolean.class);
        } catch (Exception e) {
            return left(e, Boolean.class);
        }
    }

    private AsyncHttpClient.BoundRequestBuilder buildSimpleRequest(PackId packageId) {
        if (packageId != null) {
            return this.addCookies(this.client.preparePost(getJsonUrl(packageId)));
        } else {
            return this.addCookies(this.client.preparePost(getJsonUrl()));
        }
    }

    private AsyncHttpClient.BoundRequestBuilder buildDetailedRequest(PackId packageId) {
        if (packageId != null) {
            return this.addCookies(this.client.preparePost(getHtmlUrl(packageId)));
        } else {
            return this.addCookies(this.client.preparePost(getHtmlUrl()));
        }
    }

    private static String getResponseEncoding(Response response) {
        String encoding = response.getHeader("Content-Encoding");

        if (encoding == null) {
            String contentType = response.getContentType();
            int charsetBegin = contentType.toLowerCase().indexOf(";charset=");
            if (charsetBegin >= 0) {
                encoding = contentType.substring(charsetBegin + ";charset=".length());
            }
        }

        return encoding;
    }

    abstract class AuthorizedResponseHandler<T> extends AsyncCompletionHandler<T> {
        protected abstract T onAuthorized(Response response) throws Exception;

        @Override
        public final T onCompleted(Response response) throws Exception {
            if (response.getStatusCode() == 401) {
                throw new IOException(Integer.toString(response.getStatusCode()) + " " + response.getStatusText());
            } else {
                return onAuthorized(response);
            }
        }

        @Override
        public void onThrowable(Throwable t) {
            LOGGER.debug("Caught throwable: {}", t);
        }
    }

    @Override
    protected ResponseBuilder getResponseBuilder() {
        return new AsyncResponseBuilder();
    }

    class AsyncResponseBuilder extends ResponseBuilder {

        private PackId packId = null;
        private Map<String, String> stringParams = new HashMap<String, String>();
        private Map<String, FilePart> fileParams = new HashMap<String, FilePart>();

        @Override
        protected ResponseBuilder forPackId(PackId packId) {
            this.packId = packId;
            return this;
        }

        @Override
        public ResponseBuilder withParam(String name, String value) {
            this.stringParams.put(name, value);
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
            AsyncHttpClient.BoundRequestBuilder requestBuilder = buildSimpleRequest(packId);
            for (Map.Entry<String, String> param : this.stringParams.entrySet()) {
                if (this.fileParams.isEmpty()) {
                    requestBuilder.addParameter(param.getKey(), param.getValue());
                } else {
                    requestBuilder.addQueryParameter(param.getKey(), param.getValue());
                }
            }

            for (Map.Entry<String, FilePart> param : this.fileParams.entrySet()) {
                requestBuilder.addBodyPart(param.getValue());
            }

            return executeSimpleRequest(requestBuilder.build());
        }

        @Override
        public DetailedResponse getDetailedResponse(final ResponseProgressListener listener) throws Exception {
            AsyncHttpClient.BoundRequestBuilder requestBuilder = buildDetailedRequest(packId);
            for (Map.Entry<String, String> param : this.stringParams.entrySet()) {
                if (this.fileParams.isEmpty()) {
                    requestBuilder.addParameter(param.getKey(), param.getValue());
                } else {
                    requestBuilder.addQueryParameter(param.getKey(), param.getValue());
                }
            }

            for (Map.Entry<String, FilePart> param : this.fileParams.entrySet()) {
                requestBuilder.addBodyPart(param.getValue());
            }

            return executeDetailedRequest(requestBuilder.build(), listener);
        }
    }
}
