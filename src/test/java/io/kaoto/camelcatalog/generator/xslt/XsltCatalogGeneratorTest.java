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

        generator.generate();

        Path catalogFile = tempDir.resolve("3.0").resolve("xslt-xpath-functions.json");
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
        assertEquals("3.0/xslt-xpath-functions.json", entry30.file());
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
        assertEquals("3.0/xslt-xpath-functions.json", entry.file());
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
        assertEquals("3.0/xslt-xpath-functions.json", rootDefinition.getCatalogs().get("3.0").file());
        assertEquals("3.1/xslt-xpath-functions.json", rootDefinition.getCatalogs().get("3.1").file());
        assertTrue(Files.exists(tempDir.resolve("3.0/xslt-xpath-functions.json")));
        assertTrue(Files.exists(tempDir.resolve("3.1/xslt-xpath-functions.json")));
    }

}
