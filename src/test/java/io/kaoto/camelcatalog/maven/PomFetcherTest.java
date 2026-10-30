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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.net.InetAddress;
import java.net.ServerSocket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;

class PomFetcherTest {

    private static final Duration SHORT_TIMEOUT = Duration.ofMillis(200);

    @Test
    void triesTheNextRepositoryWhenOneDoesNotAnswer(@TempDir Path repository) throws Exception {
        Path pom = repository.resolve("org/example/demo/1.0.0/demo-1.0.0.pom");
        Files.createDirectories(pom.getParent());
        Files.writeString(pom, "<project/>");

        // The server never accepts: the connection waits in its backlog, so the request is sent but never answered
        try (ServerSocket stalledRepository = new ServerSocket(0, 1, InetAddress.getByName("127.0.0.1"))) {
            List<String> repositories = List.of("http://127.0.0.1:" + stalledRepository.getLocalPort(),
                    repository.toUri().toString());

            String pomXml = assertTimeoutPreemptively(Duration.ofSeconds(10),
                    () -> PomFetcher.http(SHORT_TIMEOUT, SHORT_TIMEOUT)
                            .fetchPom("org.example", "demo", "1.0.0", repositories));

            assertEquals("<project/>", pomXml);
        }
    }
}
