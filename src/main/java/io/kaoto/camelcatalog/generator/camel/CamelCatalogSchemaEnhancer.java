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
package io.kaoto.camelcatalog.generator.camel;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.camel.catalog.CamelCatalog;
import org.apache.camel.tooling.model.*;

import java.math.BigDecimal;
import java.util.*;

public class CamelCatalogSchemaEnhancer {
    private static final String PROPERTIES = "properties";
    private static final String SLASH_PROPERTIES = "/properties";
    private static final String DEFINITIONS = "definitions";
    private static final String REQUIRED = "required";
    private static final String ANY_OF = "anyOf";
    private static final String ONE_OF = "oneOf";
    private static final String TITLE = "title";
    private static final String DESCRIPTION = "description";
    private static final String TYPE = "type";
    private static final String DEFAULT = "default";
    private static final String FORMAT = "format";
    private static final String REF = "$ref";
    private static final String BOOLEAN = "boolean";
    private static final String STRING = "string";

    private final CamelCatalog camelCatalog;
    private final Map<String, String> javaTypeToModelName = new HashMap<>();
    private final Map<String, String> modelNameToJavaType = new HashMap<>();
    ObjectMapper jsonMapper = new ObjectMapper();

    public CamelCatalogSchemaEnhancer(CamelCatalog camelCatalog) {
        this.camelCatalog = camelCatalog;
        populateJavaTypeToModelNameMap();
    }

    /**
     * Fix default values in the JSON schema that are incorrectly typed as strings
     * This is a workaround for upstream Camel issue where default values are provided as strings
     * regardless of the property type (e.g., "false" instead of false for booleans)
     * See https://github.com/apache/camel/pull/19753
     *
     * @param schemaNode the JSON schema node to fix default values in
     */
    public void fixDefaultValueTypesFromCamelSchema(ObjectNode schemaNode) {
        // Process properties at the root level
        if (schemaNode.has(PROPERTIES)) {
            ObjectNode properties = (ObjectNode) schemaNode.get(PROPERTIES);
            properties.fields().forEachRemaining(entry -> {
                ObjectNode propertyNode = (ObjectNode) entry.getValue();
                fixDefaultValueInProperty(propertyNode);
            });
        }

        // Process definitions recursively
        if (schemaNode.has(DEFINITIONS)) {
            ObjectNode definitions = (ObjectNode) schemaNode.get(DEFINITIONS);
            definitions.fields().forEachRemaining(entry -> {
                ObjectNode definitionNode = (ObjectNode) entry.getValue();
                fixDefaultValueTypesFromCamelSchema(definitionNode);
            });
        }

        // Process anyOf/oneOf arrays
        fixDefaultValueInArrayFields(schemaNode, ANY_OF);
        fixDefaultValueInArrayFields(schemaNode, ONE_OF);
    }

    /**
     * Fill the required properties of the model in the schema if they are not already present
     *
     * @param modelKind the kind of the Camel model
     * @param modelName the name of the Camel model
     * @param modelNode the JSON schema node of the model
     */
    public void fillRequiredPropertiesIfNeeded(Kind modelKind, String modelName, ObjectNode modelNode) {
        BaseModel<? extends BaseOptionModel> model = camelCatalog.model(modelKind, modelName);
        if (model == null) {
            return;
        }

        fillRequiredPropertiesIfNeeded(model, modelNode);
    }

