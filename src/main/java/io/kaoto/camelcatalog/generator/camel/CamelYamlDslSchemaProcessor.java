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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.kaoto.camelcatalog.model.Constants;

import java.util.*;
import static io.kaoto.camelcatalog.model.Constants.*;
import java.util.stream.Stream;

/**
 * Process camelYamlDsl.json file, aka Camel YAML DSL JSON schema.
 */
public class CamelYamlDslSchemaProcessor {
    private static final String PROCESSOR_DEFINITION = "org.apache.camel.model.ProcessorDefinition";
    private static final String LOAD_BALANCE_DEFINITION = "org.apache.camel.model.LoadBalanceDefinition";
    private static final String EXPRESSION_SUB_ELEMENT_DEFINITION =
            "org.apache.camel.model.ExpressionSubElementDefinition";
    
    private final ObjectMapper jsonMapper;
    private final ObjectNode yamlDslSchema;

    private final List<String> processorReferenceBlockList = List.of(PROCESSOR_DEFINITION);

    public CamelYamlDslSchemaProcessor(ObjectNode yamlDslSchema) {
        this.yamlDslSchema = yamlDslSchema;
    }

    private ObjectNode relocateToRootDefinitions(ObjectNode definitions) {
        var relocatedDefinitions = definitions.deepCopy();
        relocatedDefinitions.findParentsJSON_SCHEMA_REF.stream()
                .map(ObjectNode.class::cast)
                .forEach(n -> n.put(JSON_SCHEMA_REF, getRelocatedRef(n)));
        relocatedDefinitions.findParents(Constants.JSON_SCHEMA_REF).stream()
                .map(ObjectNode.class::cast)
                .forEach(n -> n.put(Constants.JSON_SCHEMA_REF, getRelocatedRef(n)));
        return relocatedDefinitions;
    }

    private String getRelocatedRef(ObjectNode parent) {
        return parent.get(JSON_SCHEMA_REF).asText().replace(JSON_SCHEMA_ITEMS_DEFINITIONS, DEFINITIONS_PATH);
    }

    private String getNameFromRef(ObjectNode parent) {
        var ref = parent.getJSON_SCHEMA_REF.asText();
        return ref.contains(JSON_SCHEMA_ITEMS) ? ref.replace(JSON_SCHEMA_ITEMS_DEFINITIONS_PATH, "")
                : ref.replace(DEFINITIONS_PATH, "");
        return parent.get(Constants.JSON_SCHEMA_REF).asText().replace("#/items/definitions/", "#/definitions/");
    }

    private String getNameFromRef(ObjectNode parent) {
        var ref = parent.get(Constants.JSON_SCHEMA_REF).asText();
        return ref.contains("items") ? ref.replace("#/items/definitions/", "")
                : ref.replace("#/definitions/", "");
    }

