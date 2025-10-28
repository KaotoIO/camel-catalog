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
package io.kaoto.camelcatalog;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.kaoto.camelcatalog.generators.ComponentGenerator;
import io.kaoto.camelcatalog.maven.CamelCatalogVersionLoader;
import io.kaoto.camelcatalog.model.CatalogRuntime;
import org.apache.camel.catalog.CamelCatalog;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for comparing different Camel runtime versions and validating catalog consistency.
 * This test is useful for debugging catalog generation issues across versions.
 *
 * Note: These tests load actual Camel catalogs from Maven, so each catalog loader creates
 * its own isolated classloader with the appropriate versions.
 */
public class RuntimeVersionComparisonTest {

    /**
     * Test loading Camel Main 4.12.0
     */
    @Test
    void testLoadCamelCatalog412() {
        CamelCatalogVersionLoader loader = new CamelCatalogVersionLoader(CatalogRuntime.Main, false);
        boolean loaded = loader.loadCamelCatalog("4.12.0");

        assertTrue(loaded, "Failed to load Camel catalog for Main version 4.12.0");
        assertNotNull(loader.getCamelCatalog());
        String version = loader.getCamelCatalog().getCatalogVersion();
        assertNotNull(version, "Catalog version should not be null");
        System.out.println("Loaded Camel catalog version: " + version);
    }

    /**
     * Test loading Camel Main 4.15.0
     */
    @Test
    void testLoadCamelCatalog415() {
        CamelCatalogVersionLoader loader = new CamelCatalogVersionLoader(CatalogRuntime.Main, false);
        boolean loaded = loader.loadCamelCatalog("4.15.0");

        assertTrue(loaded, "Failed to load Camel catalog for Main version 4.15.0");
        assertNotNull(loader.getCamelCatalog());
        String version = loader.getCamelCatalog().getCatalogVersion();
        assertNotNull(version, "Catalog version should not be null");
        System.out.println("Loaded Camel catalog version: " + version);
    }

    /**
     * Test that core components exist across different versions
     */
    @ParameterizedTest
    @CsvSource({
            "4.12.0",
            "4.15.0"
    })
    void testCoreComponentsExist(String version) {
        CamelCatalogVersionLoader loader = new CamelCatalogVersionLoader(CatalogRuntime.Main, false);
        loader.loadCamelCatalog(version);

        CamelCatalog catalog = loader.getCamelCatalog();
        ComponentGenerator generator = new ComponentGenerator(catalog, CatalogRuntime.Main);
        Map<String, ObjectNode> components = generator.generate();

        // Verify core components that should exist in all versions
        String[] coreComponents = {"file", "log", "timer", "direct", "seda"};

        for (String component : coreComponents) {
            assertTrue(components.containsKey(component),
                "Core component '" + component + "' not found in version " + version);
        }
    }

    /**
     * Test the file component properties across versions to detect schema changes
     */
    @Test
    void testFileComponentDifferencesBetweenVersions() {
        // Load Camel 4.12.0
        CamelCatalogVersionLoader loader412 = new CamelCatalogVersionLoader(CatalogRuntime.Main, false);
        loader412.loadCamelCatalog("4.12.0");
        ComponentGenerator generator412 = new ComponentGenerator(loader412.getCamelCatalog(), CatalogRuntime.Main);
        Map<String, ObjectNode> components412 = generator412.generate();

        // Load Camel 4.15.0
        CamelCatalogVersionLoader loader415 = new CamelCatalogVersionLoader(CatalogRuntime.Main, false);
        loader415.loadCamelCatalog("4.15.0");
        ComponentGenerator generator415 = new ComponentGenerator(loader415.getCamelCatalog(), CatalogRuntime.Main);
        Map<String, ObjectNode> components415 = generator415.generate();

        // Compare file component
        assertNotNull(components412.get("file"), "File component not found in 4.12.0");
        assertNotNull(components415.get("file"), "File component not found in 4.15.0");

        JsonNode file412 = components412.get("file");
        JsonNode file415 = components415.get("file");

        // Both should have property schemas
        assertTrue(file412.has("propertiesSchema"), "File component 4.12.0 missing propertiesSchema");
        assertTrue(file415.has("propertiesSchema"), "File component 4.15.0 missing propertiesSchema");

        // Get property names
        Set<String> props412 = getPropertyNames(file412.get("propertiesSchema"));
        Set<String> props415 = getPropertyNames(file415.get("propertiesSchema"));

        assertFalse(props412.isEmpty(), "File component 4.12.0 has no properties");
        assertFalse(props415.isEmpty(), "File component 4.15.0 has no properties");

        // Log differences for debugging
        System.out.println("File component comparison between 4.12.0 and 4.15.0:");
        System.out.println("Properties in 4.12.0: " + props412.size());
        System.out.println("Properties in 4.15.0: " + props415.size());

        Set<String> removedProps = props412.stream()
                .filter(p -> !props415.contains(p))
                .collect(Collectors.toSet());
        Set<String> addedProps = props415.stream()
                .filter(p -> !props412.contains(p))
                .collect(Collectors.toSet());

        if (!removedProps.isEmpty()) {
            System.out.println("Properties removed in 4.15.0: " + removedProps);
        }
        if (!addedProps.isEmpty()) {
            System.out.println("Properties added in 4.15.0: " + addedProps);
        }
    }

