/*
 * Copyright (C) 2025 Red Hat, Inc.
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
package io.kaoto.camelcatalog.generators;

import io.kaoto.camelcatalog.TestLoggerHandler;
import io.kaoto.camelcatalog.maven.CamelCatalogVersionLoader;
import io.kaoto.camelcatalog.model.CatalogRuntime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Map;
import java.util.logging.Logger;

import static io.kaoto.camelcatalog.model.Constants.CAMEL_YAML_DSL_FILE_NAME;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SchemasGeneratorTest {
    
    SchemasGenerator schemasGenerator;
    CamelCatalogVersionLoader versionLoader;
    ClassLoader classLoader;

    @BeforeEach
    void setUp() throws IOException {
        versionLoader = new CamelCatalogVersionLoader(CatalogRuntime.Main, false);
        versionLoader.loadCamelYamlDsl("4.12.0");
        versionLoader.loadCamelKCRDs("2.3.1");
        
        classLoader = versionLoader.getClassLoader();
        schemasGenerator = new SchemasGenerator(versionLoader, classLoader);
    }

    @Test
    void shouldContainCamelYamlDslSchema() {
        var schemasMap = schemasGenerator.generate();

        assertTrue(schemasMap.containsKey(CAMEL_YAML_DSL_FILE_NAME));
        
        String yamlDslSchema = schemasMap.get(CAMEL_YAML_DSL_FILE_NAME);
        assertNotNull(yamlDslSchema);
        assertFalse(yamlDslSchema.isEmpty());
        
        assertTrue(yamlDslSchema.contains("http://json-schema.org/draft-07/schema#"));
        assertFalse(yamlDslSchema.contains("http://json-schema.org/draft-04/schema#"));
    }

    @Test
    void shouldContainCRDSchemas() {
        var schemasMap = schemasGenerator.generate();

        // Check that CRD schemas are included
        assertTrue(schemasMap.containsKey("Integration"));
        assertTrue(schemasMap.containsKey("KameletBinding"));
        assertTrue(schemasMap.containsKey("Kamelet"));
        assertTrue(schemasMap.containsKey("Pipe"));
        
        // Verify content is valid JSON
        String integrationSchema = schemasMap.get("Integration");
        assertNotNull(integrationSchema);
        assertFalse(integrationSchema.isEmpty());
        assertTrue(integrationSchema.startsWith("{") || integrationSchema.startsWith("["));
    }

    @Test
    void shouldHandleXSDSchemas() {
        var schemasMap = schemasGenerator.generate();
        
        assertNotNull(schemasMap);

        assertEquals(7, schemasMap.size()); // 1 YAML DSL + 4 CRDs + 2 XML
    }

    @Test
    void shouldReturnEmptyMapWhenNoYamlDslSchemaLoaded() {
        CamelCatalogVersionLoader mockVersionLoader = mock(CamelCatalogVersionLoader.class);
        when(mockVersionLoader.getCamelYamlDslSchema()).thenReturn(null);
        when(mockVersionLoader.getCamelKCRDs()).thenReturn(java.util.Collections.emptyList());
        
        SchemasGenerator generator = new SchemasGenerator(mockVersionLoader, classLoader);
        var schemasMap = generator.generate();
        
        assertFalse(schemasMap.containsKey(CAMEL_YAML_DSL_FILE_NAME));
    }

    @Test
    void shouldLogWarningWhenYamlDslSchemaNotLoaded() {
        TestLoggerHandler mockLoggerHandler = new TestLoggerHandler();
        Logger logger = Logger.getLogger(SchemasGenerator.class.getName());
        logger.setUseParentHandlers(false);
        logger.addHandler(mockLoggerHandler);

        CamelCatalogVersionLoader mockVersionLoader = mock(CamelCatalogVersionLoader.class);
        when(mockVersionLoader.getCamelYamlDslSchema()).thenReturn(null);
        when(mockVersionLoader.getCamelKCRDs()).thenReturn(java.util.Collections.emptyList());
        
        SchemasGenerator generator = new SchemasGenerator(mockVersionLoader, classLoader);
        generator.generate();

        assertTrue(mockLoggerHandler.getRecords().stream()
                .anyMatch(msg -> msg.getMessage().contains("Camel YAML DSL schema is not loaded")),
                "Expected warning message not logged");
    }

    @Test
    void shouldLogWarningWhenCRDsNotLoaded() {
        TestLoggerHandler mockLoggerHandler = new TestLoggerHandler();
        Logger logger = Logger.getLogger(SchemasGenerator.class.getName());
        logger.setUseParentHandlers(false);
        logger.addHandler(mockLoggerHandler);

        CamelCatalogVersionLoader mockVersionLoader = mock(CamelCatalogVersionLoader.class);
        when(mockVersionLoader.getCamelYamlDslSchema()).thenReturn("{}");
        when(mockVersionLoader.getCamelKCRDs()).thenReturn(java.util.Collections.emptyList());
        
        SchemasGenerator generator = new SchemasGenerator(mockVersionLoader, classLoader);
        generator.generate();

        assertTrue(mockLoggerHandler.getRecords().stream()
                .anyMatch(msg -> msg.getMessage().contains("CamelK CRDs are not loaded")),
                "Expected warning message not logged");
    }

    @Test
    void shouldHandleIOExceptionsGracefully() {
        TestLoggerHandler mockLoggerHandler = new TestLoggerHandler();
        Logger logger = Logger.getLogger(SchemasGenerator.class.getName());
        logger.setUseParentHandlers(false);
        logger.addHandler(mockLoggerHandler);

        // Create a mock classloader that throws IOException
        ClassLoader mockClassLoader = mock(ClassLoader.class);
        try {
            when(mockClassLoader.getResources("org/apache/camel/catalog/schemas"))
                .thenThrow(new IOException("Test exception"));
        } catch (IOException e) {
            // This is expected in the mock setup
        }

        CamelCatalogVersionLoader mockVersionLoader = mock(CamelCatalogVersionLoader.class);
        when(mockVersionLoader.getCamelYamlDslSchema()).thenReturn("{}");
        when(mockVersionLoader.getCamelKCRDs()).thenReturn(java.util.Collections.emptyList());
        
        SchemasGenerator generator = new SchemasGenerator(mockVersionLoader, mockClassLoader);
        var schemasMap = generator.generate();

        assertTrue(schemasMap.containsKey(CAMEL_YAML_DSL_FILE_NAME));
        
        assertTrue(mockLoggerHandler.getRecords().stream()
                .anyMatch(msg -> msg.getMessage().contains("Error loading XSD schemas from classpath")),
                "Expected warning message not logged");
    }

    @Test
    void shouldReturnNonEmptyMapWithValidContent() {
        var schemasMap = schemasGenerator.generate();
        
        assertFalse(schemasMap.isEmpty());
        
        for (Map.Entry<String, String> entry : schemasMap.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            
            assertNotNull(key, "Schema key should not be null");
            assertFalse(key.isEmpty(), "Schema key should not be empty");
            assertNotNull(value, "Schema content should not be null for key: " + key);
            assertFalse(value.isEmpty(), "Schema content should not be empty for key: " + key);
        }
    }

    @Test
    void shouldPreserveDraft07SchemaFormat() {
        var schemasMap = schemasGenerator.generate();
        String yamlDslSchema = schemasMap.get(CAMEL_YAML_DSL_FILE_NAME);
        
        assertTrue(yamlDslSchema.contains("\"$schema\""));
        assertTrue(yamlDslSchema.contains("\"type\""));
        
        assertTrue(yamlDslSchema.startsWith("{"));
        assertTrue(yamlDslSchema.endsWith("}"));
    }

    @Test
    void shouldGenerateConsistentResults() {
        var firstCall = schemasGenerator.generate();
        var secondCall = schemasGenerator.generate();
        
        assertEquals(firstCall.size(), secondCall.size());
        
        for (String key : firstCall.keySet()) {
            assertTrue(secondCall.containsKey(key));
            assertEquals(firstCall.get(key), secondCall.get(key));
        }
    }
}