    private void populateDefinitions(ObjectNode schema, ObjectNode definitions) {
        boolean added = true;
        while (added) {
            added = false;
            for (JsonNode refParent : schema.findParentsJSON_SCHEMA_REF) {
                var name = getNameFromRef((ObjectNode) refParent);

                if ((!schema.has(JSON_SCHEMA_DEFINITIONS) || !schema.withObject(JSON_SCHEMA_SLASH_DEFINITIONS).has(name)) && !processorReferenceBlockList.contains(name)){
            for (JsonNode refParent : schema.findParents(Constants.JSON_SCHEMA_REF)) {
                var name = getNameFromRef((ObjectNode) refParent);

                if ((!schema.has(Constants.JSON_SCHEMA_DEFINITIONS) || !schema.withObject("/" + Constants.JSON_SCHEMA_DEFINITIONS).has(name)) && !processorReferenceBlockList.contains(name)){
                    if (!definitions.has(name)) {
                        throw new IllegalStateException("Missing definition: " + name);
                    }

                    var schemaDefinitions = schema.withObject(JSON_SCHEMA_SLASH_DEFINITIONS);
                    schemaDefinitions.set(name, definitions.get(name).deepCopy());
                    added = true;
                    break;
                    if ((!schema.has(Constants.JSON_SCHEMA_DEFINITIONS) || !schema.withObject("/" + Constants.JSON_SCHEMA_DEFINITIONS).has(name)) && !processorReferenceBlockList.contains(name)) {
                        var schemaDefinitions = schema.withObject("/" + Constants.JSON_SCHEMA_DEFINITIONS);
                        schemaDefinitions.set(name, definitions.get(name).deepCopy());
                        added = true;
                        break;
                    }
                }
            }
        }
    }

    public Map<String, ObjectNode> getDataFormats() {
        var definitions = yamlDslSchema
                .withObject("/items")
                .withObject(JSON_SCHEMA_SLASH_DEFINITIONS);
                .withObject("/" + Constants.JSON_SCHEMA_ITEMS)
                .withObject("/" + Constants.JSON_SCHEMA_DEFINITIONS);
        var relocatedDefinitions = relocateToRootDefinitions(definitions);
        var fromMarshal = relocatedDefinitions
                .withObject("/org.apache.camel.model.MarshalDefinition")
                .withArray("/" + Constants.JSON_SCHEMA_ANY_OF)
                .get(0).withArray("/" + Constants.JSON_SCHEMA_ONE_OF);
        var fromUnmarshal = relocatedDefinitions
                .withObject("/org.apache.camel.model.UnmarshalDefinition")
                .withArray("/" + Constants.JSON_SCHEMA_ANY_OF)
                .get(0).withArray("/" + Constants.JSON_SCHEMA_ONE_OF);

        var answer = new LinkedHashMap<String, ObjectNode>();
        for (var entry : fromMarshal) {
            if (entry.has(JSON_SCHEMA_REQUIRED)) {
                var entryName = entry.withArray("/required").get(0).asText();
                var property = entry
                        .withObject("/properties")
                        .withObject("/" + entryName);
                var entryDefinitionName = getNameFromRef(property);
                var dataformat = relocatedDefinitions.withObject("/" + entryDefinitionName);
                if (!dataformat.has("oneOf")) {
                    populateDefinitions(dataformat, relocatedDefinitions);
                    answer.put(entryName, dataformat);
                } else {
                    var dfOneOf = dataformat.withArray("/oneOf");
                    if (dfOneOf.size() != 2) {
                        throw new IllegalStateException(String.format(
                                "DataFormat '%s' has '%s' entries in oneOf unexpectedly, look it closer",
                                entryDefinitionName,
                                dfOneOf.size()));
        Stream.concat(fromMarshal.valueStream(), fromUnmarshal.valueStream())
                .filter(entry -> entry.has(Constants.JSON_SCHEMA_REQUIRED))
                .forEach(entry -> {
                    var entryName = entry.withArray("/" + Constants.JSON_SCHEMA_REQUIRED).get(0).asText();
                    if (answer.containsKey(entryName)) {
                        return;
                    }
                    var property = entry
                            .withObject("/" + Constants.JSON_SCHEMA_PROPERTIES)
                            .withObject("/" + entryName);
                    var entryDefinitionName = getNameFromRef(property);
                    var dataformat = relocatedDefinitions.withObject("/" + entryDefinitionName);
                    if (!dataformat.has(Constants.JSON_SCHEMA_ONE_OF)) {
                        populateDefinitions(dataformat, relocatedDefinitions);
                        answer.put(entryName, dataformat);
                    } else {
                        var dfOneOf = dataformat.withArray("/" + Constants.JSON_SCHEMA_ONE_OF);
                        if (dfOneOf.size() != 2) {
                            throw new IllegalStateException(String.format(
                                    "DataFormat '%s' has '%s' entries in oneOf unexpectedly, look it closer",
                                    entryDefinitionName,
                                    dfOneOf.size()));
                        }
                        for (var def : dfOneOf) {
                            if (def.get(Constants.JSON_SCHEMA_TYPE).asText().equals(Constants.JSON_SCHEMA_TYPE_OBJECT)) {
                                var objectDef = (ObjectNode) def;
                                objectDef.set(Constants.TITLE, dataformat.get(Constants.TITLE));
                                objectDef.set(Constants.DESCRIPTION, dataformat.get(Constants.DESCRIPTION));
                                populateDefinitions(objectDef, relocatedDefinitions);
                                answer.put(entryName, objectDef);
                                break;
                            }
                        }
                    }
                });
        return answer;
    }

    public Map<String, ObjectNode> getLanguages() {
        var definitions = yamlDslSchema
                .withObject("/items")
                .withObject(JSON_SCHEMA_SLASH_DEFINITIONS);
                .withObject("/" + Constants.JSON_SCHEMA_ITEMS)
                .withObject("/" + Constants.JSON_SCHEMA_DEFINITIONS);
        var relocatedDefinitions = relocateToRootDefinitions(definitions);
        var languages = relocatedDefinitions
                .withObject("/org.apache.camel.model.language.ExpressionDefinition")
                .withArray("/" + Constants.JSON_SCHEMA_ANY_OF).get(0)
                .withArray("/" + Constants.JSON_SCHEMA_ONE_OF);

        var answer = new LinkedHashMap<String, ObjectNode>();
        for (var entry : languages) {
            if (!entry.has("type") || !"object".equals(entry.get("type").asText()) || !entry.has(JSON_SCHEMA_REQUIRED)) {
            if (!entry.has(Constants.JSON_SCHEMA_TYPE) || !Constants.JSON_SCHEMA_TYPE_OBJECT.equals(entry.get(Constants.JSON_SCHEMA_TYPE).asText()) || !entry.has(Constants.JSON_SCHEMA_REQUIRED)) {
                throw new IllegalStateException("Unexpected language entry " + entry.asText());
            }
            var entryName = entry.withArray("/" + Constants.JSON_SCHEMA_REQUIRED).get(0).asText();
            var property = entry
                    .withObject("/" + Constants.JSON_SCHEMA_PROPERTIES)
                    .withObject("/" + entryName);
            var entryDefinitionName = getNameFromRef(property);
            var language = relocatedDefinitions.withObject("/" + entryDefinitionName);
            if (language.has(Constants.JSON_SCHEMA_ONE_OF)) {
                var langOneOf = language.withArray("/" + Constants.JSON_SCHEMA_ONE_OF);
                if (langOneOf.size() != 2) {
                    throw new IllegalStateException(String.format(
                            "Language '%s' has '%s' entries in oneOf unexpectedly, look it closer",
                            entryDefinitionName,
                            langOneOf.size()));
                }
                for (var def : langOneOf) {
                    if (def.get(Constants.JSON_SCHEMA_TYPE).asText().equals(Constants.JSON_SCHEMA_TYPE_OBJECT)) {
                        var objectDef = (ObjectNode) def;
                        objectDef.set(Constants.TITLE, language.get(Constants.TITLE));
                        objectDef.set(Constants.DESCRIPTION, language.get(Constants.DESCRIPTION));
                        populateDefinitions(objectDef, relocatedDefinitions);
                        answer.put(entryName, objectDef);
                        break;
                    }
                }
            } else {
                populateDefinitions(language, relocatedDefinitions);
                answer.put(entryName, language);
            }
        }
        return answer;
    }

    public Map<String, ObjectNode> getLoadBalancers() {
        var definitions = yamlDslSchema
                .withObject("/items")
                .withObject(JSON_SCHEMA_SLASH_DEFINITIONS);
                .withObject("/" + Constants.JSON_SCHEMA_ITEMS)
                .withObject("/" + Constants.JSON_SCHEMA_DEFINITIONS);
        var relocatedDefinitions = relocateToRootDefinitions(definitions);
        var loadBalancerAnyOfOneOf = relocatedDefinitions
                .withObject("/" + LOAD_BALANCE_DEFINITION)
                .withArray("/" + Constants.JSON_SCHEMA_ANY_OF).get(0)
                .withArray("/" + Constants.JSON_SCHEMA_ONE_OF);

        var answer = new LinkedHashMap<String, ObjectNode>();
        for (var entry : loadBalancerAnyOfOneOf) {
            if (entry.has(Constants.JSON_SCHEMA_NOT)) {
                continue;
            }
            if (!entry.has("type") || !"object".equals(entry.get("type").asText()) || !entry.has(JSON_SCHEMA_REQUIRED)) {
            if (!entry.has(Constants.JSON_SCHEMA_TYPE) || !Constants.JSON_SCHEMA_TYPE_OBJECT.equals(entry.get(Constants.JSON_SCHEMA_TYPE).asText()) || !entry.has(Constants.JSON_SCHEMA_REQUIRED)) {
                throw new IllegalStateException("Unexpected loadbalancer entry " + entry.asText());
            }
            var entryName = entry.withArray("/" + Constants.JSON_SCHEMA_REQUIRED).get(0).asText();
            var property = entry
                    .withObject("/" + Constants.JSON_SCHEMA_PROPERTIES)
                    .withObject("/" + entryName);
            var entryDefinitionName = getNameFromRef(property);
            var loadBalancer = relocatedDefinitions.withObject("/" + entryDefinitionName);
            if (loadBalancer.has(Constants.JSON_SCHEMA_ONE_OF)) {
                var lbOneOf = loadBalancer.withArray("/" + Constants.JSON_SCHEMA_ONE_OF);
                if (lbOneOf.size() != 2) {
                    throw new IllegalStateException(String.format(
                            "LoadBalancer '%s' has '%s' entries in oneOf unexpectedly, look it closer",
                            entryDefinitionName,
                            lbOneOf.size()));
                }
                for (var def : lbOneOf) {
                    if (def.get(Constants.JSON_SCHEMA_TYPE).asText().equals(Constants.JSON_SCHEMA_TYPE_OBJECT)) {
                        var objectDef = (ObjectNode) def;
                        objectDef.set(Constants.TITLE, loadBalancer.get(Constants.TITLE));
                        objectDef.set(Constants.DESCRIPTION, loadBalancer.get(Constants.DESCRIPTION));
                        loadBalancer = objectDef;
                        break;
                    }
                }
            }
            populateDefinitions(loadBalancer, relocatedDefinitions);
            for (var prop : loadBalancer.withObject("/" + Constants.JSON_SCHEMA_PROPERTIES).properties()) {
                var propertyDef = (ObjectNode) prop.getValue();
                var refParent = propertyDef.findParent(JSON_SCHEMA_REF);
                if (refParent != null) {
                    var ref = getNameFromRef(refParent);
                    if (EXPRESSION_SUB_ELEMENT_DEFINITION.equals(ref)) {
                        refParent.remove(JSON_SCHEMA_REF);
                        refParent.put("type", "object");
                        refParent.put("$comment", "expression");
                var refParent = propertyDef.findParent(Constants.JSON_SCHEMA_REF);
                if (refParent != null) {
                    var ref = getNameFromRef(refParent);
                    if (EXPRESSION_SUB_ELEMENT_DEFINITION.equals(ref)) {
                        refParent.remove(Constants.JSON_SCHEMA_REF);
                        refParent.put(Constants.JSON_SCHEMA_TYPE, Constants.JSON_SCHEMA_TYPE_OBJECT);
                        refParent.put(Constants.JSON_SCHEMA_COMMENT, "expression");
                    }
                }
            }
            answer.put(entryName, loadBalancer);
        }
        return answer;
    }
}