    /**
     * Fill the required properties of the model in the schema if they are not already present
     *
     * @param model     the Camel model
     * @param modelNode the JSON schema node of the model
     */
    public void fillRequiredPropertiesIfNeeded(BaseModel<? extends BaseOptionModel> model, ObjectNode modelNode) {
        ArrayList<String> requiredProperties = new ArrayList<>();

        if (modelNode.has(REQUIRED)) {
            modelNode.get(REQUIRED).elements().forEachRemaining(node -> {
                requiredProperties.add(node.asText());
            });
        }

        List<? extends BaseOptionModel> modelOptions = (model instanceof ComponentModel)
                ? ((ComponentModel) model).getEndpointOptions()
                : model.getOptions();

        modelOptions.forEach(option -> {
            if (option.isRequired() && modelNode.has(PROPERTIES)
                    && modelNode.get(PROPERTIES).has(option.getName())
                    && !modelNode.get(PROPERTIES).get(option.getName()).isEmpty()
                    && !requiredProperties.contains(option.getName())) {
                requiredProperties.add(option.getName());
            }
        });

        if (!requiredProperties.isEmpty()) {
            ArrayNode requiredNode = modelNode.putArray(REQUIRED);
            requiredProperties.forEach(requiredNode::add);
        }
    }

    /**
     * Sort schema properties according to the Camel catalog
     *
     * @param modelName the name of the Camel model
     * @param modelNode the JSON schema node of the model
     */
    public void sortPropertiesAccordingToCatalog(String modelName, ObjectNode modelNode) {
        EipModel model = camelCatalog.eipModel(modelName);
        if (model == null) {
            return;
        }

        sortPropertiesAccordingToCatalog(model, modelNode);
    }

    /**
     * Sort schema properties according to the Camel catalog
     *
     * @param model     the Camel model
     * @param modelNode the JSON schema node of the model
     */
    public void sortPropertiesAccordingToCatalog(EipModel model, ObjectNode modelNode) {
        sortPropertiesByOptions(modelNode, model.getOptions());
    }

    /**
     * Fill the group/label information of the model in the schema
     *
     * @param modelName the name of the Camel model
     * @param modelNode the JSON schema node of the model
     */
    public void fillPropertiesInformation(String modelName, ObjectNode modelNode) {
        EipModel model = camelCatalog.eipModel(modelName);
        if (model == null) {
            return;
        }

        fillPropertiesInformation(model, modelNode);
    }

    /**
     * Fill the group/label/format/deprecated/default information of the model in the schema
     *
     * @param model     the Camel Base model
     * @param modelNode the JSON schema node of the model
     */
    public void fillPropertiesInformation(BaseModel<? extends BaseOptionModel> model, ObjectNode modelNode) {
        List<? extends BaseOptionModel> modelOptions = model.getOptions();

        modelNode.withObject(PROPERTIES).fields().forEachRemaining(entry -> {
            String propertyName = entry.getKey();
            ObjectNode propertyNode = (ObjectNode) entry.getValue();
            if (propertyNode.isEmpty()) {
                return;
            }

            Optional<? extends BaseOptionModel> modelOption =
                    modelOptions.stream().filter(option -> option.getName().equals(propertyName)).findFirst();
            if (modelOption.isEmpty()) {
                return;
            }

            fillPropertyInformation(modelOption.get(), propertyNode);
        });
    }

    /**
     * Fill the group/label/format/deprecated/default information of the model in the property
     *
     * @param modelOption  the Camel Base model
     * @param propertyNode the JSON node of the property
     */
    public void fillPropertyInformation(BaseOptionModel modelOption, ObjectNode propertyNode) {
        addTitleAndDescription(modelOption, propertyNode);
        addGroupInfo(modelOption, propertyNode);
        addFormatInfo(modelOption, propertyNode);
        addDeprecateInfo(modelOption, propertyNode);
        addDefaultInfo(modelOption, propertyNode);
    }

    void addTitleAndDescription(BaseOptionModel modelOption, ObjectNode propertyNode) {
        var displayName = modelOption.getDisplayName();
        if (!propertyNode.has(TITLE) && displayName != null) {
            propertyNode.put(TITLE, displayName);
        }

        var description = modelOption.getDescription();
        if (!propertyNode.has(DESCRIPTION) && description != null) {
            propertyNode.put(DESCRIPTION, description);
        }
    }

    /**
     * Get the Camel model by its Java type
     *
     * @param javaType the Java type string of the Camel model, e.g. "org.apache.camel.language.simple.SimpleExpression"
     * @return the Camel model
     */
    public EipModel getCamelModelByJavaType(String javaType) {
        return camelCatalog.eipModel(javaTypeToModelName.get(javaType));
    }

