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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.camel.catalog.CamelCatalog;
import org.apache.camel.catalog.DefaultCamelCatalog;
import org.apache.camel.dsl.yaml.YamlRoutesBuilderLoader;
import org.apache.camel.tooling.model.EipModel;
import org.apache.camel.tooling.model.Kind;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

class CamelCatalogSchemaEnhancerTest {
    private CamelCatalog camelCatalog;
    private CamelCatalogSchemaEnhancer camelCatalogSchemaEnhancer;
    private ObjectNode camelYamlDslSchema;

    @BeforeEach
    void setUp() throws Exception {
        camelCatalog = new DefaultCamelCatalog();
        camelCatalogSchemaEnhancer = new CamelCatalogSchemaEnhancer(camelCatalog);

        var is = YamlRoutesBuilderLoader.class.getClassLoader().getResourceAsStream("schema/camelYamlDsl.json");
        ObjectMapper jsonMapper = new ObjectMapper();
        camelYamlDslSchema = (ObjectNode) jsonMapper.readTree(is);
    }

    @Test
    void shouldFillSchemaInformation() {
        var setHeaderNode = (ObjectNode) camelYamlDslSchema
                .get("items")
                .get("definitions")
                .get("org.apache.camel.model.SetHeaderDefinition").deepCopy();

        camelCatalogSchemaEnhancer.fillSchemaInformation(setHeaderNode);

        assertTrue(setHeaderNode.has("$schema"));
        assertTrue(setHeaderNode.has("type"));
        assertEquals("http://json-schema.org/draft-07/schema#", setHeaderNode.get("$schema").asText());
        assertEquals("object", setHeaderNode.get("type").asText());
    }

    @Test
    void shouldFillRequiredPropertiesIfNeededForModelName() {
        var setHeaderNode = (ObjectNode) camelYamlDslSchema
                .get("items")
                .get("definitions")
                .get("org.apache.camel.model.SetHeaderDefinition").deepCopy();

        // remove required property to simulate the case where it is not present
        setHeaderNode.remove("required");

        camelCatalogSchemaEnhancer.fillRequiredPropertiesIfNeeded(Kind.eip, "setHeader", setHeaderNode);

        assertTrue(setHeaderNode.has("required"));
        assertEquals(1, setHeaderNode.get("required").size());

        List<String> requiredProperties = new ArrayList<>();
        setHeaderNode.withArray("required").elements()
                .forEachRemaining(node -> requiredProperties.add(node.asText()));

        assertTrue(requiredProperties.contains("name"));
    }

    @Test
    void shouldFillRequiredPropertiesIfNeededForModel() {
        var setHeaderNode = (ObjectNode) camelYamlDslSchema
                .get("items")
                .get("definitions")
                .get("org.apache.camel.model.SetHeaderDefinition").deepCopy();

        // remove required property to simulate the case where it is not present
        setHeaderNode.remove("required");

        EipModel model = camelCatalog.eipModel("setHeader");
        camelCatalogSchemaEnhancer.fillRequiredPropertiesIfNeeded(model, setHeaderNode);

        assertTrue(setHeaderNode.has("required"));
        assertEquals(1, setHeaderNode.get("required").size());

        List<String> requiredProperties = new ArrayList<>();
        setHeaderNode.withArray("required").elements()
                .forEachRemaining(node -> requiredProperties.add(node.asText()));

        assertTrue(requiredProperties.contains("name"));
    }

    @Test
    void shouldFillGroupInformationForModel() {
        var choiceNode = camelYamlDslSchema
                .withObject("items")
                .withObject("definitions")
                .withObject("org.apache.camel.model.ChoiceDefinition");

        EipModel model = camelCatalog.eipModel("choice");
        camelCatalogSchemaEnhancer.fillPropertiesInformation(model, choiceNode);

        var idPropertyNode = choiceNode.withObject("properties").withObject("id");
        var preconditionPropertyNode = choiceNode.withObject("properties").withObject("precondition");

        assertTrue(idPropertyNode.has("$comment"));
        assertTrue(preconditionPropertyNode.has("$comment"));
        assertEquals("group:common", idPropertyNode.get("$comment").asText());
        assertEquals("group:advanced", preconditionPropertyNode.get("$comment").asText());
    }

