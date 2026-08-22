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

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.util.DefaultIndenter;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;

import java.io.IOException;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.zip.CRC32;

public class Util {

    private Util() {
    }

    private static final ObjectMapper jsonMapper = new ObjectMapper();
    /**
     * Creates a pretty printer that uses tabs for indentation instead of spaces.
     */
    public static DefaultPrettyPrinter createTabPrettyPrinter() {
        DefaultPrettyPrinter prettyPrinter = new DefaultPrettyPrinter();
        prettyPrinter.indentArraysWith(new DefaultIndenter("\t", "\n"));
        prettyPrinter.indentObjectsWith(new DefaultIndenter("\t", "\n"));
        return prettyPrinter;
    }

    /**
     * Creates an ObjectWriter configured with tab indentation.
     */
    public static ObjectWriter createTabWriter(ObjectMapper mapper) {
        return mapper.writer(createTabPrettyPrinter());
    }
    /**
     * Generates a CRC32 hex string for the given content.
     * This is used exclusively for cache-busting filename suffixes and carries
     * no security requirement — CRC32 is the appropriate tool here.
     * Using java.util.zip.CRC32 (rather than java.security.MessageDigest) makes
     * the non-cryptographic intent explicit and avoids weak-algorithm Sonar warnings.
     */
    public static String generateHash(byte[] content) {
        if (content == null)
            return null;
        var crc = new CRC32();
        crc.update(content);
        return Long.toHexString(crc.getValue());
    }

    public static String generateHash(Path path) throws IOException {
        return path == null ? null : generateHash(Files.readAllBytes(path));
    }

    public static String generateHash(String content) {
        return content == null ? null : generateHash(content.getBytes());
    }

    public static String getNormalizedFolder(String folder) {
        // Get the current working directory
        Path currentDirectory = Paths.get("").toAbsolutePath();

        // Resolve the relative path
        Path absolutePath = currentDirectory.resolve(folder);

        return absolutePath.toString();
    }

    public static String getPrettyJSON(Object node) throws IOException {
        StringWriter writer = new StringWriter();
        try (var jsonGenerator = new JsonFactory().createGenerator(writer).setPrettyPrinter(createTabPrettyPrinter())) {
            jsonMapper.writeTree(jsonGenerator, jsonMapper.valueToTree(node));
        }
        return writer.toString();
    }
}
