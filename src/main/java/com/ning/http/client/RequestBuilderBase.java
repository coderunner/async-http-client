/*
 * Copyright 2010 Ning, Inc.
 *
 * Ning licenses this file to you under the Apache License, version 2.0
 * (the "License"); you may not use this file except in compliance with the
 * License.  You may obtain a copy of the License at:
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
package com.ning.http.client;

import com.ning.http.client.Request.EntityWriter;
import com.ning.http.util.UTF8UrlEncoder;

import java.io.File;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Builder for {@link Request}
 *
 * @param <T>
 */
public abstract class RequestBuilderBase<T extends RequestBuilderBase<T>> {

    private static final class RequestImpl implements Request {
        private String reqType;
        private String url = null;
        private FluentCaseInsensitiveStringsMap headers = new FluentCaseInsensitiveStringsMap();
        private Collection<Cookie> cookies = new ArrayList<Cookie>();
        private byte[] byteData;
        private String stringData;
        private InputStream streamData;
        private EntityWriter entityWriter;
        private FluentStringsMap params;
        private List<Part> parts;
        private String virtualHost;
        private long length = -1;
        public FluentStringsMap queryParams;
        public ProxyServer proxyServer;
        private Realm realm;
        private File file;
        private boolean followRedirects;
        private PerRequestConfig perRequestConfig;

        public RequestImpl() {
        }

        public RequestImpl(Request prototype) {
            if (prototype != null) {
                this.reqType = prototype.getReqType();
                int pos = prototype.getUrl().indexOf("?");
                this.url = pos > 0 ? prototype.getUrl().substring(0,pos) : prototype.getUrl();
                this.headers = new FluentCaseInsensitiveStringsMap(prototype.getHeaders());
                this.cookies = new ArrayList<Cookie>(prototype.getCookies());
                this.byteData = prototype.getByteData();
                this.stringData = prototype.getStringData();
                this.streamData = prototype.getStreamData();
                this.entityWriter = prototype.getEntityWriter();
                this.params = (prototype.getParams() == null ? null : new FluentStringsMap(prototype.getParams()));
                this.queryParams = (prototype.getQueryParams() == null ? null : new FluentStringsMap(prototype.getQueryParams()));
                this.parts = (prototype.getParts() == null ? null : new ArrayList<Part>(prototype.getParts()));
                this.virtualHost = prototype.getVirtualHost();
                this.length = prototype.getLength();
                this.proxyServer = prototype.getProxyServer();
                this.realm = prototype.getRealm();
                this.file = prototype.getFile();
                this.followRedirects = prototype.isRedirectEnabled();
                this.perRequestConfig = prototype.getPerRequestConfig();
            }
        }

        /* @Override */

        public String getReqType() {
            return reqType;
        }

        /* @Override */

        public String getUrl() {
            return toUrl(true);
        }

        private String toUrl(boolean encode) {

            if (url == null) throw new NullPointerException("url is null");

            String uri;
            try {
                uri = URI.create(url).toURL().toString();
            } catch (MalformedURLException e) {
                throw new IllegalStateException("Illegal URL: " + url, e);
            }

            if (queryParams != null) {

                StringBuilder builder = new StringBuilder();
                if (!url.substring(8).contains("/")) { // no other "/" than http[s]:// -> http://localhost:1234
                    builder.append("/");
                }
                builder.append("?"); 

                for (Iterator<Entry<String, List<String>>> i = queryParams.iterator(); i.hasNext();) {
                    Map.Entry<String, List<String>> param = i.next();
                    String name = param.getKey();
                    for (Iterator<String> j = param.getValue().iterator(); j.hasNext();) {
                        String value = j.next();
                        if (encode) {
                            UTF8UrlEncoder.appendEncoded(builder, name);
                        } else {
                            builder.append(name);
                        }
                        if (value != null) {
                            builder.append('=');
                            if (encode) {
                                UTF8UrlEncoder.appendEncoded(builder, value);
                            } else {
                                builder.append(value);
                            }
                        }
                        if (j.hasNext()) {
                            builder.append('&');
                        }
                    }
                    if (i.hasNext()) {
                        builder.append('&');
                    }
                }
                uri += builder.toString();
            }
            return uri;
        }

        /* @Override */
        public String getRawUrl() {
            return toUrl(false);
        }

        /* @Override */
        public FluentCaseInsensitiveStringsMap getHeaders() {
            return headers;
        }

