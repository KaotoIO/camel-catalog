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
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.*;

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

    public CamelYamlDslSchemaProcessor(ObjectMapper mapper, ObjectNode yamlDslSchema) {
        this.jsonMapper = mapper;
        this.yamlDslSchema = yamlDslSchema;
    }

    private ObjectNode relocateToRootDefinitions(ObjectNode definitions) {
        var relocatedDefinitions = definitions.deepCopy();
        relocatedDefinitions.findParents("$ref").stream()
                .map(ObjectNode.class::cast)
                .forEach(n -> n.put("$ref", getRelocatedRef(n)));
        return relocatedDefinitions;
    }

    private String getRelocatedRef(ObjectNode parent) {
        return parent.get("$ref").asText().replace("#/items/definitions/", "#/definitions/");
    }

    private String getNameFromRef(ObjectNode parent) {
        var ref = parent.get("$ref").asText();
        return ref.contains("items") ? ref.replace("#/items/definitions/", "")
                : ref.replace("#/definitions/", "");
    }

    private void populateDefinitions(ObjectNode schema, ObjectNode definitions) {
        boolean added = true;
        while (added) {
            added = false;
            for (JsonNode refParent : schema.findParents("$ref")) {
                var name = getNameFromRef((ObjectNode) refParent);

                if ((!schema.has("definitions") || !schema.withObject("/definitions").has(name)) && !processorReferenceBlockList.contains(name)){
                    if (!definitions.has(name)) {
                        throw new IllegalStateException("Missing definition: " + name);
                    }

                    if ((!schema.has("definitions") || !schema.withObject("/definitions").has(name)) && !processorReferenceBlockList.contains(name)) {
                        var schemaDefinitions = schema.withObject("/definitions");
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
                .withObject("/definitions");
        var relocatedDefinitions = relocateToRootDefinitions(definitions);
        var fromMarshal = relocatedDefinitions
                .withObject("/org.apache.camel.model.MarshalDefinition")
                .withArray("/anyOf")
                .get(0).withArray("/oneOf");
        var fromUnmarshal = relocatedDefinitions
                .withObject("/org.apache.camel.model.UnmarshalDefinition")
                .withArray("/anyOf")
                .get(0).withArray("/oneOf");
        if (fromMarshal.size() != fromUnmarshal.size()) {
            // Could this happen in the future? If so, we need to prepare separate sets for
            // marshal and unmarshal
            throw new IllegalStateException("Marshal and Unmarshal dataformats are not the same size");
        }

        var answer = new LinkedHashMap<String, ObjectNode>();
        for (var entry : fromMarshal) {
            if (entry.has("required")) {
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
                    }
                    for (var def : dfOneOf) {
                        if (def.get("type").asText().equals("object")) {
                            var objectDef = (ObjectNode) def;
                            objectDef.set("title", dataformat.get("title"));
                            objectDef.set("description", dataformat.get("description"));
                            populateDefinitions(objectDef, relocatedDefinitions);
                            answer.put(entryName, objectDef);
                            break;
                        }
                    }
                }
            }
        }
        return answer;
    }

    public Map<String, ObjectNode> getLanguages() {
        var definitions = yamlDslSchema
                .withObject("/items")
                .withObject("/definitions");
        var relocatedDefinitions = relocateToRootDefinitions(definitions);
        var languages = relocatedDefinitions
                .withObject("/org.apache.camel.model.language.ExpressionDefinition")
                .withArray("/anyOf").get(0)
                .withArray("/oneOf");

        var answer = new LinkedHashMap<String, ObjectNode>();
        for (var entry : languages) {
            if (!entry.has("type") || !"object".equals(entry.get("type").asText()) || !entry.has("required")) {
                throw new IllegalStateException("Unexpected language entry " + entry.asText());
            }
            var entryName = entry.withArray("/required").get(0).asText();
            var property = entry
                    .withObject("/properties")
                    .withObject("/" + entryName);
            var entryDefinitionName = getNameFromRef(property);
            var language = relocatedDefinitions.withObject("/" + entryDefinitionName);
            if (language.has("oneOf")) {
                var langOneOf = language.withArray("/oneOf");
                if (langOneOf.size() != 2) {
                    throw new IllegalStateException(String.format(
                            "Language '%s' has '%s' entries in oneOf unexpectedly, look it closer",
                            entryDefinitionName,
                            langOneOf.size()));
                }
                for (var def : langOneOf) {
                    if (def.get("type").asText().equals("object")) {
                        var objectDef = (ObjectNode) def;
                        objectDef.set("title", language.get("title"));
                        objectDef.set("description", language.get("description"));
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
                .withObject("/definitions");
        var relocatedDefinitions = relocateToRootDefinitions(definitions);
        var loadBalancerAnyOfOneOf = relocatedDefinitions
                .withObject("/" + LOAD_BALANCE_DEFINITION)
                .withArray("/anyOf").get(0)
                .withArray("/oneOf");

        var answer = new LinkedHashMap<String, ObjectNode>();
        for (var entry : loadBalancerAnyOfOneOf) {
            if (entry.has("not")) {
                continue;
            }
            if (!"object".equals(entry.get("type").asText()) || !entry.has("required")) {
                throw new IllegalStateException("Unexpected loadbalancer entry " + entry.asText());
            }
            var entryName = entry.withArray("/required").get(0).asText();
            var property = entry
                    .withObject("/properties")
                    .withObject("/" + entryName);
            var entryDefinitionName = getNameFromRef(property);
            var loadBalancer = relocatedDefinitions.withObject("/" + entryDefinitionName);
            if (loadBalancer.has("oneOf")) {
                var lbOneOf = loadBalancer.withArray("/oneOf");
                if (lbOneOf.size() != 2) {
                    throw new IllegalStateException(String.format(
                            "LoadBalancer '%s' has '%s' entries in oneOf unexpectedly, look it closer",
                            entryDefinitionName,
                            lbOneOf.size()));
                }
                for (var def : lbOneOf) {
                    if (def.get("type").asText().equals("object")) {
                        var objectDef = (ObjectNode) def;
                        objectDef.set("title", loadBalancer.get("title"));
                        objectDef.set("description", loadBalancer.get("description"));
                        loadBalancer = objectDef;
                        break;
                    }
                }
            }
            populateDefinitions(loadBalancer, relocatedDefinitions);
            for (var prop : loadBalancer.withObject("/properties").properties()) {
                var propertyDef = (ObjectNode) prop.getValue();
                var refParent = propertyDef.findParent("$ref");
                if (refParent != null) {
                    var ref = getNameFromRef(refParent);
                    if (EXPRESSION_SUB_ELEMENT_DEFINITION.equals(ref)) {
                        refParent.remove("$ref");
                        refParent.put("type", "object");
                        refParent.put("$comment", "expression");
                    }
                }
            }
            answer.put(entryName, loadBalancer);
        }
        return answer;
    }
}
