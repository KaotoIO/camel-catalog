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

    private static final String PROP_TITLE = "title";
    private static final String PROP_DESCRIPTION = "description";
    private static final String PROP_REQUIRED = "required";
    private static final String PROP_PROPERTIES_PATH = "/properties";
    private static final String PROP_DEFAULT = "default";
    private static final String PROP_FORMAT = "format";
    private static final List<String> TO_STRING_TYPES = List.of("binary");

    public static void process(ObjectNode kamelet) {
        var schema = kamelet.withObject("/propertiesSchema");
        var kameletDef = kamelet.withObject("/spec")
                .withObject("/definition");
        schema.put("$schema", "http://json-schema.org/draft-07/schema#");
        schema.put("type", "object");
        if (kameletDef.has(PROP_TITLE)) schema.set(PROP_TITLE, kameletDef.get(PROP_TITLE));
        if (kameletDef.has(PROP_DESCRIPTION)) schema.set(PROP_DESCRIPTION, kameletDef.get(PROP_DESCRIPTION));
        if (kameletDef.has(PROP_REQUIRED)) schema.set(PROP_REQUIRED, kameletDef.get(PROP_REQUIRED));
        if (kameletDef.has("properties") && !kameletDef.withObject(PROP_PROPERTIES_PATH).isEmpty()) {
            var kameletProperties = kameletDef.withObject(PROP_PROPERTIES_PATH);
            var schemaProperties = schema.withObject(PROP_PROPERTIES_PATH);
            for (var entry : kameletProperties.properties()) {
                var name = entry.getKey();
                var property = entry.getValue();
                var schemaProperty = schemaProperties.withObject("/" + name);
                if (property.has("type")) schemaProperty.set("type", property.get("type"));
                if (TO_STRING_TYPES.contains(property.get("type").asText())) {
                    schemaProperty.put("$comment", "type:" + property.get("type").asText());
                    schemaProperty.put("type", "string");
                }
                if (property.has(PROP_TITLE)) schemaProperty.set(PROP_TITLE, property.get(PROP_TITLE));
                if (property.has(PROP_DESCRIPTION)) schemaProperty.set(PROP_DESCRIPTION, property.get(PROP_DESCRIPTION));
                if (property.has("enum")) schemaProperty.set("enum", property.get("enum"));
                if (property.has(PROP_DEFAULT)) schemaProperty.set(PROP_DEFAULT, property.get(PROP_DEFAULT));
                if (property.has(PROP_FORMAT)) schemaProperty.set(PROP_FORMAT, property.get(PROP_FORMAT));
            }
        }
    }
}
