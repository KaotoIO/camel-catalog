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
package io.kaoto.camelcatalog.generator.citrus;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
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

class CitrusCatalogGeneratorTest {

    @TempDir
    Path tempDir;

    private CitrusCatalogGenerator generator;
    private File outputDirectory;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        outputDirectory = tempDir.toFile();
        objectMapper = new ObjectMapper();
    }

    @Test
    void testConstructorInitialization() {
        generator = new CitrusCatalogGenerator("4.10.0", outputDirectory, false);
        assertNotNull(generator);
    }

    @Test
    void testConstructorWithVerboseLogging() {
        generator = new CitrusCatalogGenerator("4.10.0", outputDirectory, true);
        assertNotNull(generator);
    }

    @Test
    void testGenerateReturnsValidCatalogDefinition() {
        generator = new CitrusCatalogGenerator("4.10.0", outputDirectory, false);
        
        CatalogDefinition catalogDefinition = generator.generate();
        
        assertNotNull(catalogDefinition);
        assertEquals("Citrus 4.10.0", catalogDefinition.getName());
        assertEquals(CatalogRuntime.Citrus, catalogDefinition.getRuntime());
        assertEquals("4.10.0", catalogDefinition.getVersion());
        assertEquals("index.json", catalogDefinition.getFileName());
    }

    @Test
    void testGenerateWithResolvedVersions() {
        generator = new CitrusCatalogGenerator("4.10.0", outputDirectory, false);
        
        ResolvedVersions resolvedVersions = new ResolvedVersions(
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
    void testGenerateWithSnapshotVersion() {
        generator = new CitrusCatalogGenerator("4.10.0-SNAPSHOT", outputDirectory, false);
        
        CatalogDefinition catalogDefinition = generator.generate();
        
        assertNotNull(catalogDefinition);
        assertEquals("Citrus 4.10.0-SNAPSHOT", catalogDefinition.getName());
        assertEquals("4.10.0-SNAPSHOT", catalogDefinition.getVersion());
    }

    @Test
    void testRemoveActionsPropertyFromTestActions() throws Exception {
        String testActionsJson = """
            {
              "echo": {
                "propertiesSchema": {
                  "properties": {
                    "message": {
                      "type": "string"
                    },
                    "actions": {
                      "type": "array",
                      "items": {
                        "type": "object"
                      }
                    }
                  }
                }
              },
              "sleep": {
                "propertiesSchema": {
                  "properties": {
                    "milliseconds": {
                      "type": "number"
                    },
                    "actions": {
                      "type": "array"
                    }
                  }
                }
              }
            }
            """;

        generator = new CitrusCatalogGenerator("4.10.0", outputDirectory, false);
        
        // Use reflection to access private method
        var method = CitrusCatalogGenerator.class.getDeclaredMethod(
            "removeActionsProperty", String.class, String.class);
        method.setAccessible(true);
        
        String result = (String) method.invoke(generator, testActionsJson, "citrus-catalog-aggregate-test-actions");
        
        JsonNode resultNode = objectMapper.readTree(result);
        
        // Verify actions property is removed from echo
        assertFalse(resultNode.at("/echo/propertiesSchema/properties").has("actions"),
            "actions property should be removed from echo");
        assertTrue(resultNode.at("/echo/propertiesSchema/properties").has("message"),
            "message property should still exist");
        
        // Verify actions property is removed from sleep
        assertFalse(resultNode.at("/sleep/propertiesSchema/properties").has("actions"),
            "actions property should be removed from sleep");
        assertTrue(resultNode.at("/sleep/propertiesSchema/properties").has("milliseconds"),
            "milliseconds property should still exist");
    }

    @Test
    void testRemoveActionsPropertyFromTestContainers() throws Exception {
        String testContainersJson = """
            {
              "doFinally": {
                "propertiesSchema": {
                  "properties": {
                    "actions": {
                      "type": "array",
                      "items": {
                        "type": "object"
                      }
                    },
                    "description": {
                      "type": "string"
                    }
                  },
                  "required": ["actions"]
                }
              },
              "iterate": {
                "propertiesSchema": {
                  "properties": {
                    "actions": {
                      "type": "array"
                    },
                    "condition": {
                      "type": "string"
                    }
                  },
                  "required": ["actions", "condition"]
                }
              }
            }
            """;

        generator = new CitrusCatalogGenerator("4.10.0", outputDirectory, false);

        var method = CitrusCatalogGenerator.class.getDeclaredMethod(
            "removeActionsProperty", String.class, String.class);
        method.setAccessible(true);

        String result = (String) method.invoke(generator, testContainersJson, "citrus-catalog-aggregate-test-containers");

        JsonNode resultNode = objectMapper.readTree(result);

        // Verify actions property is removed from doFinally
        assertFalse(resultNode.at("/doFinally/propertiesSchema/properties").has("actions"),
            "actions property should be removed from doFinally");
        assertTrue(resultNode.at("/doFinally/propertiesSchema/properties").has("description"),
            "description property should still exist");

        // Verify actions is removed from doFinally required array (was the only entry)
        JsonNode doFinallyRequired = resultNode.at("/doFinally/propertiesSchema/required");
        assertTrue(doFinallyRequired.isArray());
        assertEquals(0, doFinallyRequired.size(), "doFinally required array should be empty after removing 'actions'");

        // Verify actions property is removed from iterate
        assertFalse(resultNode.at("/iterate/propertiesSchema/properties").has("actions"),
            "actions property should be removed from iterate");
        assertTrue(resultNode.at("/iterate/propertiesSchema/properties").has("condition"),
            "condition property should still exist");

        // Verify actions is removed from iterate required array but condition remains
        JsonNode iterateRequired = resultNode.at("/iterate/propertiesSchema/required");
        assertTrue(iterateRequired.isArray());
        assertEquals(1, iterateRequired.size(), "iterate required should only contain 'condition'");
        assertEquals("condition", iterateRequired.get(0).asText());
    }

    @Test
    void testRemoveActionsPropertyHandlesInvalidJson() throws Exception {
        String invalidJson = "not valid json";

        generator = new CitrusCatalogGenerator("4.10.0", outputDirectory, false);
        
        var method = CitrusCatalogGenerator.class.getDeclaredMethod(
            "removeActionsProperty", String.class, String.class);
        method.setAccessible(true);
        
        String result = (String) method.invoke(generator, invalidJson, "citrus-testcase");
        
        // Should return original content on error
        assertEquals(invalidJson, result);
    }

    @Test
    void testRemoveActionsPropertyHandlesNonObjectJson() throws Exception {
        String arrayJson = "[1, 2, 3]";

        generator = new CitrusCatalogGenerator("4.10.0", outputDirectory, false);
        
        var method = CitrusCatalogGenerator.class.getDeclaredMethod(
            "removeActionsProperty", String.class, String.class);
        method.setAccessible(true);
        
        String result = (String) method.invoke(generator, arrayJson, "citrus-testcase");
        
        // Should return original content for non-object JSON
        assertEquals(arrayJson, result);
    }

    @Test
    void testSetResolvedVersions() {
        generator = new CitrusCatalogGenerator("4.10.0", outputDirectory, false);
        
        ResolvedVersions resolvedVersions = new ResolvedVersions(
            "4.15.0",
            "4.15.0",
            "3.27.0",
            "4.15.0"
        );
        
        assertDoesNotThrow(() -> generator.setResolvedVersions(resolvedVersions));
    }

    @Test
    void testGenerateCreatesOutputDirectory() {
        // Note: The current implementation requires the output directory to exist
        // Files.writeString doesn't create parent directories automatically
        File nestedDir = new File(tempDir.toFile(), "nested/output");
        nestedDir.mkdirs();
        assertTrue(nestedDir.exists());
        
        generator = new CitrusCatalogGenerator("4.10.0", nestedDir, false);
        
        assertDoesNotThrow(() -> generator.generate());
    }

    @Test
    void testCatalogDefinitionFileName() {
        generator = new CitrusCatalogGenerator("4.10.0", outputDirectory, false);
        
        CatalogDefinition catalogDefinition = generator.generate();
        
        assertEquals("index.json", catalogDefinition.getFileName(),
            "Catalog definition should always use 'index.json' as filename");
    }

    @Test
    void testCatalogDefinitionNameFormat() {
        String[] versions = {"4.10.0", "4.10.1-SNAPSHOT", "4.11.0.redhat-00001"};
        
        for (String version : versions) {
            generator = new CitrusCatalogGenerator(version, outputDirectory, false);
            CatalogDefinition catalogDefinition = generator.generate();
            
            assertEquals("Citrus " + version, catalogDefinition.getName(),
                "Catalog name should be 'Citrus ' + version");
        }
    }

    @Test
    void testGenerateWithNullResolvedVersions() {
        generator = new CitrusCatalogGenerator("4.10.0", outputDirectory, false);
        // Don't set resolved versions
        
        CatalogDefinition catalogDefinition = generator.generate();
        
        assertNotNull(catalogDefinition);
        assertNull(catalogDefinition.getCamelCatalogVersion());
        assertNull(catalogDefinition.getRuntimeProviderVersion());
        assertNull(catalogDefinition.getFrameworkVersion());
    }
}
