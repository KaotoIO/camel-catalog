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
import io.kaoto.camelcatalog.generator.camel.CamelCatalogSchemaEnhancer;
import io.kaoto.camelcatalog.generator.camel.CamelYamlDslSchemaProcessor;
import io.kaoto.camelcatalog.generator.camel.CatalogEntryHandler;
import org.apache.camel.catalog.CamelCatalog;
import org.apache.camel.tooling.model.EipModel;
import org.apache.camel.tooling.model.EipModel.EipOptionModel;
import org.apache.camel.tooling.model.JsonMapper;
import org.apache.camel.tooling.model.Kind;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class DataFormatHandler implements CatalogEntryHandler {

    private final CamelCatalog camelCatalog;
    private final CamelYamlDslSchemaProcessor schemaProcessor;
    private final CamelCatalogSchemaEnhancer schemaEnhancer;
    private final ObjectMapper jsonMapper = new ObjectMapper()
            .configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true);

    public DataFormatHandler(CamelCatalog camelCatalog, CamelYamlDslSchemaProcessor schemaProcessor,
                             CamelCatalogSchemaEnhancer schemaEnhancer) {
        this.camelCatalog = camelCatalog;
        this.schemaProcessor = schemaProcessor;
        this.schemaEnhancer = schemaEnhancer;
    }

    @Override
    public Map<String, ObjectNode> generate() {
        try {
            return doGenerate();
        } catch (Exception e) {
            throw new RuntimeException("Error generating data format catalog", e);
        }
    }

    private Map<String, ObjectNode> doGenerate() throws Exception {
        Map<String, ObjectNode> answer = new LinkedHashMap<>();

        var catalogMap = new LinkedHashMap<String, EipModel>();
        for (var name : camelCatalog.findDataFormatNames()) {
            var modelCatalog = camelCatalog.dataFormatModel(name);
            catalogMap.put(modelCatalog.getName(), camelCatalog.eipModel(name));
        }

        var dataFormatSchemaMap = schemaProcessor.getDataFormats();
        for (var entry : dataFormatSchemaMap.entrySet()) {
            var dataFormatName = entry.getKey();
            var dataFormatSchema = entry.getValue();
            EipModel eipModel = catalogMap.get(dataFormatName);
            List<EipOptionModel> eipModelOptions = List.of();
            if (eipModel != null) {
                eipModelOptions = eipModel.getOptions();
            }

            schemaEnhancer.sortPropertiesByOptions(dataFormatSchema, eipModelOptions);

            var dataFormatCatalog = (EipModel) camelCatalog.model(Kind.eip, dataFormatName);
            if (dataFormatCatalog == null) {
                throw new RuntimeException("DataFormat " + dataFormatName + " is not found in Camel model catalog.");
            }
            var json = JsonMapper.asJsonObject(dataFormatCatalog).toJson();
            try {
                var catalogTree = (ObjectNode) jsonMapper.readTree(json);
                catalogTree.set("propertiesSchema", dataFormatSchema);
                schemaEnhancer.setRequiredToPropertiesSchema(dataFormatSchema, catalogTree);
                schemaEnhancer.fixDefaultValueTypesFromCamelSchema(dataFormatSchema);
                answer.put(dataFormatName, catalogTree);
            } catch (Exception e) {
                throw new RuntimeException("Error processing data format: " + dataFormatName, e);
            }
        }

        return answer;
    }
}
