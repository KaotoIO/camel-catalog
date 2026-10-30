/*
 * Copyright (C) 2026 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.kaoto.camelcatalog.maven;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLConnection;
import java.time.Duration;

/**
 * Opens remote URLs, such as files in Maven repositories, with finite connect and read timeouts.
 * {@code URL.openStream()} waits forever by default, so a repository that stops answering would block the
 * catalog generation.
 */
public final class UrlStreams {
    public static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(10);
    /** The longest wait for the next bytes of a response, not for the whole download. */
    public static final Duration READ_TIMEOUT = Duration.ofSeconds(30);

    private UrlStreams() {
    }

    /** Opens the URL with {@link #CONNECT_TIMEOUT} and {@link #READ_TIMEOUT}. */
    public static InputStream open(String url) throws IOException, URISyntaxException {
        return open(url, CONNECT_TIMEOUT, READ_TIMEOUT);
    }

    /**
     * Opens the URL, failing with a {@link java.net.SocketTimeoutException} if it doesn't connect or answer within
     * the given timeouts.
     */
    public static InputStream open(String url, Duration connectTimeout, Duration readTimeout)
            throws IOException, URISyntaxException {
        return openConnection(url, connectTimeout, readTimeout).getInputStream();
    }

    static URLConnection openConnection(String url, Duration connectTimeout, Duration readTimeout)
            throws IOException, URISyntaxException {
        URLConnection connection = new URI(url).toURL().openConnection();
        connection.setConnectTimeout(Math.toIntExact(connectTimeout.toMillis()));
        connection.setReadTimeout(Math.toIntExact(readTimeout.toMillis()));
        return connection;
    }
}
