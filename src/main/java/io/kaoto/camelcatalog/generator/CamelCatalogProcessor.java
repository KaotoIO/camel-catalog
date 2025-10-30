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
package io.kaoto.camelcatalog.generator;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.kaoto.camelcatalog.generators.CamelCatalogSchemaEnhancer;
import io.kaoto.camelcatalog.generators.ComponentGenerator;
import io.kaoto.camelcatalog.generators.EIPGenerator;
import io.kaoto.camelcatalog.generators.EntityGenerator;
import io.kaoto.camelcatalog.generators.FunctionsGenerator;
import io.kaoto.camelcatalog.maven.CamelCatalogVersionLoader;
import io.kaoto.camelcatalog.model.CatalogRuntime;
import org.apache.camel.catalog.CamelCatalog;
import org.apache.camel.tooling.model.EipModel;
import org.apache.camel.tooling.model.EipModel.EipOptionModel;
import org.apache.camel.tooling.model.JsonMapper;
import org.apache.camel.tooling.model.Kind;

import java.io.StringWriter;
import java.util.*;

/**
 * Customize Camel Catalog for Kaoto.
 */
public class CamelCatalogProcessor {

    private final ObjectMapper jsonMapper;
    private final CamelCatalog camelCatalog;
    private final CamelYamlDslSchemaProcessor schemaProcessor;
    private final CamelCatalogVersionLoader camelCatalogVersionLoader;
    private final CatalogRuntime runtime;
    private final CamelCatalogSchemaEnhancer schemaEnhancer;

    public CamelCatalogProcessor(CamelCatalog camelCatalog, ObjectMapper jsonMapper,
                                 CamelYamlDslSchemaProcessor schemaProcessor, CatalogRuntime runtime,
                                 CamelCatalogVersionLoader camelCatalogVersionLoader) {
        this.jsonMapper = jsonMapper;
        this.schemaEnhancer = new CamelCatalogSchemaEnhancer(camelCatalog);
        this.camelCatalog = camelCatalog;
        this.schemaProcessor = schemaProcessor;
        this.runtime = runtime;
        this.camelCatalogVersionLoader = camelCatalogVersionLoader;
    }

    /**
     * Create Camel catalogs customized for Kaoto usage.
     *
     * @return
     */
    public Map<String, String> processCatalog() throws Exception {
        var answer = new LinkedHashMap<String, String>();
        ComponentGenerator componentGenerator = new ComponentGenerator(camelCatalog, runtime);
        var componentCatalog = Util.getPrettyJSON(componentGenerator.generate());
        var dataFormatCatalog = getDataFormatCatalog();
        var languageCatalog = getLanguageCatalog();
        var modelCatalog = getModelCatalog();
        EIPGenerator eipGenerator = new EIPGenerator(camelCatalog, camelCatalogVersionLoader.getCamelYamlDslSchema(),
                camelCatalogVersionLoader.getKaotoPatterns());
        var patternCatalog = Util.getPrettyJSON(eipGenerator.generate());
        EntityGenerator entityGenerator = new EntityGenerator(camelCatalog,
                camelCatalogVersionLoader.getCamelYamlDslSchema(),
                camelCatalogVersionLoader.getKubernetesSchema(),
                camelCatalogVersionLoader.getLocalSchemas());
        var entityCatalog = Util.getPrettyJSON(entityGenerator.generate());
        var loadBalancerCatalog = getLoadBalancerCatalog();
        var functionsCatalog = Util.getPrettyJSON(new FunctionsGenerator(camelCatalog, camelCatalogVersionLoader).generate());

        answer.put("components", componentCatalog);
        answer.put("dataformats", dataFormatCatalog);
        answer.put("languages", languageCatalog);
        answer.put("models", modelCatalog);
        answer.put("patterns", patternCatalog);
        answer.put("entities", entityCatalog);
        answer.put("loadbalancers", loadBalancerCatalog);
        answer.put("functions", functionsCatalog);
        return answer;
    }

