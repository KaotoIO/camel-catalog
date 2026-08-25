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

import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.List;

public class KameletProcessor {

    private KameletProcessor() {
    }
    private static final String SPEC = "spec";
    private static final String DEFINITION = "definition";
    private static final String PROPERTIES = "properties";
    private static final String SLASH_PROPERTIES = "/properties";
    private static final String TITLE = "title";
    private static final String DESCRIPTION = "description";
    private static final String TYPE = "type";
    private static final String REQUIRED = "required";
    private static final String ENUM = "enum";
    private static final String DEFAULT = "default";
    private static final String FORMAT = "format";
    
    private static final List<String> TO_STRING_TYPES = List.of("binary");

    public static void process(ObjectNode kamelet) {
        var schema = kamelet.withObject("/propertiesSchema");
        var kameletDef = kamelet.withObject(SPEC)
                .withObject(DEFINITION);
        schema.put("$schema", "http://json-schema.org/draft-07/schema#");
        schema.put(TYPE, "object");
        if (kameletDef.has(TITLE)) schema.set(TITLE, kameletDef.get(TITLE));
        if (kameletDef.has(DESCRIPTION)) schema.set(DESCRIPTION, kameletDef.get(DESCRIPTION));
        if (kameletDef.has(REQUIRED)) schema.set(REQUIRED, kameletDef.get(REQUIRED));
        if (kameletDef.has(PROPERTIES) && !kameletDef.withObject(SLASH_PROPERTIES).isEmpty()) {
            var kameletProperties = kameletDef.withObject(PROPERTIES);
            var schemaProperties = schema.withObject(PROPERTIES);
            for (var entry : kameletProperties.properties()) {
                var name = entry.getKey();
                var property = entry.getValue();
                var schemaProperty = schemaProperties.withObject("/" + name);
                if (property.has(TYPE)) schemaProperty.set(TYPE, property.get(TYPE));
                if (TO_STRING_TYPES.contains(property.get(TYPE).asText())) {
                    schemaProperty.put("$comment", "type:" + property.get(TYPE).asText());
                    schemaProperty.put(TYPE, "string");
                }
                if (property.has(TITLE)) schemaProperty.set(TITLE, property.get(TITLE));
                if (property.has(DESCRIPTION)) schemaProperty.set(DESCRIPTION, property.get(DESCRIPTION));
                if (property.has(ENUM)) schemaProperty.set(ENUM, property.get(ENUM));
                if (property.has(DEFAULT)) schemaProperty.set(DEFAULT, property.get(DEFAULT));
                if (property.has(FORMAT)) schemaProperty.set(FORMAT, property.get(FORMAT));
            }
        }
    }
}
