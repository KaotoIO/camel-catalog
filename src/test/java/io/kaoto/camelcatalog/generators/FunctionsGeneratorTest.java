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

import com.fasterxml.jackson.databind.node.ObjectNode;
import io.kaoto.camelcatalog.TestLoggerHandler;
import io.kaoto.camelcatalog.maven.CamelCatalogVersionLoader;
import io.kaoto.camelcatalog.maven.ResourceLoader;
import io.kaoto.camelcatalog.model.CatalogRuntime;
import org.apache.camel.catalog.CamelCatalog;
import org.apache.camel.catalog.DefaultCamelCatalog;
import org.apache.camel.tooling.model.LanguageModel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class FunctionsGeneratorTest {
    FunctionsGenerator functionsGenerator;
    CamelCatalogVersionLoader camelCatalogVersionLoader;

    @BeforeEach
    void setUp() {
        CamelCatalog camelCatalog = new DefaultCamelCatalog();
        camelCatalogVersionLoader = new CamelCatalogVersionLoader(CatalogRuntime.Main, false);
        functionsGenerator = new FunctionsGenerator(camelCatalog, camelCatalogVersionLoader);
    }

    @Test
    void shouldContainAListOfLanguagesWithFunctions() {
        var functionsMap = functionsGenerator.generate();
        assertTrue(functionsMap.containsKey("simple"));
    }

    @Test
    void shouldContainFunctionsForSimpleLanguage() {
        var functionsMap = functionsGenerator.generate();
        var simpleNode = functionsMap.get("simple");
        assertNotNull(simpleNode);
        assertFalse(simpleNode.properties().isEmpty());
        assertTrue(simpleNode.has("bodyAs(type)"));
        assertTrue(simpleNode.has("header.name"));
    }

    @Test
    void shouldSkipLanguagesWithNoFunctions() {
        var functionsMap = functionsGenerator.generate();
        for (Map.Entry<String, ObjectNode> entry : functionsMap.entrySet()) {
            assertFalse(entry.getValue().isEmpty(),
                    "Language " + entry.getKey() + " should have at least one function");
        }
    }

    @Test
    void shouldMockLanguageModelGetFunctions() {
        LanguageModel.LanguageFunctionModel mockFunction = mock(LanguageModel.LanguageFunctionModel.class);
        when(mockFunction.getName()).thenReturn("mockFunc");
        when(mockFunction.getDisplayName()).thenReturn("Mock Function");
        when(mockFunction.getDescription()).thenReturn("Mock Desc");
        when(mockFunction.getJavaType()).thenReturn("Integer");

        LanguageModel mockLanguageModel = mock(LanguageModel.class);
        when(mockLanguageModel.getFunctions()).thenReturn(List.of(mockFunction));

        CamelCatalog realCatalog = new DefaultCamelCatalog();
        CamelCatalog spyCatalog = spy(realCatalog);
        when(spyCatalog.findLanguageNames()).thenReturn(List.of("mockLang"));
        when(spyCatalog.languageModel("mockLang")).thenReturn(mockLanguageModel);

        functionsGenerator = new FunctionsGenerator(spyCatalog, camelCatalogVersionLoader);
        Map<String, ObjectNode> result = functionsGenerator.generate();

        assertTrue(result.containsKey("mockLang"));

        ObjectNode node = result.get("mockLang");
        assertTrue(node.has("mockFunc"));
        assertEquals("Mock Function", node.get("mockFunc").get("displayName").asText());
        assertEquals("Mock Desc", node.get("mockFunc").get("description").asText());
        assertEquals("Integer", node.get("mockFunc").get("returnType").asText());
    }

    @Test
    void shouldLogWarningAndReturnEmptyOnInvalidSimpleLanguageJson() {
        CamelCatalogVersionLoader mockLoader = mock(CamelCatalogVersionLoader.class);

        var mockResourceLoader = mock(ResourceLoader.class);
        when(mockLoader.getResourceLoader()).thenReturn(mockResourceLoader);
        when(mockResourceLoader.getResourceAsString(anyString())).thenReturn("not-a-json");

        TestLoggerHandler mockLoggerHandler = new TestLoggerHandler();
        Logger logger = Logger.getLogger(FunctionsGenerator.class.getName());
        logger.setUseParentHandlers(false);
        logger.addHandler(mockLoggerHandler);

        // Mock catalog to exclude "simple" language so the fallback code is triggered
        CamelCatalog realCatalog = new DefaultCamelCatalog();
        CamelCatalog spyCatalog = spy(realCatalog);
        List<String> languagesWithoutSimple = realCatalog.findLanguageNames().stream()
                .filter(name -> !name.equals("simple"))
                .toList();
        when(spyCatalog.findLanguageNames()).thenReturn(languagesWithoutSimple);

        functionsGenerator = new FunctionsGenerator(spyCatalog, mockLoader);
        functionsGenerator.generate();

        // Should still contain other languages, but 'simple' should be empty or missing
        assertTrue(mockLoggerHandler.getRecords().stream()
                .anyMatch(msg -> msg.getMessage().contains("Failed to parse simple language functions JSON")));
    }

    @Test
    void shouldLogInfoWhenSimpleLanguageFunctionsNotFound() {
        var mockResourceLoader = mock(ResourceLoader.class);
        when(mockResourceLoader.getResourceAsString(anyString())).thenReturn("{}");

        CamelCatalogVersionLoader mockLoader = mock(CamelCatalogVersionLoader.class);
        when(mockLoader.getResourceLoader()).thenReturn(mockResourceLoader);

        TestLoggerHandler mockLoggerHandler = new TestLoggerHandler();
        Logger logger = Logger.getLogger(FunctionsGenerator.class.getName());
        logger.setUseParentHandlers(false);
        logger.addHandler(mockLoggerHandler);

        // Mock catalog to exclude "simple" language so the fallback code is triggered
        CamelCatalog realCatalog = new DefaultCamelCatalog();
        CamelCatalog spyCatalog = spy(realCatalog);
        List<String> languagesWithoutSimple = realCatalog.findLanguageNames().stream()
                .filter(name -> !name.equals("simple"))
                .toList();
        when(spyCatalog.findLanguageNames()).thenReturn(languagesWithoutSimple);

        functionsGenerator = new FunctionsGenerator(spyCatalog, mockLoader);
        functionsGenerator.generate();

        assertTrue(mockLoggerHandler.getRecords().stream().anyMatch(msg -> msg.getMessage()
                .contains("Simple language functions not found in the catalog, generating locally.")));
    }
}