    @Test
    void shouldFillGroupInformationForModelName() {
        var setHeaderNode = camelYamlDslSchema
                .withObject("items")
                .withObject("definitions")
                .withObject("org.apache.camel.model.SetHeaderDefinition");

        camelCatalogSchemaEnhancer.fillPropertiesInformation("setHeader", setHeaderNode);

        var expressionPropertyNode = setHeaderNode.withObject("properties").withObject("expression");
        var idPropertyNode = setHeaderNode.withObject("properties").withObject("id");
        var disabledPropertyNode = setHeaderNode.withObject("properties").withObject("disabled");

        assertFalse(expressionPropertyNode.has("$comment"));
        assertTrue(idPropertyNode.has("$comment"));
        assertTrue(disabledPropertyNode.has("$comment"));
        assertEquals("group:common", idPropertyNode.get("$comment").asText());
        assertEquals("group:advanced", disabledPropertyNode.get("$comment").asText());
    }

    @Test
    void shouldFillFormatInformationForModel() {
        var aggregateNode = camelYamlDslSchema
                .withObject("items")
                .withObject("definitions")
                .withObject("org.apache.camel.model.AggregateDefinition");

        EipModel model = camelCatalog.eipModel("aggregate");
        camelCatalogSchemaEnhancer.fillPropertiesInformation(model, aggregateNode);

        var executorServicePropertyNode = aggregateNode.withObject("properties")
                .withObject("executorService");
        var timeoutCheckerExecutorServicePropertyNode = aggregateNode.withObject("properties")
                .withObject("timeoutCheckerExecutorService");

        assertTrue(executorServicePropertyNode.has("format"));
        assertTrue(timeoutCheckerExecutorServicePropertyNode.has("format"));
        assertEquals("bean:java.util.concurrent.ExecutorService", executorServicePropertyNode.get("format").asText());
        assertEquals("bean:java.util.concurrent.ScheduledExecutorService",
                timeoutCheckerExecutorServicePropertyNode.get("format").asText());
    }

    @Test
    void shouldFillExpressionInformationForModelName() {
        ObjectNode aggregateNode = camelYamlDslSchema
                .withObject("items")
                .withObject("definitions")
                .withObject("org.apache.camel.model.AggregateDefinition").deepCopy();

        CamelCatalogSchemaEnhancer camelCatalogSchemaEnhancerSpy = spy(camelCatalogSchemaEnhancer);

        camelCatalogSchemaEnhancerSpy.fillPropertiesInformation("aggregate", aggregateNode);

        ArgumentCaptor<EipModel> modelCaptor = ArgumentCaptor.forClass(EipModel.class);
        ArgumentCaptor<ObjectNode> modelNodeCaptor = ArgumentCaptor.forClass(ObjectNode.class);

        verify(camelCatalogSchemaEnhancerSpy).fillPropertiesInformation(modelCaptor.capture(),
                modelNodeCaptor.capture());

        assertInstanceOf(EipModel.class, modelCaptor.getValue());
        assertEquals(aggregateNode, modelNodeCaptor.getValue());
    }

    @Test
    void shouldFillExpressionInformationForModel() {
        var aggregateNode = camelYamlDslSchema
                .withObject("items")
                .withObject("definitions")
                .withObject("org.apache.camel.model.AggregateDefinition");

        EipModel model = camelCatalog.eipModel("aggregate");
        camelCatalogSchemaEnhancer.fillPropertiesInformation(model, aggregateNode);

        var correlationExpressionPropertyNode = aggregateNode.withObject("properties")
                .withObject("correlationExpression");

        assertTrue(correlationExpressionPropertyNode.has("format"));
        assertEquals("expressionProperty", correlationExpressionPropertyNode.get("format").asText());
    }

    @Test
    void shouldFillFormatInformationForModelName() {
        var aggregateNode = camelYamlDslSchema
                .withObject("items")
                .withObject("definitions")
                .withObject("org.apache.camel.model.AggregateDefinition");

        camelCatalogSchemaEnhancer.fillPropertiesInformation("aggregate", aggregateNode);

        var executorServicePropertyNode = aggregateNode.withObject("properties")
                .withObject("executorService");
        var timeoutCheckerExecutorServicePropertyNode = aggregateNode.withObject("properties")
                .withObject("timeoutCheckerExecutorService");

        assertTrue(executorServicePropertyNode.has("format"));
        assertTrue(timeoutCheckerExecutorServicePropertyNode.has("format"));
        assertEquals("bean:java.util.concurrent.ExecutorService", executorServicePropertyNode.get("format").asText());
        assertEquals("bean:java.util.concurrent.ScheduledExecutorService",
                timeoutCheckerExecutorServicePropertyNode.get("format").asText());
    }

