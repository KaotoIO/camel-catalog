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
package io.kaoto.camelcatalog.generator.camel;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.camel.dsl.yaml.YamlRoutesBuilderLoader;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CamelYamlDslSchemaProcessorTest {
    private ObjectMapper jsonMapper;
    private CamelYamlDslSchemaProcessor processor;

    @BeforeEach
    void setUp() throws Exception {
        jsonMapper = new ObjectMapper();
        var is = YamlRoutesBuilderLoader.class.getClassLoader().getResourceAsStream("schema/camelYamlDsl.json");
        ObjectNode yamlDslSchema = (ObjectNode) jsonMapper.readTree(is);

        processor = new CamelYamlDslSchemaProcessor(jsonMapper, yamlDslSchema);
    }

    @Test
    void testGetDataFormats() throws Exception {
        var dataFormatMap = processor.getDataFormats();
        assertTrue(dataFormatMap.size() > 30 && dataFormatMap.size() < 50);
        var customDataFormat = dataFormatMap.get("custom");
        assertEquals("Custom", customDataFormat.get("title").asText());
        var refProperty = customDataFormat.withObject("/properties").withObject("/ref");
        assertEquals("Ref", refProperty.get("title").asText());
        var jsonDataFormat = dataFormatMap.get("json");
        assertEquals("JSon", jsonDataFormat.get("title").asText());
        var libraryEnum = jsonDataFormat.withObject("/properties").withObject("/library").withArray("enum");
        assertTrue(libraryEnum.size() > 3);
    }

    @Test
    void testGetDataFormatYaml() throws Exception {
        var dataFormatMap = processor.getDataFormats();
        var yamlDataFormat = dataFormatMap.get("yaml");
        var typeFilterDefinition = yamlDataFormat.withObject("/properties").withObject("/typeFilter");
        assertEquals("string", typeFilterDefinition.get("type").asText());
        assertEquals("Type Filter", typeFilterDefinition.get("title").asText());
        assertEquals("Set the types SnakeYAML is allowed to un-marshall. Multiple types can be separated by comma.", typeFilterDefinition.get("description").asText());
    }

    @Test
    void testGetLanguages() throws Exception {
        var languageMap = processor.getLanguages();
        assertTrue(languageMap.size() > 20 && languageMap.size() < 30);
        var customLanguage = languageMap.get("language");
        assertEquals("Language", customLanguage.get("title").asText());
        var languageProperty = customLanguage.withObject("/properties").withObject("/language");
        assertEquals("Language", languageProperty.get("title").asText());
        var jqLanguage = languageMap.get("jq");
        assertEquals("JQ", jqLanguage.get("title").asText());
        var expressionProperty = jqLanguage.withObject("/properties").withObject("/expression");
        assertEquals("Expression", expressionProperty.get("title").asText());
    }

    @Test
    void testGetLoadBalancers() throws Exception {
        var lbMap = processor.getLoadBalancers();
        assertTrue(lbMap.containsKey("customLoadBalancer"));
        var customLb = lbMap.get("customLoadBalancer");
        assertEquals("Custom Load Balancer", customLb.get("title").asText());
        var customLbRefProp = customLb.withObject("/properties/ref");
        assertEquals("string", customLbRefProp.get("type").asText());
        assertEquals("Ref", customLbRefProp.get("title").asText());
    }
}
