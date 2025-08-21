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

import io.kaoto.camelcatalog.maven.CamelCatalogVersionLoader;

import java.io.IOException;
import java.io.InputStream;
import java.net.JarURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.logging.Level;
import java.util.logging.Logger;

import static io.kaoto.camelcatalog.model.Constants.CAMEL_YAML_DSL_FILE_NAME;

public class SchemasGenerator {
    private static final Logger LOGGER = Logger.getLogger(SchemasGenerator.class.getName());
    private static final String XSD_RESOURCE_PATH = "org/apache/camel/catalog/schemas";
    private static final String XSD_FILE_SUFFIX = ".xsd";

    private final CamelCatalogVersionLoader versionLoader;
    private final ClassLoader classLoader;

    public SchemasGenerator(CamelCatalogVersionLoader versionLoader, ClassLoader classLoader) {
        this.versionLoader = versionLoader;
        this.classLoader = classLoader;
    }

    public Map<String, String> generate() {
        var schemas = new LinkedHashMap<String, String>();

        addCamelYamlDslSchema(schemas);
        addXSDSchemas(schemas);
        addCRDSchemas(schemas);

        return schemas;
    }

    private void addCamelYamlDslSchema(Map<String, String> schemas) {
        String yamlDslSchema = versionLoader.getCamelYamlDslSchema();
        if (yamlDslSchema != null) {
            // Convert from draft-04 to draft-07 (same as existing code)
            String schema07 = yamlDslSchema.replace(
                    "http://json-schema.org/draft-04/schema#", 
                    "http://json-schema.org/draft-07/schema#");
            schemas.put(CAMEL_YAML_DSL_FILE_NAME, schema07);
            LOGGER.log(Level.INFO, "Added Camel YAML DSL schema");
        } else {
            LOGGER.log(Level.WARNING, "Camel YAML DSL schema is not loaded");
        }
    }

    private void addXSDSchemas(Map<String, String> schemas) {
        try {
            Iterator<URL> it = classLoader.getResources(XSD_RESOURCE_PATH).asIterator();

            while (it.hasNext()) {
                URL resourceUrl = it.next();
                LOGGER.log(Level.FINE, "Processing XSD resource URL: {0}", resourceUrl);

                if ("jar".equals(resourceUrl.getProtocol())) {
                    loadXSDSchemasFromJar(resourceUrl, schemas);
                } else {
                    LOGGER.log(Level.FINE, "Non-jar resource URL found for XSD schemas: {0}", resourceUrl);
                }
            }
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Error loading XSD schemas from classpath", e);
        }
    }

    private void loadXSDSchemasFromJar(URL resourceUrl, Map<String, String> schemas) {
        try {
            JarURLConnection connection = (JarURLConnection) resourceUrl.openConnection();
            try (JarFile jarFile = connection.getJarFile()) {
                Enumeration<JarEntry> entries = jarFile.entries();
                String entryBaseName = connection.getEntryName();

                while (entries.hasMoreElements()) {
                    JarEntry entry = entries.nextElement();
                    if (entry.getName().startsWith(entryBaseName) && 
                        !entry.isDirectory() && 
                        entry.getName().endsWith(XSD_FILE_SUFFIX)) {

                        LOGGER.log(Level.INFO, "Loading XSD schema: {0}", entry.getName());
                        
                        try (InputStream inputStream = jarFile.getInputStream(entry)) {
                            String schemaContent = readInputStreamAsString(inputStream);
                            String schemaName = extractSchemaName(entry.getName(), entryBaseName);
                            schemas.put(schemaName, schemaContent);
                        } catch (IOException e) {
                            LOGGER.log(Level.WARNING, "Error reading XSD schema: " + entry.getName(), e);
                        }
                    }
                }
            }
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Error processing jar for XSD schemas: " + resourceUrl, e);
        }
    }

    private void addCRDSchemas(Map<String, String> schemas) {
        List<String> crdList = versionLoader.getCamelKCRDs();
        if (crdList.isEmpty()) {
            LOGGER.log(Level.WARNING, "CamelK CRDs are not loaded");
            return;
        }

        io.kaoto.camelcatalog.generators.CRDGenerator crdGenerator = new io.kaoto.camelcatalog.generators.CRDGenerator(crdList);
        Map<String, String> crdMap = crdGenerator.generate();
        crdMap.forEach((name, content) -> {
            schemas.put(name, content);
            LOGGER.log(Level.INFO, "Added CRD schema: {0}", name);
        });
    }

    private String readInputStreamAsString(InputStream inputStream) throws IOException {
        try (Scanner scanner = new Scanner(inputStream, StandardCharsets.UTF_8.name())) {
            scanner.useDelimiter("\\A");
            return scanner.hasNext() ? scanner.next() : "";
        }
    }

    private String extractSchemaName(String entryName, String basePath) {
        return entryName
                .replace(basePath + "/", "")
                .replace(XSD_FILE_SUFFIX, "");
    }
}