    @Test
    void shouldFillDeprecatedInformationForEIPModelName() {
        var recipientListNode = camelYamlDslSchema
                .withObject("items")
                .withObject("definitions")
                .withObject("org.apache.camel.model.RecipientListDefinition");

        camelCatalogSchemaEnhancer.fillPropertiesInformation("RecipientList", recipientListNode);

        var parallelAggregateNode = recipientListNode.withObject("properties").withObject("parallelAggregate");

        assertTrue(parallelAggregateNode.has("deprecated"));
        assertTrue(parallelAggregateNode.get("deprecated").asBoolean());
    }

    @Test
    void shouldFillDefaultInformationForEIPModelName() {
        var multicastNode = camelYamlDslSchema
                .withObject("items")
                .withObject("definitions")
                .withObject("org.apache.camel.model.MulticastDefinition");

        camelCatalogSchemaEnhancer.fillPropertiesInformation("multicast", multicastNode);

        var timeoutNode = multicastNode.withObject("properties").withObject("timeout");

        assertTrue(timeoutNode.has("default"));
        assertEquals("0", timeoutNode.get("default").asText());
    }

    @Test
    void shouldSortPropertiesAccordingToCatalogForModelName() {
        var choiceNode = (ObjectNode) camelYamlDslSchema
                .get("items")
                .get("definitions")
                .get("org.apache.camel.model.ChoiceDefinition").deepCopy();

        camelCatalogSchemaEnhancer.sortPropertiesAccordingToCatalog("choice", choiceNode);

        assertTrue(choiceNode.has("properties"));
        List<String> actualKeys = choiceNode.withObject("/properties").properties().stream()
                .map(Map.Entry::getKey).toList();
        assertEquals(List.of("id", "description", "disabled", "when", "otherwise", "precondition"), actualKeys);
    }

    @Test
    void shouldSortPropertiesAccordingToCatalogForModel() {
        var choiceNode = (ObjectNode) camelYamlDslSchema
                .get("items")
                .get("definitions")
                .get("org.apache.camel.model.ChoiceDefinition").deepCopy();

        EipModel model = camelCatalog.eipModel("choice");
        camelCatalogSchemaEnhancer.sortPropertiesAccordingToCatalog(model, choiceNode);

        assertTrue(choiceNode.has("properties"));
        List<String> actualKeys = choiceNode.withObject("/properties").properties().stream()
                .map(Map.Entry::getKey).toList();
        assertEquals(List.of("id", "description", "disabled", "when", "otherwise", "precondition"), actualKeys);
    }

    @Test
    void shouldGetCamelModelByJavaType() {
        EipModel setHeaderModel =
                camelCatalogSchemaEnhancer.getCamelModelByJavaType("org.apache.camel.model.SetHeaderDefinition");

        assertNotNull(setHeaderModel);
        assertEquals("setHeader", setHeaderModel.getName());
        assertFalse(setHeaderModel.getOptions().isEmpty());
    }

    @Test
    void shouldSetExpressionFormatInExpressionAnyOfFromSetHeader() {
        ObjectNode setHeaderNode = camelYamlDslSchema
                .withObject("items")
                .withObject("definitions")
                .withObject("org.apache.camel.model.SetHeaderDefinition");

        camelCatalogSchemaEnhancer.fillModelFormatInOneOf(setHeaderNode);

        var firstOneOfNode = setHeaderNode.withArray("anyOf").get(0);

        assertTrue(firstOneOfNode.has("format"));
        assertTrue(firstOneOfNode.get("format").asText().contains("expression"));
    }