    public String getJavaTypeByModelName(String modelName) {
        String javaType = modelNameToJavaType.get(modelName);
        if (javaType == null) {
            EipModel model = camelCatalog.eipModel(modelName);
            if (model != null) {
                javaType = model.getJavaType();
                modelNameToJavaType.put(modelName, javaType);
            }
        }
        return javaType;
    }

    /**
     * Fill the JSON schema details of the model in the schema
     *
     * @param modelNode the JSON schema node of the model
     */
    public void fillSchemaInformation(ObjectNode modelNode) {
        modelNode.put("$schema", "http://json-schema.org/draft-07/schema#");
        if (!modelNode.has(TYPE)) {
            modelNode.put(TYPE, "object");
        }
    }

    /**
     * Fix default value type in a single property node
     *
     * @param propertyNode the property node to fix
     */
    private void fixDefaultValueInProperty(ObjectNode propertyNode) {
        if (!propertyNode.has(DEFAULT) || !propertyNode.has(TYPE)) {
            return;
        }

        var defaultValue = propertyNode.get(DEFAULT);
        var propertyType = propertyNode.get(TYPE).asText();

        // Only process if default is currently a string
        if (!defaultValue.isTextual()) {
            return;
        }

        String defaultValueString = defaultValue.asText();

        // Fix boolean defaults
        if (BOOLEAN.equals(propertyType)) {
            if ("true".equals(defaultValueString)) {
                propertyNode.put(DEFAULT, true);
            } else if ("false".equals(defaultValueString)) {
                propertyNode.put(DEFAULT, false);
            }
        }
        // Fix number/integer defaults
        else if ("number".equals(propertyType) || "integer".equals(propertyType)) {
            try {
                // Check if it's a decimal number
                if (defaultValueString.contains(".")) {
                    propertyNode.put(DEFAULT, Double.parseDouble(defaultValueString));
                } else {
                    propertyNode.put(DEFAULT, Long.parseLong(defaultValueString));
                }
            } catch (NumberFormatException e) {
                // Keep as string if parsing fails
            }
        }
    }

    /**
     * Fix default values in array fields (anyOf, oneOf)
     *
     * @param node      the node containing the array field
     * @param arrayName the name of the array field
     */
    private void fixDefaultValueInArrayFields(ObjectNode node, String arrayName) {
        if (!node.has(arrayName)) {
            return;
        }

        var array = (ArrayNode) node.get(arrayName);
        array.forEach(element -> {
            if (element.isObject()) {
                var elementNode = (ObjectNode) element;
                fixDefaultValueTypesFromCamelSchema(elementNode);
            }
        });
    }

    /**
     * Fill the expression format property in the oneOf nodes
     * This is used to provide a hint to the UI that this oneOf
     * is an expression. Example of this is the "setHeader" EIP or the
     * "resequence" EIP
     *
     * @param modelNode the JSON schema node of the model
     */
    public void fillModelFormatInOneOf(ObjectNode modelNode) {
        if (modelNode.has(ANY_OF) && modelNode.get(ANY_OF).isArray()) {
            modelNode.withArray(ANY_OF).elements().forEachRemaining(node -> {
                fillModelFormatInOneOf((ObjectNode) node);
            });
        }

        if (!modelNode.has(ONE_OF)) {
            return;
        }

        modelNode.withArray(ONE_OF).elements().forEachRemaining(node -> {
            if (node.has(REF) && node.get(REF).asText().contains("org.apache.camel.model.language.ExpressionDefinition")) {
                modelNode.put(FORMAT, "expression");
            } else if (node.has(PROPERTIES) && node.get(PROPERTIES).has("customLoadBalancer")
                    && node.get(PROPERTIES).get("customLoadBalancer").has(REF)
                    && node.get(PROPERTIES).get("customLoadBalancer").get(REF).asText().contains("org.apache.camel.model.loadbalancer")) {
                modelNode.put(FORMAT, "loadBalancerType");
            } else if (node.has(PROPERTIES) && node.get(PROPERTIES).has("asn1")
                    && node.get(PROPERTIES).get("asn1").has(REF)
                    && node.get(PROPERTIES).get("asn1").get(REF).asText().contains("org.apache.camel.model.dataformat")) {
                modelNode.put(FORMAT, "dataFormatType");
            } else if (node.has(PROPERTIES) && node.get(PROPERTIES).has("deadLetterChannel")
                    && node.get(PROPERTIES).get("deadLetterChannel").has(REF)
                    && node.get(PROPERTIES).get("deadLetterChannel").get(REF).asText().contains("org.apache.camel.model.errorhandler")) {
                modelNode.put(FORMAT, "errorHandlerType");
            }
        });
    }

