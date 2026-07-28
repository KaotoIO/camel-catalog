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
        var generator = new XsltCatalogGenerator("3.0", outputDirectory);
        assertNotNull(generator);
    }

    @Test
    void testGenerateReturnsValidCatalogDefinition() {
        var generator = new XsltCatalogGenerator("3.0", outputDirectory);

        CatalogDefinition catalogDefinition = generator.generate();

        assertNotNull(catalogDefinition);
        assertEquals("XSLT 3.0", catalogDefinition.getName());
        assertEquals(CatalogRuntime.XSLT, catalogDefinition.getRuntime());
        assertEquals("3.0", catalogDefinition.getVersion());
        assertEquals("index.json", catalogDefinition.getFileName());
    }

    @Test
    void testGenerateCreatesCatalogFile() throws Exception {
        var generator = new XsltCatalogGenerator("3.0", outputDirectory);

        generator.generate();

        Path catalogFile = tempDir.resolve("xslt-xpath-functions.json");
        assertTrue(Files.exists(catalogFile));

        JsonNode catalog = objectMapper.readTree(catalogFile.toFile());
        assertTrue(catalog.has("namespaces"));
        assertTrue(catalog.size() > 1);
    }

    @Test
    void testGenerateCreatesIndexFile() throws Exception {
        var generator = new XsltCatalogGenerator("3.0", outputDirectory);

        generator.generate();

        Path indexFile = tempDir.resolve("index.json");
        assertTrue(Files.exists(indexFile));

        JsonNode index = objectMapper.readTree(indexFile.toFile());
        assertEquals("XSLT 3.0", index.get("name").asText());
        assertEquals("XSLT", index.get("runtime").asText());
        assertEquals("3.0", index.get("version").asText());
    }

    @Test
    void testGenerateCatalogDefinitionHasXPathFunctionsEntry() {
        var generator = new XsltCatalogGenerator("3.0", outputDirectory);

        CatalogDefinition catalogDefinition = generator.generate();

        assertTrue(catalogDefinition.getCatalogs().containsKey("xpathFunctions"));
        var entry = catalogDefinition.getCatalogs().get("xpathFunctions");
        assertEquals("xpathFunctions", entry.name());
        assertEquals("xslt-xpath-functions.json", entry.file());
        assertEquals("3.0", entry.version());
    }

    @Test
    void testGenerateWithResolvedVersions() {
        var generator = new XsltCatalogGenerator("3.0", outputDirectory);

        var resolvedVersions = new ResolvedVersions(
                "4.15.0",
                "4.15.0",
                "3.27.0",
                "4.15.0"
        );
        generator.setResolvedVersions(resolvedVersions);

        CatalogDefinition catalogDefinition = generator.generate();

        assertNotNull(catalogDefinition);
        assertEquals("4.15.0", catalogDefinition.getCamelCatalogVersion());
        assertEquals("4.15.0", catalogDefinition.getRuntimeProviderVersion());
        assertEquals("3.27.0", catalogDefinition.getFrameworkVersion());
    }

    @Test
    void testGenerateWithNullResolvedVersions() {
        var generator = new XsltCatalogGenerator("3.0", outputDirectory);

        CatalogDefinition catalogDefinition = generator.generate();

        assertNotNull(catalogDefinition);
        assertNull(catalogDefinition.getCamelCatalogVersion());
        assertNull(catalogDefinition.getRuntimeProviderVersion());
        assertNull(catalogDefinition.getFrameworkVersion());
    }

    @Test
    void testCatalogDefinitionNameFormat() {
        String[] versions = {"3.0", "3.1", "4.0-SNAPSHOT"};

        for (String version : versions) {
            var generator = new XsltCatalogGenerator(version, outputDirectory);
            CatalogDefinition catalogDefinition = generator.generate();

            assertEquals("XSLT " + version, catalogDefinition.getName());
        }
    }

    @Test
    void testCatalogDefinitionFileName() {
        var generator = new XsltCatalogGenerator("3.0", outputDirectory);

        CatalogDefinition catalogDefinition = generator.generate();

        assertEquals("index.json", catalogDefinition.getFileName());
    }

}