    @Test
    void shouldSetExpressionFormatInExpressionAnyOfFromResequence() {
        ObjectNode setHeaderNode = camelYamlDslSchema
                .withObject("items")
                .withObject("definitions")
                .withObject("org.apache.camel.model.ResequenceDefinition");

        camelCatalogSchemaEnhancer.fillModelFormatInOneOf(setHeaderNode);

        /* The first anyOf node is the one that contains the expression property */
        var firstOneOfNode = setHeaderNode.withArray("anyOf").get(0);
        /* The second anyOf node is the one that contains the batchConfig or streamConfig */
        var secondOneOfNode = setHeaderNode.withArray("anyOf").get(1);

        assertTrue(firstOneOfNode.has("format"));
        assertTrue(firstOneOfNode.get("format").asText().contains("expression"));
        assertFalse(secondOneOfNode.has("format"));
    }

    @Test
    void shouldSetExpressionFormatInExpressionAnyOfFromSaga() {
        ObjectNode propertyExpressionNode = camelYamlDslSchema
                .withObject("items")
                .withObject("definitions")
                .withObject("org.apache.camel.model.PropertyExpressionDefinition");

        camelCatalogSchemaEnhancer.fillModelFormatInOneOf(propertyExpressionNode);

        var firstOneOfNode = propertyExpressionNode.withArray("anyOf").get(0);

        assertTrue(firstOneOfNode.has("format"));
        assertTrue(firstOneOfNode.get("format").asText().contains("expression"));
    }

    @Test
    void shouldSetFormatInAnyOfFromMarshal() {
        ObjectNode propertyNode = camelYamlDslSchema
                .withObject("items")
                .withObject("definitions")
                .withObject("org.apache.camel.model.MarshalDefinition");

        camelCatalogSchemaEnhancer.fillModelFormatInOneOf(propertyNode);

        var firstOneOfNode = propertyNode.withArray("anyOf").get(0);

        assertTrue(firstOneOfNode.has("format"));
        assertTrue(firstOneOfNode.get("format").asText().contains("dataFormatType"));
    }

    @Test
    void shouldSetFormatInAnyOfFromLoadBalance() {
        ObjectNode propertyNode = camelYamlDslSchema
                .withObject("items")
                .withObject("definitions")
                .withObject("org.apache.camel.model.LoadBalanceDefinition");

        camelCatalogSchemaEnhancer.fillModelFormatInOneOf(propertyNode);

        var firstOneOfNode = propertyNode.withArray("anyOf").get(0);

        assertTrue(firstOneOfNode.has("format"));
        assertTrue(firstOneOfNode.get("format").asText().contains("loadBalancerType"));
    }

    @Test
    void shouldSetFormatInAnyOfFromErrorHandler() {
        ObjectNode propertyNode = camelYamlDslSchema
                .withObject("items")
                .withObject("definitions")
                .withObject("org.apache.camel.model.ErrorHandlerDefinition");

        camelCatalogSchemaEnhancer.fillModelFormatInOneOf(propertyNode);

        var firstOneOfNode = propertyNode.withArray("anyOf").get(0);

        assertTrue(firstOneOfNode.has("format"));
        assertTrue(firstOneOfNode.get("format").asText().contains("errorHandlerType"));
    }

    @Test
    void shouldFixBooleanDefaultValueFromString() {
        ObjectMapper jsonMapper = new ObjectMapper();
        ObjectNode schemaNode = jsonMapper.createObjectNode();
        ObjectNode propertiesNode = jsonMapper.createObjectNode();
        ObjectNode propertyNode = jsonMapper.createObjectNode();

        propertyNode.put("type", "boolean");
        propertyNode.put("default", "true");
        propertiesNode.set("testProperty", propertyNode);
        schemaNode.set("properties", propertiesNode);

        camelCatalogSchemaEnhancer.fixDefaultValueTypesFromCamelSchema(schemaNode);

        var defaultValue = propertyNode.get("default");
        assertTrue(defaultValue.isBoolean());
        assertTrue(defaultValue.asBoolean());
    }

    @Test
    void shouldFixBooleanDefaultValueFalseFromString() {
        ObjectMapper jsonMapper = new ObjectMapper();
        ObjectNode schemaNode = jsonMapper.createObjectNode();
        ObjectNode propertiesNode = jsonMapper.createObjectNode();
        ObjectNode propertyNode = jsonMapper.createObjectNode();

        propertyNode.put("type", "boolean");
        propertyNode.put("default", "false");
        propertiesNode.set("testProperty", propertyNode);
        schemaNode.set("properties", propertiesNode);

        camelCatalogSchemaEnhancer.fixDefaultValueTypesFromCamelSchema(schemaNode);

        var defaultValue = propertyNode.get("default");
        assertTrue(defaultValue.isBoolean());
        assertFalse(defaultValue.asBoolean());
    }

