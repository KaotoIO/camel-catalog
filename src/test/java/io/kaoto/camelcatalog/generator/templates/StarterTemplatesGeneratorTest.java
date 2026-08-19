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

package io.kaoto.camelcatalog.generator.templates;

import io.kaoto.camelcatalog.model.CatalogDefinition;
import io.kaoto.camelcatalog.model.CatalogRuntime;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Files;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

class StarterTemplatesGeneratorTest {

    private static final int EXPECTED_TEMPLATE_COUNT = 7;
    private static final Pattern HASH_YAML = Pattern.compile(".*-[0-9a-f]{1,8}\\.yaml");
    private static final Pattern HASH_XML  = Pattern.compile(".*-[0-9a-f]{1,8}\\.xml");
    private static final Pattern HASH_INDEX = Pattern.compile("index-[0-9a-f]{1,8}\\.json");
    private static final String KAOTO_ID_TOKEN = "__KAOTO_ID__";

    @TempDir
    File tempDir;

    @Test
    void testGeneratorProducesDefinition() {
        var generator = new StarterTemplatesGeneratorBuilder()
                .withOutputDirectory(tempDir)
                .build();

        CatalogDefinition def = generator.generate();

        assertNotNull(def);
        assertEquals(CatalogRuntime.StarterTemplates, def.getRuntime());
        assertEquals("Kaoto Starter Templates", def.getName());
        assertEquals("1", def.getVersion());
    }

    @Test
    void testGeneratorProducesSevenTemplates() {
        var generator = new StarterTemplatesGeneratorBuilder()
                .withOutputDirectory(tempDir)
                .build();

        CatalogDefinition def = generator.generate();

        assertEquals(EXPECTED_TEMPLATE_COUNT, def.getCatalogs().size());
    }

    @Test
    void testAllTemplateFilesExistInOutputDirectory() throws Exception {
        var generator = new StarterTemplatesGeneratorBuilder()
                .withOutputDirectory(tempDir)
                .build();

        CatalogDefinition def = generator.generate();

        for (var entry : def.getCatalogs().values()) {
            File file = tempDir.toPath().resolve(entry.file()).toFile();
            assertTrue(file.exists(), "Missing template file: " + entry.file());
            assertTrue(file.length() > 0, "Empty template file: " + entry.file());
        }
    }

    @Test
    void testTemplateFilenamesContainHash() {
        var generator = new StarterTemplatesGeneratorBuilder()
                .withOutputDirectory(tempDir)
                .build();

        CatalogDefinition def = generator.generate();

        for (var entry : def.getCatalogs().values()) {
            String file = entry.file();
            boolean matchesYaml = HASH_YAML.matcher(file).matches();
            boolean matchesXml  = HASH_XML.matcher(file).matches();
            assertTrue(matchesYaml || matchesXml,
                    "Filename does not match hashed pattern: " + file);
        }
    }

    @Test
    void testIndexFilenameContainsHash() {
        var generator = new StarterTemplatesGeneratorBuilder()
                .withOutputDirectory(tempDir)
                .build();

        CatalogDefinition def = generator.generate();

        assertTrue(HASH_INDEX.matcher(def.getFileName()).matches(),
                "Index filename does not match hashed pattern: " + def.getFileName());
    }

    @Test
    void testIndexFileExistsInOutputDirectory() {
        var generator = new StarterTemplatesGeneratorBuilder()
                .withOutputDirectory(tempDir)
                .build();

        CatalogDefinition def = generator.generate();

        File indexFile = tempDir.toPath().resolve(def.getFileName()).toFile();
        assertTrue(indexFile.exists(), "Index file not found: " + def.getFileName());
    }

    @Test
    void testKaotoIdTokenPresentInCamelTemplates() throws Exception {
        var generator = new StarterTemplatesGeneratorBuilder()
                .withOutputDirectory(tempDir)
                .build();

        CatalogDefinition def = generator.generate();

        String[] tokened = {
            "camel-route-yaml", "camel-route-xml",
            "pipe-yaml",
            "kamelet-source-yaml", "kamelet-action-yaml", "kamelet-sink-yaml"
        };

        for (String id : tokened) {
            var entry = def.getCatalogs().get(id);
            assertNotNull(entry, "Missing catalog entry: " + id);
            String content = Files.readString(tempDir.toPath().resolve(entry.file()));
            assertTrue(content.contains(KAOTO_ID_TOKEN),
                    "Template '" + id + "' must contain " + KAOTO_ID_TOKEN);
        }
    }

    @Test
    void testKaotoIdTokenAbsentInCitrusTemplate() throws Exception {
        var generator = new StarterTemplatesGeneratorBuilder()
                .withOutputDirectory(tempDir)
                .build();

        CatalogDefinition def = generator.generate();

        var entry = def.getCatalogs().get("citrus-yaml");
        assertNotNull(entry, "Missing catalog entry: citrus-yaml");
        String content = Files.readString(tempDir.toPath().resolve(entry.file()));
        assertFalse(content.contains(KAOTO_ID_TOKEN),
                "Citrus template must not contain " + KAOTO_ID_TOKEN);
    }

    @Test
    void testExpectedTemplateIdsPresent() {
        var generator = new StarterTemplatesGeneratorBuilder()
                .withOutputDirectory(tempDir)
                .build();

        CatalogDefinition def = generator.generate();

        String[] expectedIds = {
            "camel-route-yaml", "camel-route-xml",
            "pipe-yaml",
            "kamelet-source-yaml", "kamelet-action-yaml", "kamelet-sink-yaml",
            "citrus-yaml"
        };

        for (String id : expectedIds) {
            assertTrue(def.getCatalogs().containsKey(id), "Missing template id: " + id);
        }
    }
}