    /**
     * Test the timer component properties across versions to detect schema changes
     */
    @Test
    void testTimerComponentDifferencesBetweenVersions() {
        // Load Camel 4.12.0
        CamelCatalogVersionLoader loader412 = new CamelCatalogVersionLoader(CatalogRuntime.Main, false);
        loader412.loadCamelCatalog("4.12.0");
        ComponentGenerator generator412 = new ComponentGenerator(loader412.getCamelCatalog(), CatalogRuntime.Main);
        Map<String, ObjectNode> components412 = generator412.generate();

        // Load Camel 4.15.0
        CamelCatalogVersionLoader loader415 = new CamelCatalogVersionLoader(CatalogRuntime.Main, false);
        loader415.loadCamelCatalog("4.15.0");
        ComponentGenerator generator415 = new ComponentGenerator(loader415.getCamelCatalog(), CatalogRuntime.Main);
        Map<String, ObjectNode> components415 = generator415.generate();

        // Compare timer component
        assertNotNull(components412.get("timer"), "Timer component not found in 4.12.0");
        assertNotNull(components415.get("timer"), "Timer component not found in 4.15.0");

        JsonNode timer412 = components412.get("timer");
        JsonNode timer415 = components415.get("timer");

        // Both should have property schemas
        assertTrue(timer412.has("propertiesSchema"), "Timer component 4.12.0 missing propertiesSchema");
        assertTrue(timer415.has("propertiesSchema"), "Timer component 4.15.0 missing propertiesSchema");

        // Check common properties exist
        JsonNode schema412 = timer412.get("propertiesSchema");
        JsonNode schema415 = timer415.get("propertiesSchema");

        // Timer should have these core properties in both versions
        String[] coreTimerProps = {"timerName", "delay", "period", "repeatCount"};

        for (String prop : coreTimerProps) {
            assertTrue(hasProperty(schema412, prop),
                "Timer 4.12.0 missing core property: " + prop);
            assertTrue(hasProperty(schema415, prop),
                "Timer 4.15.0 missing core property: " + prop);
        }

        // Get property names
        Set<String> props412 = getPropertyNames(schema412);
        Set<String> props415 = getPropertyNames(schema415);

        // Log differences for debugging
        System.out.println("Timer component comparison between 4.12.0 and 4.15.0:");
        System.out.println("Properties in 4.12.0: " + props412.size());
        System.out.println("Properties in 4.15.0: " + props415.size());

        Set<String> removedProps = props412.stream()
                .filter(p -> !props415.contains(p))
                .collect(Collectors.toSet());
        Set<String> addedProps = props415.stream()
                .filter(p -> !props412.contains(p))
                .collect(Collectors.toSet());

        if (!removedProps.isEmpty()) {
            System.out.println("Properties removed in 4.15.0: " + removedProps);
        }
        if (!addedProps.isEmpty()) {
            System.out.println("Properties added in 4.15.0: " + addedProps);
        }
    }