    @Test
    void shouldFixIntegerDefaultValueFromString() {
        ObjectMapper jsonMapper = new ObjectMapper();
        ObjectNode schemaNode = jsonMapper.createObjectNode();
        ObjectNode propertiesNode = jsonMapper.createObjectNode();
        ObjectNode propertyNode = jsonMapper.createObjectNode();

        propertyNode.put("type", "integer");
        propertyNode.put("default", "42");
        propertiesNode.set("testProperty", propertyNode);
        schemaNode.set("properties", propertiesNode);

        camelCatalogSchemaEnhancer.fixDefaultValueTypesFromCamelSchema(schemaNode);

        var defaultValue = propertyNode.get("default");
        assertTrue(defaultValue.isNumber());
        assertEquals(42, defaultValue.asLong());
    }

    @Test
    void shouldFixNumberDefaultValueFromString() {
        ObjectMapper jsonMapper = new ObjectMapper();
        ObjectNode schemaNode = jsonMapper.createObjectNode();
        ObjectNode propertiesNode = jsonMapper.createObjectNode();
        ObjectNode propertyNode = jsonMapper.createObjectNode();

        propertyNode.put("type", "number");
        propertyNode.put("default", "3.14");
        propertiesNode.set("testProperty", propertyNode);
        schemaNode.set("properties", propertiesNode);

        camelCatalogSchemaEnhancer.fixDefaultValueTypesFromCamelSchema(schemaNode);

        var defaultValue = propertyNode.get("default");
        assertTrue(defaultValue.isNumber());
        assertEquals(3.14, defaultValue.asDouble(), 0.001);
    }

    @Test
    void shouldNotModifyNonStringDefaultValues() {
        ObjectMapper jsonMapper = new ObjectMapper();
        ObjectNode schemaNode = jsonMapper.createObjectNode();
        ObjectNode propertiesNode = jsonMapper.createObjectNode();
        ObjectNode propertyNode = jsonMapper.createObjectNode();

        propertyNode.put("type", "boolean");
        propertyNode.put("default", true);
        propertiesNode.set("testProperty", propertyNode);
        schemaNode.set("properties", propertiesNode);

        camelCatalogSchemaEnhancer.fixDefaultValueTypesFromCamelSchema(schemaNode);

        var defaultValue = propertyNode.get("default");
        assertTrue(defaultValue.isBoolean());
        assertTrue(defaultValue.asBoolean());
    }

    @Test
    void shouldKeepStringDefaultForStringType() {
        ObjectMapper jsonMapper = new ObjectMapper();
        ObjectNode schemaNode = jsonMapper.createObjectNode();
        ObjectNode propertiesNode = jsonMapper.createObjectNode();
        ObjectNode propertyNode = jsonMapper.createObjectNode();

        propertyNode.put("type", "string");
        propertyNode.put("default", "hello");
        propertiesNode.set("testProperty", propertyNode);
        schemaNode.set("properties", propertiesNode);

        camelCatalogSchemaEnhancer.fixDefaultValueTypesFromCamelSchema(schemaNode);

        var defaultValue = propertyNode.get("default");
        assertTrue(defaultValue.isTextual());
        assertEquals("hello", defaultValue.asText());
    }

    @Test
    void shouldFixDefaultValuesInDefinitions() {
        ObjectMapper jsonMapper = new ObjectMapper();
        ObjectNode schemaNode = jsonMapper.createObjectNode();
        ObjectNode definitionsNode = jsonMapper.createObjectNode();
        ObjectNode definitionNode = jsonMapper.createObjectNode();
        ObjectNode propertiesNode = jsonMapper.createObjectNode();
        ObjectNode propertyNode = jsonMapper.createObjectNode();

        propertyNode.put("type", "boolean");
        propertyNode.put("default", "true");
        propertiesNode.set("testProperty", propertyNode);
        definitionNode.set("properties", propertiesNode);
        definitionsNode.set("TestDefinition", definitionNode);
        schemaNode.set("definitions", definitionsNode);

        camelCatalogSchemaEnhancer.fixDefaultValueTypesFromCamelSchema(schemaNode);

        var defaultValue = propertyNode.get("default");
        assertTrue(defaultValue.isBoolean());
        assertTrue(defaultValue.asBoolean());
    }

