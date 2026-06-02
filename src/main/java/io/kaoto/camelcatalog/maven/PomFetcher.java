/*
 * Copyright (C) 2023 Red Hat, Inc.
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

import org.apache.commons.io.IOUtils;

import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Fetches the raw text of a Maven POM by coordinates, trying each repository in order.
 * Extracted as an interface so resolution logic can be unit tested without network access.
 */
@FunctionalInterface
public interface PomFetcher {

    /**
     * @return the POM XML text, or {@code null} if it could not be fetched from any repository.
     */
    String fetchPom(String groupId, String artifactId, String version, List<String> repositoryBaseUrls);

    /** Default HTTP-based implementation. */
    static PomFetcher http() {
        final Logger logger = Logger.getLogger(PomFetcher.class.getName());
        return (groupId, artifactId, version, repositoryBaseUrls) -> {
            String groupPath = groupId.replace('.', '/');
            for (String base : repositoryBaseUrls) {
                String normalized = base.endsWith("/") ? base.substring(0, base.length() - 1) : base;
                String url = String.format("%s/%s/%s/%s/%s-%s.pom",
                        normalized, groupPath, artifactId, version, artifactId, version);
                try (InputStream is = new URI(url).toURL().openStream()) {
                    return IOUtils.toString(is, StandardCharsets.UTF_8);
                } catch (Exception e) {
                    logger.log(Level.FINE, () -> "POM not found at " + url + ": " + e.getMessage());
                }
            }
            logger.warning("Could not fetch POM " + groupId + ":" + artifactId + ":" + version);
            return null;
        };
    }
}
