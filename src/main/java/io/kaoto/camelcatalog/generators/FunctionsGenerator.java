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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.kaoto.camelcatalog.maven.CamelCatalogVersionLoader;
import io.kaoto.camelcatalog.model.KaotoFunction;
import org.apache.camel.catalog.CamelCatalog;
import org.apache.camel.tooling.model.LanguageModel;

import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class FunctionsGenerator implements Generator {
    private static final Logger LOGGER = Logger.getLogger(FunctionsGenerator.class.getName());
    private final ObjectMapper jsonMapper;
    private final CamelCatalog camelCatalog;
    private final CamelCatalogVersionLoader camelCatalogVersionLoader;

    public FunctionsGenerator(CamelCatalog camelCatalog, CamelCatalogVersionLoader camelCatalogVersionLoader) {
        this.jsonMapper = new ObjectMapper();
        this.camelCatalog = camelCatalog;
        this.camelCatalogVersionLoader = camelCatalogVersionLoader;
    }

    @Override
    public Map<String, ObjectNode> generate() {
        Map<String, ObjectNode> functionsMap = new HashMap<>();

        addLanguagesFunction(functionsMap);
        includeLocalSimpleLangFunctionsIfNeeded(functionsMap);

        return functionsMap;
    }

    void addLanguagesFunction(Map<String, ObjectNode> rootNode) {
        camelCatalog.findLanguageNames().forEach(languageName -> {
            var languageFunctions = camelCatalog.languageModel(languageName).getFunctions();
            if (languageFunctions.isEmpty()) return;

            var functionsMap = languageFunctions.stream()
                    .sorted(Comparator.comparing(LanguageModel.LanguageFunctionModel::getName))
                    .collect(Collectors.toMap(LanguageModel.LanguageFunctionModel::getName,
                            function -> getKaotoFunctionFromLanguageFunction(function.getName(), function),
                            (e1, e2) -> e1, LinkedHashMap::new));
            rootNode.put(languageName, jsonMapper.valueToTree(functionsMap));
        });
    }

    void includeLocalSimpleLangFunctionsIfNeeded(Map<String, ObjectNode> rootNode) {
        if (rootNode.get("simple") == null) {
            LOGGER.info("Simple language functions not found in the catalog, generating locally.");
        }

        rootNode.computeIfAbsent("simple", simpleNode -> {
            var simpleLangString = camelCatalogVersionLoader.getResourceLoader()
                    .getResourceAsString("functions/simple-language-functions.json");

            Map<String, LanguageModel.LanguageFunctionModel> simpleLangFunctionsNode;
            try {
                simpleLangFunctionsNode = jsonMapper.readValue(simpleLangString, new TypeReference<>() {
                });
            } catch (JsonProcessingException e) {
                LOGGER.warning("Failed to parse simple language functions JSON: " + e.getMessage());
                simpleLangFunctionsNode = new HashMap<>();
            }

            var functionsMap = simpleLangFunctionsNode.entrySet().stream()
                    .sorted(Map.Entry.comparingByKey())
                    .collect(Collectors.toMap(Map.Entry::getKey,
                            entry -> getKaotoFunctionFromLanguageFunction(entry.getKey(), entry.getValue()),
                            (e1, e2) -> e1, LinkedHashMap::new));
            return jsonMapper.valueToTree(functionsMap);
        });
    }

    private KaotoFunction getKaotoFunctionFromLanguageFunction(String functionName,
                                                               LanguageModel.LanguageFunctionModel camelFunction) {
        var kaotoFunction = new KaotoFunction();
        kaotoFunction.setName(functionName);
        kaotoFunction.setDisplayName(camelFunction.getDisplayName());
        kaotoFunction.setDescription(camelFunction.getDescription());
        kaotoFunction.setReturnType(camelFunction.getJavaType());

        return kaotoFunction;
    }
}