    /**
     * Get aggregated Camel DataFormat catalog with a custom dataformat added.
     *
     * @return
     * @throws Exception
     */
    public String getDataFormatCatalog() throws Exception {
        var catalogMap = new LinkedHashMap<String, EipModel>();
        for (var name : camelCatalog.findDataFormatNames()) {
            var modelCatalog = camelCatalog.dataFormatModel(name);
            catalogMap.put(modelCatalog.getName(), camelCatalog.eipModel(name));
        }
        var answer = jsonMapper.createObjectNode();
        var dataFormatSchemaMap = schemaProcessor.getDataFormats();
        for (var entry : dataFormatSchemaMap.entrySet()) {
            var dataFormatName = entry.getKey();
            var dataFormatSchema = entry.getValue();
            EipModel eipModel = catalogMap.get(dataFormatName);
            List<EipOptionModel> eipModelOptions = List.of();
            if (eipModel != null) {
                eipModelOptions = eipModel.getOptions();
            }

            sortPropertiesAccordingToCamelCatalog(dataFormatSchema, eipModelOptions);

            var dataFormatCatalog = (EipModel) camelCatalog.model(Kind.eip, dataFormatName);
            if (dataFormatCatalog == null) {
                throw new Exception("DataFormat " + dataFormatName + " is not found in Camel model catalog.");
            }
            var json = JsonMapper.asJsonObject(dataFormatCatalog).toJson();
            var catalogTree = (ObjectNode) jsonMapper.readTree(json);
            catalogTree.set("propertiesSchema", dataFormatSchema);
            // setting required property to all the dataformats schema
            setRequiredToPropertiesSchema(dataFormatSchema, catalogTree);
            // Sanitize default values to ensure they match their declared types
            schemaEnhancer.fixDefaultValueTypesFromCamelSchema(dataFormatSchema);
            answer.set(dataFormatName, catalogTree);
        }
        StringWriter writer = new StringWriter();
        try (var jsonGenerator = new JsonFactory().createGenerator(writer).useDefaultPrettyPrinter()) {
            jsonMapper.writeTree(jsonGenerator, answer);
        }
        return writer.toString();
    }

    /**
     * Get Camel language catalog with a custom language added.
     *
     * @return
     * @throws Exception
     */
    public String getLanguageCatalog() throws Exception {
        var answer = jsonMapper.createObjectNode();
        var languageSchemaMap = schemaProcessor.getLanguages();
        var catalogMap = new LinkedHashMap<String, EipModel>();
        for (var name : camelCatalog.findLanguageNames()) {
            var modelCatalog = camelCatalog.languageModel(name);
            catalogMap.put(modelCatalog.getName(), camelCatalog.eipModel(name));
        }
        for (var entry : languageSchemaMap.entrySet()) {
            var languageName = entry.getKey();
            var languageSchema = entry.getValue();
            EipModel eipModel = catalogMap.get(languageName);
            List<EipOptionModel> eipModelOptions = List.of();
            if (eipModel != null) {
                eipModelOptions = eipModel.getOptions();
            }

            sortPropertiesAccordingToCamelCatalog(languageSchema, eipModelOptions);

            var languageCatalog = (EipModel) camelCatalog.model(Kind.eip, languageName);
            if (languageCatalog == null) {
                throw new Exception("Language " + languageName + " is not found in Camel model catalog.");
            }
            var json = JsonMapper.asJsonObject(languageCatalog).toJson();
            var catalogTree = (ObjectNode) jsonMapper.readTree(json);
            catalogTree.set("propertiesSchema", languageSchema);
            // setting required property to all the languages schema
            setRequiredToPropertiesSchema(languageSchema, catalogTree);
            // Sanitize default values to ensure they match their declared types
            schemaEnhancer.fixDefaultValueTypesFromCamelSchema(languageSchema);
            answer.set(languageName, catalogTree);
        }
        StringWriter writer = new StringWriter();
        try (var jsonGenerator = new JsonFactory().createGenerator(writer).useDefaultPrettyPrinter()) {
            jsonMapper.writeTree(jsonGenerator, answer);
        }
        return writer.toString();
    }

