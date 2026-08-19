/*
 * Copyright (C) 2026 Red Hat, Inc.
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

package io.kaoto.camelcatalog.generator.templates;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import io.kaoto.camelcatalog.generator.CatalogGenerator;
import io.kaoto.camelcatalog.generator.Util;
import io.kaoto.camelcatalog.maven.KaotoMavenVersionManager;
import io.kaoto.camelcatalog.maven.ResourceLoader;
import io.kaoto.camelcatalog.model.CatalogDefinition;
import io.kaoto.camelcatalog.model.CatalogDefinitionEntry;
import io.kaoto.camelcatalog.model.CatalogRuntime;
import io.kaoto.camelcatalog.model.StarterTemplateId;

import java.io.File;
import java.nio.file.Files;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class StarterTemplatesGenerator implements CatalogGenerator {

    private static final Logger LOGGER = Logger.getLogger(StarterTemplatesGenerator.class.getName());
    private static final String RESOURCE_FOLDER = "starter-templates";
    private static final String CATALOG_VERSION = "1";

    /**
     * Maps the filename-without-extension key (as produced by ResourceLoader) to
     * the {@link StarterTemplateId} enum value.
     *
     * ResourceLoader strips the fileSuffix from the filename, so:
     *   "camel-route.camel.yaml" loaded with ".yaml" → key "camel-route.camel"
     *   "camel-route.xml"        loaded with ".xml"  → key "camel-route"
     */
    private static final Map<String, StarterTemplateId> TEMPLATE_IDS = Map.of(
            "camel-route.camel",      StarterTemplateId.CAMEL_ROUTE_YAML,
            "camel-route",            StarterTemplateId.CAMEL_ROUTE_XML,
            "pipe.pipe",              StarterTemplateId.PIPE_YAML,
            "kamelet-source.kamelet", StarterTemplateId.KAMELET_SOURCE_YAML,
            "kamelet-action.kamelet", StarterTemplateId.KAMELET_ACTION_YAML,
            "kamelet-sink.kamelet",   StarterTemplateId.KAMELET_SINK_YAML,
            "citrus-test.citrus",     StarterTemplateId.CITRUS_YAML
    );

    private static final Map<StarterTemplateId, String> TEMPLATE_DESCRIPTIONS = Map.of(
            StarterTemplateId.CAMEL_ROUTE_YAML,    "Camel route starter using the YAML DSL",
            StarterTemplateId.CAMEL_ROUTE_XML,     "Camel route starter using the XML DSL",
            StarterTemplateId.PIPE_YAML,           "Camel Pipe connecting a timer-source to a log-sink",
            StarterTemplateId.KAMELET_SOURCE_YAML, "Kamelet source starter producing periodic messages",
            StarterTemplateId.KAMELET_ACTION_YAML, "Kamelet action starter applying a delay",
            StarterTemplateId.KAMELET_SINK_YAML,   "Kamelet sink starter logging received messages",
            StarterTemplateId.CITRUS_YAML,         "Citrus test starter in YAML format"
    );

    private final File outputDirectory;
    private final ObjectMapper jsonMapper;
    private final ResourceLoader resourceLoader;

    StarterTemplatesGenerator(File outputDirectory) {
        this.outputDirectory = outputDirectory;
        this.jsonMapper = new ObjectMapper()
                .configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true);
        KaotoMavenVersionManager versionManager = new KaotoMavenVersionManager();
        this.resourceLoader = new ResourceLoader(versionManager, false);
    }

    @Override
    public CatalogDefinition generate() {
        var catalogDefinition = new CatalogDefinition();
        catalogDefinition.setName("Kaoto Starter Templates");
        catalogDefinition.setVersion(CATALOG_VERSION);
        catalogDefinition.setRuntime(CatalogRuntime.StarterTemplates);

        var allTemplates = new LinkedHashMap<String, String>();
        resourceLoader.loadResourcesFromFolderAsString(RESOURCE_FOLDER, allTemplates, ".yaml");
        resourceLoader.loadResourcesFromFolderAsString(RESOURCE_FOLDER, allTemplates, ".xml");

        for (var entry : allTemplates.entrySet()) {
            String key = entry.getKey();
            String content = entry.getValue();

            StarterTemplateId templateId = TEMPLATE_IDS.get(key);
            if (templateId == null) {
                LOGGER.warning("Unknown starter template file, skipping: " + key);
                continue;
            }

            // Keys with a dot came from .yaml files; keys without a dot came from .xml files
            String extension = key.contains(".") ? ".yaml" : ".xml";
            String outputFileName = key.replace(".", "-") + "-" + Util.generateHash(content) + extension;

            try {
                Files.writeString(outputDirectory.toPath().resolve(outputFileName), content);
                String description = TEMPLATE_DESCRIPTIONS.getOrDefault(templateId, templateId.getId());
                catalogDefinition.getCatalogs().put(templateId.getId(),
                        new CatalogDefinitionEntry(templateId.getId(), description, CATALOG_VERSION, outputFileName));
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Error writing template file: " + outputFileName, e);
                throw new IllegalStateException("Could not write starter template: " + outputFileName, e);
            }
        }

        try {
            String indexContent = jsonMapper.writeValueAsString(catalogDefinition);
            String indexFileName = "index-" + Util.generateHash(indexContent) + ".json";
            catalogDefinition.setFileName(indexFileName);
            Util.createTabWriter(jsonMapper).writeValue(
                    outputDirectory.toPath().resolve(indexFileName).toFile(), catalogDefinition);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error writing starter templates index", e);
            throw new IllegalStateException("Could not write starter templates index", e);
        }

        return catalogDefinition;
    }
}