    /**
     * Test component count differences between versions
     */
    @Test
    void testComponentCountAcrossVersions() {
        // Load Camel 4.12.0
        CamelCatalogVersionLoader loader412 = new CamelCatalogVersionLoader(CatalogRuntime.Main, false);
        loader412.loadCamelCatalog("4.12.0");
        ComponentGenerator generator412 = new ComponentGenerator(loader412.getCamelCatalog(), CatalogRuntime.Main);
        Map<String, ObjectNode> components412 = generator412.generate();

        // Load Camel 4.15.0
        CamelCatalogVersionLoader loader415 = new CamelCatalogVersionLoader(CatalogRuntime.Main, false);
        loader415.loadCamelCatalog("4.15.0");
        ComponentGenerator generator415 = new ComponentGenerator(loader415.getCamelCatalog(), CatalogRuntime.Main);
        Map<String, ObjectNode> components415 = generator415.generate();

        int count412 = components412.size();
        int count415 = components415.size();

        System.out.println("Component count comparison:");
        System.out.println("4.12.0: " + count412 + " components");
        System.out.println("4.15.0: " + count415 + " components");

        // Both versions should have a reasonable number of components
        assertTrue(count412 > 100, "4.12.0 should have more than 100 components");
        assertTrue(count415 > 100, "4.15.0 should have more than 100 components");

        // Find components that were added or removed
        Set<String> components412Names = components412.keySet();
        Set<String> components415Names = components415.keySet();

        Set<String> removedComponents = components412Names.stream()
                .filter(c -> !components415Names.contains(c))
                .collect(Collectors.toSet());
        Set<String> addedComponents = components415Names.stream()
                .filter(c -> !components412Names.contains(c))
                .collect(Collectors.toSet());

        if (!removedComponents.isEmpty()) {
            System.out.println("Components removed in 4.15.0: " + removedComponents);
        }
        if (!addedComponents.isEmpty()) {
            System.out.println("Components added in 4.15.0: " + addedComponents);
        }
    }

    /**
     * Test catalog runtime metadata is correctly set
     */
    @Test
    void testCatalogRuntimeMetadata() {
        CamelCatalogVersionLoader loader = new CamelCatalogVersionLoader(CatalogRuntime.Main, false);
        loader.loadCamelCatalog("4.15.0");

        assertEquals(CatalogRuntime.Main, loader.getRuntime());
        assertNotNull(loader.getCamelCatalog().getCatalogVersion());
    }

    /**
     * Test different runtimes with the same Camel version
     */
    @ParameterizedTest
    @CsvSource({
            "Main, 4.12.0",
            "Quarkus, 3.16.0",
            "SpringBoot, 4.12.0"
    })
    void testDifferentRuntimesLoad(String runtimeStr, String version) {
        CatalogRuntime runtime = CatalogRuntime.fromString(runtimeStr);
        CamelCatalogVersionLoader loader = new CamelCatalogVersionLoader(runtime, false);

        boolean loaded = loader.loadCamelCatalog(version);

        assertTrue(loaded, "Failed to load " + runtimeStr + " catalog version " + version);
        assertEquals(runtime, loader.getRuntime());
        assertNotNull(loader.getCamelCatalog().getRuntimeProvider());
    }