    @Test
    void shouldFixDefaultValuesInAnyOfArrays() {
        ObjectMapper jsonMapper = new ObjectMapper();
        ObjectNode schemaNode = jsonMapper.createObjectNode();
        var anyOfArray = schemaNode.putArray("anyOf");
        ObjectNode anyOfItem = jsonMapper.createObjectNode();
        ObjectNode propertiesNode = jsonMapper.createObjectNode();
        ObjectNode propertyNode = jsonMapper.createObjectNode();

        propertyNode.put("type", "boolean");
        propertyNode.put("default", "false");
        propertiesNode.set("testProperty", propertyNode);
        anyOfItem.set("properties", propertiesNode);
        anyOfArray.add(anyOfItem);

        camelCatalogSchemaEnhancer.fixDefaultValueTypesFromCamelSchema(schemaNode);

        var defaultValue = propertyNode.get("default");
        assertTrue(defaultValue.isBoolean());
        assertFalse(defaultValue.asBoolean());
    }

    @Test
    void shouldFixDefaultValuesInOneOfArrays() {
        ObjectMapper jsonMapper = new ObjectMapper();
        ObjectNode schemaNode = jsonMapper.createObjectNode();
        var oneOfArray = schemaNode.putArray("oneOf");
        ObjectNode oneOfItem = jsonMapper.createObjectNode();
        ObjectNode propertiesNode = jsonMapper.createObjectNode();
        ObjectNode propertyNode = jsonMapper.createObjectNode();

        propertyNode.put("type", "integer");
        propertyNode.put("default", "100");
        propertiesNode.set("testProperty", propertyNode);
        oneOfItem.set("properties", propertiesNode);
        oneOfArray.add(oneOfItem);

        camelCatalogSchemaEnhancer.fixDefaultValueTypesFromCamelSchema(schemaNode);

        var defaultValue = propertyNode.get("default");
        assertTrue(defaultValue.isNumber());
        assertEquals(100, defaultValue.asLong());
    }

    @Test
    void shouldHandleInvalidNumberGracefully() {
        ObjectMapper jsonMapper = new ObjectMapper();
        ObjectNode schemaNode = jsonMapper.createObjectNode();
        ObjectNode propertiesNode = jsonMapper.createObjectNode();
        ObjectNode propertyNode = jsonMapper.createObjectNode();

        propertyNode.put("type", "integer");
        propertyNode.put("default", "not-a-number");
        propertiesNode.set("testProperty", propertyNode);
        schemaNode.set("properties", propertiesNode);

        camelCatalogSchemaEnhancer.fixDefaultValueTypesFromCamelSchema(schemaNode);

        // Should keep as string if parsing fails
        var defaultValue = propertyNode.get("default");
        assertTrue(defaultValue.isTextual());
        assertEquals("not-a-number", defaultValue.asText());
    }

    @Test
    void shouldNotModifyPropertiesWithoutDefaultValue() {
        ObjectMapper jsonMapper = new ObjectMapper();
        ObjectNode schemaNode = jsonMapper.createObjectNode();
        ObjectNode propertiesNode = jsonMapper.createObjectNode();
        ObjectNode propertyNode = jsonMapper.createObjectNode();

        propertyNode.put("type", "boolean");
        propertiesNode.set("testProperty", propertyNode);
        schemaNode.set("properties", propertiesNode);

        camelCatalogSchemaEnhancer.fixDefaultValueTypesFromCamelSchema(schemaNode);

        assertFalse(propertyNode.has("default"));
    }

    @Test
    void shouldNotModifyPropertiesWithoutType() {
        ObjectMapper jsonMapper = new ObjectMapper();
        ObjectNode schemaNode = jsonMapper.createObjectNode();
        ObjectNode propertiesNode = jsonMapper.createObjectNode();
        ObjectNode propertyNode = jsonMapper.createObjectNode();

        propertyNode.put("default", "true");
        propertiesNode.set("testProperty", propertyNode);
        schemaNode.set("properties", propertiesNode);

        camelCatalogSchemaEnhancer.fixDefaultValueTypesFromCamelSchema(schemaNode);

        // Should remain as string without type information
        var defaultValue = propertyNode.get("default");
        assertTrue(defaultValue.isTextual());
    }
}