    public String getModelCatalog() throws Exception {
        var answer = jsonMapper.createObjectNode();
        camelCatalog.findModelNames().stream().sorted().forEach(name -> {
            try {
                var model = (EipModel) camelCatalog.model(Kind.eip, name);
                var json = JsonMapper.asJsonObject(model).toJson();
                var catalogNode = (ObjectNode) jsonMapper.readTree(json);

                answer.set(name, catalogNode);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
        StringWriter writer = new StringWriter();
        try (var jsonGenerator = new JsonFactory().createGenerator(writer).useDefaultPrettyPrinter()) {
            jsonMapper.writeTree(jsonGenerator, answer);
        }
        return writer.toString();
    }

    private void sortPropertiesAccordingToCamelCatalog(ObjectNode entitySchema,
                                                       List<EipOptionModel> entityCatalogOptions) {
        var sortedSchemaProperties = jsonMapper.createObjectNode();
        var camelYamlDslProperties = entitySchema.withObject("/properties").properties().stream().map(Map.Entry::getKey)
                .sorted(
                        new CamelYamlDSLKeysComparator(entityCatalogOptions))
                .toList();

        for (var propertyName : camelYamlDslProperties) {
            var propertySchema = entitySchema.withObject("/properties").withObject("/" + propertyName);
            sortedSchemaProperties.set(propertyName, propertySchema);
        }

        entitySchema.set("properties", sortedSchemaProperties);
    }

    /**
     * Get Camel LoadBalancer catalog with a custom loadbalancer added.
     *
     * @return
     * @throws Exception
     */
    public String getLoadBalancerCatalog() throws Exception {
        var answer = jsonMapper.createObjectNode();
        var loadBalancerSchemaMap = schemaProcessor.getLoadBalancers();
        for (var entry : loadBalancerSchemaMap.entrySet()) {
            var loadBalancerName = entry.getKey();
            var loadBalancerSchema = entry.getValue();
            var loadBalancerCatalog = (EipModel) camelCatalog.model(Kind.eip, loadBalancerName);
            if (loadBalancerCatalog == null) {
                throw new Exception("LoadBalancer " + loadBalancerName + " is not found in Camel model catalog.");
            }
            var json = JsonMapper.asJsonObject(loadBalancerCatalog).toJson();
            var catalogTree = (ObjectNode) jsonMapper.readTree(json);
            catalogTree.set("propertiesSchema", loadBalancerSchema);
            // setting required property to all the load-balancers schema
            setRequiredToPropertiesSchema(loadBalancerSchema, catalogTree);
            answer.set(loadBalancerName, catalogTree);
        }
        StringWriter writer = new StringWriter();
        var jsonGenerator = new JsonFactory().createGenerator(writer).useDefaultPrettyPrinter();
        jsonMapper.writeTree(jsonGenerator, answer);
        return writer.toString();
    }

    private void setRequiredToPropertiesSchema(ObjectNode camelYamlDslSchema, ObjectNode catalogModel) {
        List<String> required = new ArrayList<>();
        var camelYamlDslProperties = camelYamlDslSchema.withObject("/properties").properties().stream()
                .map(Map.Entry::getKey).toList();
        for (var propertyName : camelYamlDslProperties) {
            var catalogPropertySchema = catalogModel.withObject("/properties").withObject("/" + propertyName);
            if (catalogPropertySchema.has("required") && catalogPropertySchema.get("required").asBoolean()) {
                required.add(propertyName);
            }
        }
        catalogModel.withObject("/propertiesSchema").set("required", jsonMapper.valueToTree(required));
    }

}
