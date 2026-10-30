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

import java.net.URLConnection;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;

class UrlStreamsTest {

    @Test
    void setsTheConnectAndReadTimeouts() throws Exception {
        URLConnection connection = UrlStreams.openConnection("http://localhost/", Duration.ofMillis(1500),
                Duration.ofMillis(2500));

        assertEquals(1500, connection.getConnectTimeout());
        assertEquals(2500, connection.getReadTimeout());
    }
}
