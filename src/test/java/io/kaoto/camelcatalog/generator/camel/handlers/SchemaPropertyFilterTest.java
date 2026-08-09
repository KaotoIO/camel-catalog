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
package io.kaoto.camelcatalog.generator.camel.handlers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SchemaPropertyFilterTest {
    private SchemaPropertyFilter schemaPropertyFilter;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        schemaPropertyFilter = new SchemaPropertyFilter();
        objectMapper = new ObjectMapper();
    }

    @Test
    void shouldFilterMultipleBlocklistedProperties() {
        ObjectNode node = createNodeWithProperties("when", "otherwise", "id", "description");

        schemaPropertyFilter.schemaPropertyFilter("choice", node);

        ObjectNode properties = (ObjectNode) node.get("properties");
        assertFalse(properties.has("when"), "Blocklisted property 'when' should be filtered");
        assertFalse(properties.has("otherwise"), "Blocklisted property 'otherwise' should be filtered");
        assertTrue(properties.has("id"), "Non-blocklisted property should remain");
        assertTrue(properties.has("description"), "Non-blocklisted property should remain");
    }

    @Test
    void shouldNotFilterWhenEipNotInBlocklist() {
        ObjectNode node = createNodeWithProperties("steps", "when", "otherwise", "id");

        schemaPropertyFilter.schemaPropertyFilter("unknownEip", node);

        ObjectNode properties = (ObjectNode) node.get("properties");
        assertTrue(properties.has("steps"), "All properties should remain when EIP not in blocklist");
        assertTrue(properties.has("when"), "All properties should remain when EIP not in blocklist");
        assertTrue(properties.has("otherwise"), "All properties should remain when EIP not in blocklist");
        assertTrue(properties.has("id"), "All properties should remain when EIP not in blocklist");
    }

    @Test
    void shouldFilterPropertiesInOneOfArray() {
        ObjectNode node = objectMapper.createObjectNode();
        ArrayNode oneOfArray = node.putArray("oneOf");

        ObjectNode option = objectMapper.createObjectNode();
        ObjectNode properties = option.putObject("properties");
        properties.putObject("steps");
        properties.putObject("id");
        oneOfArray.add(option);

        schemaPropertyFilter.schemaPropertyFilter("filter", node);

        ObjectNode resultProperties = (ObjectNode) oneOfArray.get(0).get("properties");
        assertFalse(resultProperties.has("steps"), "Blocklisted property should be filtered in oneOf");
        assertTrue(resultProperties.has("id"), "Non-blocklisted property should remain in oneOf");
    }

    @Test
    void shouldFilterPropertiesInAnyOfArray() {
        ObjectNode node = objectMapper.createObjectNode();
        ArrayNode anyOfArray = node.putArray("anyOf");

        ObjectNode option = objectMapper.createObjectNode();
        ObjectNode properties = option.putObject("properties");
        properties.putObject("steps");
        properties.putObject("id");
        anyOfArray.add(option);

        schemaPropertyFilter.schemaPropertyFilter("filter", node);

        ObjectNode resultProperties = (ObjectNode) anyOfArray.get(0).get("properties");
        assertFalse(resultProperties.has("steps"), "Blocklisted property should be filtered in anyOf");
        assertTrue(resultProperties.has("id"), "Non-blocklisted property should remain in anyOf");
    }

    /**
     * Helper method to create a node with properties
     */
    private ObjectNode createNodeWithProperties(String... propertyNames) {
        ObjectNode node = objectMapper.createObjectNode();
        ObjectNode properties = node.putObject("properties");
        for (String propertyName : propertyNames) {
            properties.putObject(propertyName);
        }
        return node;
    }
}
