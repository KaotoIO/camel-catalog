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
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.kaoto.camelcatalog.generator.camel.CatalogEntryHandler;
import org.apache.camel.catalog.CamelCatalog;
import org.apache.camel.tooling.model.EipModel;
import org.apache.camel.tooling.model.JsonMapper;
import org.apache.camel.tooling.model.Kind;

import java.util.LinkedHashMap;
import java.util.Map;

public class ModelHandler implements CatalogEntryHandler {

    private final CamelCatalog camelCatalog;
    private final ObjectMapper jsonMapper = new ObjectMapper()
            .configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true);

    public ModelHandler(CamelCatalog camelCatalog) {
        this.camelCatalog = camelCatalog;
    }

    @Override
    public Map<String, ObjectNode> generate() {
        Map<String, ObjectNode> answer = new LinkedHashMap<>();

        for (var name : camelCatalog.findModelNames().stream().sorted().toList()) {
            var model = (EipModel) camelCatalog.model(Kind.eip, name);
            var json = JsonMapper.asJsonObject(model).toJson();
            try {
                var catalogNode = (ObjectNode) jsonMapper.readTree(json);
                answer.put(name, catalogNode);
            } catch (Exception e) {
                throw new RuntimeException("Error processing model: " + name, e);
            }
        }

        return answer;
    }
}