        /* @Override */
        public Collection<Cookie> getCookies() {
            return Collections.unmodifiableCollection(cookies);
        }

        /* @Override */
        public byte[] getByteData() {
            return byteData;
        }

        /* @Override */
        public String getStringData() {
            return stringData;
        }

        /* @Override */
        public InputStream getStreamData() {
            return streamData;
        }

        /* @Override */
        public EntityWriter getEntityWriter() {
            return entityWriter;
        }

        /* @Override */
        public long getLength() {
            return length;
        }

        /* @Override */
        public FluentStringsMap getParams() {
            return params;
        }

        /* @Override */
        public List<Part> getParts() {
            return parts;
        }

        /* @Override */
        public String getVirtualHost() {
            return virtualHost;
        }

        public FluentStringsMap getQueryParams() {
            return queryParams;
        }

        public ProxyServer getProxyServer() {
            return proxyServer;
        }

        public Realm getRealm() {
            return realm;
        }

        public File getFile() {
            return file;
        }

        public boolean isRedirectEnabled() {
            return followRedirects;
        }

        public PerRequestConfig getPerRequestConfig() {
            return perRequestConfig;
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder(url);

            sb.append("\t");
            sb.append(reqType);
            for (String name : headers.keySet()) {
                sb.append("\t");
                sb.append(name);
                sb.append(":");
                sb.append(headers.getJoinedValue(name, ", "));
            }

            return sb.toString();
        }
    }

    private final Class<T> derived;
    protected final RequestImpl request;

    protected RequestBuilderBase(Class<T> derived, String reqType) {
        this.derived = derived;
        request = new RequestImpl();
        request.reqType = reqType;
    }

    protected RequestBuilderBase(Class<T> derived, Request prototype) {
        this.derived = derived;
        request = new RequestImpl(prototype);
    }
    
    public T setUrl(String url) {
        request.url = buildUrl(url);
        return derived.cast(this);
    }

    private String buildUrl(String url) {
        URI uri = URI.create(url);
        StringBuilder buildedUrl = new StringBuilder();

        if (uri.getScheme() != null) {
            buildedUrl.append(uri.getScheme());
            buildedUrl.append("://");
        }

        if (uri.getAuthority() != null) {
            buildedUrl.append(uri.getAuthority());
        }
        buildedUrl.append(uri.getRawPath());

        if (uri.getRawQuery() != null && !uri.getRawQuery().equals("")) {
            String[] queries = uri.getRawQuery().split("&");
            int pos = 0;
            for( String query : queries) {
                pos = query.indexOf("=");
                if (pos <= 0) {
                    addQueryParameter(query, null);
                }else{
                    addQueryParameter(query.substring(0, pos) , query.substring(pos +1));
                }
            }
        }
        return buildedUrl.toString();
    }


    public T setVirtualHost(String virtualHost) {
        request.virtualHost = virtualHost;
        return derived.cast(this);
    }

    public T setHeader(String name, String value) {
        request.headers.replace(name, value);
        return derived.cast(this);
    }

    public T addHeader(String name, String value) {
        request.headers.add(name, value);
        return derived.cast(this);
    }

    public T setHeaders(FluentCaseInsensitiveStringsMap headers) {
        request.headers = (headers == null ? new FluentCaseInsensitiveStringsMap() : new FluentCaseInsensitiveStringsMap(headers));
        return derived.cast(this);
    }

    public T setHeaders(Map<String, Collection<String>> headers) {
        request.headers = (headers == null ? new FluentCaseInsensitiveStringsMap() : new FluentCaseInsensitiveStringsMap(headers));
        return derived.cast(this);
    }

    public T addCookie(Cookie cookie) {
        request.cookies.add(cookie);
        return derived.cast(this);
    }

    private void resetParameters() {
        request.params = null;
    }

    private void resetNonMultipartData() {
        request.byteData = null;
        request.stringData = null;
        request.streamData = null;
        request.entityWriter = null;
        request.length = -1;
    }

    private void resetMultipartData() {
        request.parts = null;
    }
        
    public T setBody(File file) {
        request.file = file;
        return derived.cast(this);
    }

    public T setBody(byte[] data) throws IllegalArgumentException {
        if ((!"POST".equals(request.reqType)) && (!"PUT".equals(request.reqType))) {
            throw new IllegalArgumentException("Request type has to POST or PUT for content");
        }
        resetParameters();
        resetNonMultipartData();
        resetMultipartData();
        request.byteData = data;
        return derived.cast(this);
    }