    /**
     * Populate the JavaType to ModelName map
     */
    private void populateJavaTypeToModelNameMap() {
        camelCatalog.findModelNames().forEach(modelName -> {
            EipModel model = camelCatalog.eipModel(modelName);
            if (model != null) {
                javaTypeToModelName.put(model.getJavaType(), modelName);
                modelNameToJavaType.put(modelName, model.getJavaType());
            }
        });
    }

    /**
     * Enhance the parameters property in the schema by adding metadata.
     * <p>
     * This method updates existing "parameters" properties in JSON schemas for endpoint-related
     * definitions (e.g., from, to, kamelet) by adding standard metadata fields.
     * It does NOT create new parameters properties - it only enhances existing ones.
     * <p>
     *
     * @param javaType the fully qualified Java type of the Camel model (e.g., "org.apache.camel.model.ToDefinition")
     * @param schema   the JSON schema node to enhance
     */
    public void enhanceParametersProperty(String javaType, ObjectNode schema) {
        if (javaType == null) {
            return;
        }

        // Handle schemas with oneOf (multiple possible schema variants)
        // Each variant needs to be enhanced independently
        if (schema.has(ONE_OF)) {
            ArrayNode oneOfArray = (ArrayNode) schema.get(ONE_OF);
            oneOfArray.forEach(option -> {
                if (option.isObject()) {
                    enhanceParametersInNode((ObjectNode) option);
                }
            });
        } else {
            // Handle simple schema without oneOf
            enhanceParametersInNode(schema);
        }
    }

    /**
     * Enhance the parameters property within a schema node by adding metadata.
     * <p>
     * This method only adds metadata to existing "parameters" properties.
     * It does NOT create new parameters properties.
     * <p>
     *
     * @param node the JSON schema node to enhance
     */
    private void enhanceParametersInNode(ObjectNode node) {
        if (!node.has(PROPERTIES)) {
            return;
        }

        ObjectNode properties = node.withObject(SLASH_PROPERTIES);

        if (properties.has("parameters")) {
            ObjectNode parameters = (ObjectNode) properties.get("parameters");
            setParametersMetadata(parameters);
        }
    }

    /**
     * Set the standard metadata for an endpoint properties parameters object.
     * <p>
     * Sets only the essential metadata fields. The 'properties' and 'required' fields
     * are intentionally omitted here as they should be populated dynamically based on
     * the actual endpoint component being used.
     *
     * @param parameters the parameters object node to configure
     */
    private void setParametersMetadata(ObjectNode parameters) {
        parameters.put(TYPE, "object");
        parameters.put(TITLE, "Endpoint Properties");
        parameters.put(DESCRIPTION, "The key-value pairs of the properties to configure this endpoint");
    }

    private void addGroupInfo(BaseOptionModel modelOption, ObjectNode propertyNode) {
        String group =
                modelOption.getGroup() != null ? modelOption.getGroup() : modelOption.getLabel();
        if (group == null) {
            return;
        }

        if (propertyNode.has("$comment")) {
            propertyNode.put("$comment", propertyNode.get("$comment").asText() + "|group:" + group);
        } else {
            propertyNode.put("$comment", "group:" + group);
        }
    }