    /**
     * Test Camel YAML DSL schema changes between versions.
     * This test specifically validates the breaking change in YAMLDataFormat's typeFilter property
     * where it changed from an array type in 4.12.0 to a string type in 4.15.0.
     *
     * Note: This test loads both schemas within the same test method to ensure they can be compared.
     * Each loader creates its own isolated classloader with the appropriate version.
     */
    @Test
    void testCamelYamlDslSchemaBreakingChanges() throws Exception {
        ObjectMapper mapper = new ObjectMapper();

        // Load Camel 4.12.0 YAML DSL schema first
        CamelCatalogVersionLoader loader412 = new CamelCatalogVersionLoader(CatalogRuntime.Main, false);
        loader412.loadCamelCatalog("4.12.0");
        boolean loaded412 = loader412.loadCamelYamlDsl("4.12.0");
        assertTrue(loaded412, "Failed to load YAML DSL for 4.12.0");
        String yamlDsl412 = loader412.getCamelYamlDslSchema();
        assertNotNull(yamlDsl412, "YAML DSL schema for 4.12.0 should not be null");

        // Parse 4.12.0 schema immediately to avoid classloader conflicts
        JsonNode schema412 = mapper.readTree(yamlDsl412);

        // Load Camel 4.15.0 YAML DSL schema
        CamelCatalogVersionLoader loader415 = new CamelCatalogVersionLoader(CatalogRuntime.Main, false);
        loader415.loadCamelCatalog("4.15.0");
        boolean loaded415 = loader415.loadCamelYamlDsl("4.15.0");
        assertTrue(loaded415, "Failed to load YAML DSL for 4.15.0");
        String yamlDsl415 = loader415.getCamelYamlDslSchema();
        assertNotNull(yamlDsl415, "YAML DSL schema for 4.15.0 should not be null");

        // Parse 4.15.0 schema
        JsonNode schema415 = mapper.readTree(yamlDsl415);

        // Navigate to YAMLDataFormat definition
        JsonNode definitions412 = schema412.get("items").get("definitions");
        JsonNode definitions415 = schema415.get("items").get("definitions");

        assertNotNull(definitions412, "Definitions should exist in 4.12.0 schema");
        assertNotNull(definitions415, "Definitions should exist in 4.15.0 schema");

        // Get YAMLDataFormat definition
        JsonNode yamlDataFormat412 = definitions412.get("org.apache.camel.model.dataformat.YAMLDataFormat");
        JsonNode yamlDataFormat415 = definitions415.get("org.apache.camel.model.dataformat.YAMLDataFormat");

        assertNotNull(yamlDataFormat412, "YAMLDataFormat should exist in 4.12.0 schema");
        assertNotNull(yamlDataFormat415, "YAMLDataFormat should exist in 4.15.0 schema");

        // Get the typeFilter property
        JsonNode typeFilter412 = yamlDataFormat412.get("properties").get("typeFilter");
        JsonNode typeFilter415 = yamlDataFormat415.get("properties").get("typeFilter");

        assertNotNull(typeFilter412, "typeFilter property should exist in 4.12.0");
        assertNotNull(typeFilter415, "typeFilter property should exist in 4.15.0");

        System.out.println("=== YAMLDataFormat typeFilter Breaking Change ===");
        System.out.println("4.12.0 typeFilter type: " + typeFilter412.get("type").asText());
        System.out.println("4.15.0 typeFilter type: " + typeFilter415.get("type").asText());

        // Validate the breaking change in 4.12.0
        assertEquals("array", typeFilter412.get("type").asText(),
                "In 4.12.0, typeFilter should be an array type");
        assertTrue(typeFilter412.has("items"),
                "In 4.12.0, typeFilter should have an items property");
        JsonNode items412 = typeFilter412.get("items");
        assertTrue(items412.has("$ref"),
                "In 4.12.0, typeFilter items should have a $ref to YAMLTypeFilterDefinition");
        String ref412 = items412.get("$ref").asText();
        assertTrue(ref412.contains("YAMLTypeFilterDefinition"),
                "In 4.12.0, typeFilter should reference YAMLTypeFilterDefinition, but got: " + ref412);

        System.out.println("4.12.0 typeFilter items.$ref: " + ref412);

        // Validate the simplified version in 4.15.0
        assertEquals("string", typeFilter415.get("type").asText(),
                "In 4.15.0, typeFilter should be simplified to a string type");
        assertFalse(typeFilter415.has("items"),
                "In 4.15.0, typeFilter should NOT have an items property");

        // Verify descriptions mention the change
        String desc415 = typeFilter415.get("description").asText();
        assertTrue(desc415.contains("comma") || desc415.contains("separated"),
                "In 4.15.0, description should mention comma-separated values: " + desc415);

        System.out.println("4.15.0 typeFilter description: " + desc415);
        System.out.println("Breaking change detected: typeFilter changed from array to comma-separated string");
    }

    /**
     * Test to verify that both versions can load their YAML DSL schemas
     */
    @ParameterizedTest
    @CsvSource({
            "4.12.0",
            "4.15.0"
    })
    void testYamlDslSchemaLoads(String version) {
        CamelCatalogVersionLoader loader = new CamelCatalogVersionLoader(CatalogRuntime.Main, false);
        loader.loadCamelCatalog(version);
        boolean loaded = loader.loadCamelYamlDsl(version);

        assertTrue(loaded, "Failed to load YAML DSL schema for version " + version);
        assertNotNull(loader.getCamelYamlDslSchema(), "YAML DSL schema should not be null for version " + version);

        String schema = loader.getCamelYamlDslSchema();
        assertTrue(schema.length() > 1000, "YAML DSL schema should be substantial (>1000 chars) for version " + version);
        assertTrue(schema.contains("\"$schema\""), "YAML DSL schema should contain $schema property for version " + version);

        System.out.println("Successfully loaded YAML DSL schema for " + version + " (" + schema.length() + " chars)");
    }

    // Helper methods

    /**
     * Extract property names from a JSON schema
     */
    private Set<String> getPropertyNames(JsonNode schema) {
        if (!schema.has("properties")) {
            return Set.of();
        }

        JsonNode properties = schema.get("properties");
        return Set.of(properties.fieldNames())
                .stream()
                .flatMap(iter -> {
                    java.util.List<String> names = new java.util.ArrayList<>();
                    iter.forEachRemaining(names::add);
                    return names.stream();
                })
                .collect(Collectors.toSet());
    }

    /**
     * Check if a schema has a specific property
     */
    private boolean hasProperty(JsonNode schema, String propertyName) {
        if (!schema.has("properties")) {
            return false;
        }
        return schema.get("properties").has(propertyName);
    }
}