    public T setBody(String data) throws IllegalArgumentException {
        if ((!"POST".equals(request.reqType)) && (!"PUT".equals(request.reqType))) {
            throw new IllegalArgumentException("Request type has to POST or PUT for content");
        }
        resetParameters();
        resetNonMultipartData();
        resetMultipartData();
        request.stringData = data;
        return derived.cast(this);
    }

    public T setBody(InputStream stream) throws IllegalArgumentException {
        if ((!"POST".equals(request.reqType)) && (!"PUT".equals(request.reqType))) {
            throw new IllegalArgumentException("Request type has to POST or PUT for content");
        }
        resetParameters();
        resetNonMultipartData();
        resetMultipartData();
        request.streamData = stream;
        return derived.cast(this);
    }

    public T setBody(EntityWriter dataWriter) {
        return setBody(dataWriter, -1);
    }

    public T setBody(EntityWriter dataWriter, long length) throws IllegalArgumentException {
        if ((!"POST".equals(request.reqType)) && (!"PUT".equals(request.reqType))) {
            throw new IllegalArgumentException("Request type has to POST or PUT for content");
        }
        resetParameters();
        resetNonMultipartData();
        resetMultipartData();
        request.entityWriter = dataWriter;
        request.length = length;
        return derived.cast(this);
    }

    public T addQueryParameter(String name, String value) {
        if (request.queryParams == null) {
            request.queryParams = new FluentStringsMap();
        }
        request.queryParams.add(name, value);
        return derived.cast(this);
    }

    public T addParameter(String key, String value) throws IllegalArgumentException {
        if ((!"POST".equals(request.reqType)) && (!"PUT".equals(request.reqType))) {
            throw new IllegalArgumentException("Request type has to POST or PUT for form parameters");
        }
        resetNonMultipartData();
        resetMultipartData();
        if (request.params == null) {
            request.params = new FluentStringsMap();
        }
        request.params.add(key, value);
        return derived.cast(this);
    }

    public T setParameters(FluentStringsMap parameters) throws IllegalArgumentException {
        if ((!"POST".equals(request.reqType)) && (!"PUT".equals(request.reqType))) {
            throw new IllegalArgumentException("Request type has to POST or PUT for form parameters");
        }
        resetNonMultipartData();
        resetMultipartData();
        request.params = new FluentStringsMap(parameters);
        return derived.cast(this);
    }

    public T setParameters(Map<String, Collection<String>> parameters) throws IllegalArgumentException {
        if ((!"POST".equals(request.reqType)) && (!"PUT".equals(request.reqType))) {
            throw new IllegalArgumentException("Request type has to POST or PUT for form parameters");
        }
        resetNonMultipartData();
        resetMultipartData();
        request.params = new FluentStringsMap(parameters);
        return derived.cast(this);
    }

    public T addBodyPart(Part part) throws IllegalArgumentException {
        if ((!"POST".equals(request.reqType)) && (!"PUT".equals(request.reqType))) {
            throw new IllegalArgumentException("Request type has to POST or PUT for parts");
        }
        resetParameters();
        resetNonMultipartData();
        if (request.parts == null) {
            request.parts = new ArrayList<Part>();
        }
        request.parts.add(part);
        return derived.cast(this);
    }

    public T setProxyServer(ProxyServer proxyServer) {
        request.proxyServer = proxyServer;
        return derived.cast(this);
    }

    public T setRealm(Realm realm) {
        request.realm = realm;
        return derived.cast(this);
    }

    public T setFollowRedirects(boolean followRedirects) {
        request.followRedirects = followRedirects;
        return derived.cast(this);
    }

    public T setPerRequestConfig(PerRequestConfig perRequestConfig) {
        request.perRequestConfig = perRequestConfig;
        return derived.cast(this);
    }

    public Request build() {
        if ((request.length < 0) && (request.streamData == null) &&
                (("POST".equals(request.reqType)) || ("PUT".equals(request.reqType)))) {
            // can't concatenate content-length
            String contentLength = request.headers.getFirstValue("Content-Length");

            if (contentLength != null) {
                try {
                    request.length = Long.parseLong(contentLength);
                }
                catch (NumberFormatException e) {
                    // NoOp -- we wdn't specify length so it will be chunked?
                }
            }
        }
        return request;
    }
}