    private void addFormatInfo(BaseOptionModel modelOption, ObjectNode propertyNode) {
        List<String> format = new ArrayList<>();
        if (propertyNode.has(FORMAT)) {
            format.add(propertyNode.get(FORMAT).asText());
        }

        var propertyType = modelOption.getType();
        String bean =
                "object".equals(propertyType) && !propertyNode.has(REF) ? modelOption.getJavaType() : null;

        if (bean != null && !bean.startsWith("java.util.Map")) {
            format.add("bean:" + bean);
            propertyNode.put(TYPE, "string");
        }

        if ("duration".equals(propertyType)) {
            format.add("duration");
            propertyNode.put(TYPE, STRING);
        }

        if (modelOption.isSecret()) {
            format.add("password");
        }

        if ("org.apache.camel.model.ExpressionSubElementDefinition".equals(modelOption.getJavaType())) {
            format.add("expressionProperty");
        }

        if (!format.isEmpty()) {
            propertyNode.put(FORMAT, String.join("|", format));
        }
    }

    private void addDeprecateInfo(BaseOptionModel modelOption, ObjectNode propertyNode) {
        boolean isDeprecated = modelOption.isDeprecated();
        if (isDeprecated) {
            propertyNode.put("deprecated", true);
        }
    }

    public void sortPropertiesByOptions(ObjectNode entitySchema, List<? extends BaseOptionModel> options) {
        var sortedSchemaProperties = jsonMapper.createObjectNode();
        var propertiesNode = entitySchema.get(PROPERTIES);
        if (propertiesNode == null || !propertiesNode.isObject()) {
            return;
        }
        var properties = ((ObjectNode) propertiesNode).properties().stream()
                .map(Map.Entry::getKey)
                .sorted(new CamelYamlDSLKeysComparator(options))
                .toList();

        for (var propertyName : properties) {
            var propertySchema = propertiesNode.get(propertyName);
            sortedSchemaProperties.set(propertyName, propertySchema);
        }

        entitySchema.set(PROPERTIES, sortedSchemaProperties);
    }

    public void setRequiredToPropertiesSchema(ObjectNode yamlDslSchema, ObjectNode catalogModel) {
        List<String> required = new ArrayList<>();
        var yamlDslProperties = yamlDslSchema.withObject(SLASH_PROPERTIES).properties().stream()
                .map(Map.Entry::getKey).toList();
        for (var propertyName : yamlDslProperties) {
            var catalogPropertySchema = catalogModel.path(PROPERTIES).path(propertyName);
            if (catalogPropertySchema.has(REQUIRED) && catalogPropertySchema.get(REQUIRED).asBoolean()) {
                required.add(propertyName);
            }
        }
        catalogModel.withObject("/propertiesSchema").set(REQUIRED, jsonMapper.valueToTree(required));
    }

    private void addDefaultInfo(BaseOptionModel modelOption, ObjectNode propertyNode) {
        var defaultValue = modelOption.getDefaultValue();
        if (defaultValue != null && !propertyNode.has(DEFAULT)) {
            var propertyType = modelOption.getType();
            var schemaPropTypeNode = propertyNode.get(TYPE);
            if (BOOLEAN.equals(schemaPropTypeNode.asText())) {
                // some boolean properties have its type as string in the catalog. prioritize the schema if type is declared.
                propertyType = BOOLEAN;
            }

            if ("integer".equals(propertyType) && !(defaultValue instanceof String)) {
                propertyNode.put(DEFAULT, ((BigDecimal) defaultValue).intValue());
            } else if (BOOLEAN.equals(propertyType)) {
                if ("true".equals(defaultValue.toString())) {
                    propertyNode.put(DEFAULT, true);
                } else if ("false".equals(defaultValue.toString())) {
                    propertyNode.put(DEFAULT, false);
                }
            } else {
                propertyNode.put(DEFAULT, defaultValue.toString());
            }
        }
    }
}
