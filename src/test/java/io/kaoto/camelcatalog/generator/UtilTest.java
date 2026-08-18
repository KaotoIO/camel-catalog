/*
 * Copyright (C) 2024 Red Hat, Inc.
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
package io.kaoto.camelcatalog.generator;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class UtilTest {
    private final List<String> testFiles = List.of("testfile1.txt", "testfile2.txt");

    @Test
    void testGenerateHash() throws Exception {
        var fileHashMap = new HashMap<String, String>();
        for (var file : testFiles) {
            var is = Thread.currentThread().getContextClassLoader().getResourceAsStream(file);
            if (is == null) continue;
            var bytes = is.readAllBytes();
            var hash = Util.generateHash(bytes);
            assertFalse(fileHashMap.containsKey(file));
            assertFalse(fileHashMap.containsValue(hash));
            fileHashMap.put(file, hash);
        }
        for (var file : testFiles) {
            var is = Thread.currentThread().getContextClassLoader().getResourceAsStream(file);
            if (is == null) continue;
            var bytes = is.readAllBytes();
            var hash = Util.generateHash(bytes);
            assertTrue(fileHashMap.containsKey(file));
            assertEquals(hash, fileHashMap.get(file));
        }
    }

    @Test
    void testGenerateHashFromPath() throws Exception {
        var url = Thread.currentThread().getContextClassLoader().getResource(testFiles.get(0));
        if (url == null) throw new Exception("no test file available");
        var testFilePath = Path.of(url.toURI());
        var hash = Util.generateHash(testFilePath);
        assertNotNull(hash);
        url = Thread.currentThread().getContextClassLoader().getResource(testFiles.get(0));
        if (url == null) throw new Exception("no test file available");
        testFilePath = Path.of(url.toURI());
        var hash2 = Util.generateHash(testFilePath);
        assertEquals(hash, hash2);
    }

    @Test
    void testGenerateHashFromString() throws Exception {
        var is = Thread.currentThread().getContextClassLoader().getResourceAsStream(testFiles.get(0));
        if (is == null) throw new Exception("no test file available");
        var testFileString = new String(is.readAllBytes());
        var hash = Util.generateHash(testFileString);
        assertNotNull(hash);
        is = Thread.currentThread().getContextClassLoader().getResourceAsStream(testFiles.get(0));
        if (is == null) throw new Exception("no test file available");
        testFileString = new String(is.readAllBytes());
        var hash2 = Util.generateHash(testFileString);
        assertEquals(hash, hash2);
    }

    @Test
    void testGenerateHashUsesCRC32() throws Exception {
        // Known-vector: CRC32 of "123456789" is cbf43926 (ISO 3309 standard check value)
        assertEquals("cbf43926", Util.generateHash("123456789".getBytes(StandardCharsets.UTF_8)));

        // Resource-based: non-null, hex format, and within CRC32's 32-bit range (≤8 hex chars)
        var is = Thread.currentThread().getContextClassLoader().getResourceAsStream(testFiles.get(0));
        if (is == null) throw new Exception("no test file available");
        var hash = Util.generateHash(is.readAllBytes());
        assertNotNull(hash);
        assertTrue(hash.matches("[0-9a-f]{1,8}"),
                "Expected a CRC32 hex hash matching [0-9a-f]{1,8} but got: " + hash);
    }
}
