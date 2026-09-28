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
package io.kaoto.camelcatalog.generator.xslt;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.kaoto.camelcatalog.model.CatalogDefinition;
import io.kaoto.camelcatalog.model.CatalogDefinitionEntry;
import io.kaoto.camelcatalog.model.CatalogRuntime;
import io.kaoto.camelcatalog.model.ResolvedVersions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class XsltCatalogGeneratorTest {

    @TempDir
    Path tempDir;

    private File outputDirectory;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        outputDirectory = tempDir.toFile();
        objectMapper = new ObjectMapper();
    }

    @Test
    void testConstructorInitialization() {
        var generator = new XsltCatalogGenerator(java.util.List.of("3.0"), outputDirectory);
        assertNotNull(generator);
    }

    @Test
    void testGenerateReturnsValidRootCatalogDefinition() {
        var generator = new XsltCatalogGenerator(java.util.List.of("3.0"), outputDirectory);

        CatalogDefinition catalogDefinition = generator.generate();

        assertNotNull(catalogDefinition);
        assertEquals("XSLT Catalogs", catalogDefinition.getName());
        assertEquals(CatalogRuntime.XSLT, catalogDefinition.getRuntime());
        assertEquals("1", catalogDefinition.getVersion());
        assertTrue(catalogDefinition.getFileName().startsWith("index-"),
                "fileName should be a hashed index filename");
        assertTrue(catalogDefinition.getFileName().endsWith(".json"),
                "fileName should end with .json");
        assertTrue(catalogDefinition.getCatalogs().containsKey("3.0"));
    }

    @Test
    void testGenerateCreatesCatalogFile() throws Exception {
        var generator = new XsltCatalogGenerator(java.util.List.of("3.0"), outputDirectory);

        CatalogDefinition rootDefinition = generator.generate();

        // filename now includes a content hash, e.g. xslt-xpath-functions-<hash>.json
        String catalogFileName = rootDefinition.getCatalogs().get("3.0").file();
        Path catalogFile = tempDir.resolve(catalogFileName);
        assertTrue(Files.exists(catalogFile));

        JsonNode catalog = objectMapper.readTree(catalogFile.toFile());
        assertTrue(catalog.has("namespaces"));
        assertTrue(catalog.size() > 1);
    }

    @Test
    void testGenerateCreatesIndexFiles() throws Exception {
        var generator = new XsltCatalogGenerator(java.util.List.of("3.0"), outputDirectory);

        CatalogDefinition rootDefinition = generator.generate();

        Path rootIndexFile = tempDir.resolve(rootDefinition.getFileName());
        assertTrue(Files.exists(rootIndexFile));

        JsonNode rootIndex = objectMapper.readTree(rootIndexFile.toFile());
        assertEquals("XSLT Catalogs", rootIndex.get("name").asText());
        assertEquals("XSLT", rootIndex.get("runtime").asText());
        assertEquals("1", rootIndex.get("version").asText());

        CatalogDefinitionEntry entry30 = rootDefinition.getCatalogs().get("3.0");
        assertNotNull(entry30);
        assertTrue(entry30.file().startsWith("3.0/xslt-xpath-functions-"),
                "catalog entry file should start with versioned prefix");
        assertTrue(entry30.file().endsWith(".json"),
                "catalog entry file should end with .json");
        Path v30FunctionsFile = tempDir.resolve(entry30.file());
        assertTrue(Files.exists(v30FunctionsFile));
    }

    @Test
    void testGenerateCatalogDefinitionHasVersionEntry() {
        var generator = new XsltCatalogGenerator(java.util.List.of("3.0"), outputDirectory);

        CatalogDefinition rootDefinition = generator.generate();

        assertTrue(rootDefinition.getCatalogs().containsKey("3.0"));
        var entry = rootDefinition.getCatalogs().get("3.0");
        assertEquals("3.0", entry.name());
        assertTrue(entry.file().startsWith("3.0/xslt-xpath-functions-"),
                "catalog entry file should start with versioned prefix");
        assertTrue(entry.file().endsWith(".json"),
                "catalog entry file should end with .json");
        assertEquals("3.0", entry.version());
    }

    @Test
    void testGenerateWithResolvedVersions() {
        var generator = new XsltCatalogGenerator(java.util.List.of("3.0"), outputDirectory);

        var resolvedVersions = new ResolvedVersions(
                "4.15.0",
                "4.15.0",
                "3.27.0",
                "4.15.0"
        );
        generator.setResolvedVersions(resolvedVersions);

        CatalogDefinition rootDefinition = generator.generate();

        assertNotNull(rootDefinition);
        assertEquals("4.15.0", rootDefinition.getCamelCatalogVersion());
        assertEquals("4.15.0", rootDefinition.getRuntimeProviderVersion());
        assertEquals("3.27.0", rootDefinition.getFrameworkVersion());
    }

    @Test
    void testGenerateWithNullResolvedVersions() {
        var generator = new XsltCatalogGenerator(java.util.List.of("3.0"), outputDirectory);

        CatalogDefinition rootDefinition = generator.generate();

        assertNotNull(rootDefinition);
        assertNull(rootDefinition.getCamelCatalogVersion());
        assertNull(rootDefinition.getRuntimeProviderVersion());
        assertNull(rootDefinition.getFrameworkVersion());
    }

    @Test
    void testGenerateMultipleVersions() {
        var generator = new XsltCatalogGenerator(java.util.List.of("3.0", "3.1"), outputDirectory);
        CatalogDefinition rootDefinition = generator.generate();

        assertEquals("XSLT Catalogs", rootDefinition.getName());
        assertEquals(2, rootDefinition.getCatalogs().size());
        assertTrue(rootDefinition.getCatalogs().containsKey("3.0"));
        assertTrue(rootDefinition.getCatalogs().containsKey("3.1"));
        String file30 = rootDefinition.getCatalogs().get("3.0").file();
        String file31 = rootDefinition.getCatalogs().get("3.1").file();
        assertTrue(file30.startsWith("3.0/xslt-xpath-functions-") && file30.endsWith(".json"));
        assertTrue(file31.startsWith("3.1/xslt-xpath-functions-") && file31.endsWith(".json"));
        assertTrue(Files.exists(tempDir.resolve(file30)));
        assertTrue(Files.exists(tempDir.resolve(file31)));
    }

    @Test
    void testFunctionCatalogFilenameIncludesContentHash() {
        var generator = new XsltCatalogGenerator(java.util.List.of("3.0"), outputDirectory);
        CatalogDefinition rootDefinition = generator.generate();

        String catalogFile = rootDefinition.getCatalogs().get("3.0").file();
        // filename must be: xslt-xpath-functions-<hash>.json  (not fixed)
        assertTrue(catalogFile.matches("3\\.0/xslt-xpath-functions-[0-9a-f]+\\.json"),
                "function catalog filename must contain a content hash: " + catalogFile);
    }

}
