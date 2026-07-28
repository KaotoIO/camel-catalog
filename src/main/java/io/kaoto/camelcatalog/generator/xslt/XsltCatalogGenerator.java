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
package io.kaoto.camelcatalog.generator.xslt;

import static io.kaoto.camelcatalog.model.Constants.XPATH_FUNCTIONS;
import static io.kaoto.camelcatalog.model.Constants.XPATH_FUNCTIONS_FILENAME;

import java.io.File;
import java.nio.file.Files;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import io.kaoto.camelcatalog.generator.CatalogGenerator;
import io.kaoto.camelcatalog.generator.Util;
import io.kaoto.camelcatalog.generators.XPathFunctionsGenerator;
import io.kaoto.camelcatalog.model.CatalogDefinition;
import io.kaoto.camelcatalog.model.CatalogDefinitionEntry;
import io.kaoto.camelcatalog.model.CatalogRuntime;
import io.kaoto.camelcatalog.model.ResolvedVersions;

/**
 * Generates the XSLT/XPath function catalog from bundled W3C specification data.
 * Unlike Camel and Citrus generators, this does not resolve Maven artifacts at runtime
 * because the W3C XPath 3.1 and XSLT 3.0 specifications are stable standards whose
 * source XML is bundled as classpath resources.
 */
public class XsltCatalogGenerator implements CatalogGenerator {

    private static final Logger LOGGER = Logger.getLogger(XsltCatalogGenerator.class.getName());

    private final String catalogVersion;
    private final File outputDirectory;
    private final ObjectMapper jsonMapper;
    private ResolvedVersions resolvedVersions;

    public XsltCatalogGenerator(String catalogVersion, File outputDirectory) {
        this(catalogVersion, outputDirectory, false);
    }

    public XsltCatalogGenerator(String catalogVersion, File outputDirectory, boolean verbose) {
        this.catalogVersion = catalogVersion;
        this.outputDirectory = outputDirectory;
        this.jsonMapper = new ObjectMapper()
                .configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true);
        if (verbose) {
            LOGGER.setLevel(Level.FINE);
        }
    }

    public void setResolvedVersions(ResolvedVersions resolvedVersions) {
        this.resolvedVersions = resolvedVersions;
    }

    @Override
    public CatalogDefinition generate() {
        try {
            var catalog = Util.getPrettyJSON(
                    new XPathFunctionsGenerator(getClass().getClassLoader()).generate());
            var catalogFileName = XPATH_FUNCTIONS_FILENAME + ".json";

            Files.writeString(outputDirectory.toPath().resolve(catalogFileName), catalog);

            var catalogDefinition = new CatalogDefinition();
            catalogDefinition.setName("XSLT " + catalogVersion);
            catalogDefinition.setRuntime(CatalogRuntime.XSLT);
            catalogDefinition.setVersion(catalogVersion);
            catalogDefinition.setFileName("index.json");
            catalogDefinition.getCatalogs().put(XPATH_FUNCTIONS,
                    new CatalogDefinitionEntry(
                            XPATH_FUNCTIONS,
                            "XPath 3.1 and XSLT 3.0 function catalog",
                            catalogVersion,
                            catalogFileName));

            if (resolvedVersions != null) {
                catalogDefinition.setCamelCatalogVersion(resolvedVersions.camelCatalogVersion());
                catalogDefinition.setRuntimeProviderVersion(resolvedVersions.runtimeProviderVersion());
                catalogDefinition.setFrameworkVersion(resolvedVersions.frameworkVersion());
            }

            Util.createTabWriter(jsonMapper).writeValue(
                    outputDirectory.toPath().resolve("index.json").toFile(), catalogDefinition);

            return catalogDefinition;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error generating XPath functions catalog", e);
            throw new RuntimeException("Error generating XPath functions catalog", e);
        }
    }
}
