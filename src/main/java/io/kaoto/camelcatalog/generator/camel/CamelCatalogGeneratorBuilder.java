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

import io.kaoto.camelcatalog.generator.CatalogGenerator;
import io.kaoto.camelcatalog.generator.CatalogGeneratorBuilder;

import java.io.File;

import io.kaoto.camelcatalog.maven.CamelCatalogVersionLoader;
import io.kaoto.camelcatalog.model.CatalogRuntime;
import io.kaoto.camelcatalog.model.ResolvedVersions;

/**
 * Collects the camel metadata files such as catalog and schema and
 * tailors them to fit with Kaoto needs.
 * This class expects the following directory structure under inputDirectory:
 *
 * <ul>
 * <li>catalog/ - The root directory of extracted camel-catalog</li>
 * <li>crds/ - Holds Camel K CRD YAML files</li>
 * <li>kamelets/ - Holds Kamelet definition YAML files</li>
 * <li>schema/ - Holds Camel YAML DSL JSON schema files</li>
 * </ul>
 *
 * In addition to what is generated from above input files, this plugin
 * generates index.json file that holds the list of all the generated.
 */
public class CamelCatalogGeneratorBuilder implements CatalogGeneratorBuilder {

    private CatalogRuntime runtime;
    private String catalogVersion;
    private String kameletsVersion;
    private String camelKCRDsVersion;
    private File outputDirectory;
    private boolean verbose = false;
    private ResolvedVersions resolvedVersions;

    public CamelCatalogGeneratorBuilder withRuntime(CatalogRuntime runtime) {
        this.runtime = runtime;
        return this;
    }

    public CamelCatalogGeneratorBuilder withCatalogVersion(String camelCatalogVersion) {
        this.catalogVersion = camelCatalogVersion;
        return this;
    }

    public CamelCatalogGeneratorBuilder withKameletsVersion(String kameletsVersion) {
        this.kameletsVersion = kameletsVersion;
        return this;
    }

    public CamelCatalogGeneratorBuilder withCamelKCRDsVersion(String camelKCRDsVersion) {
        this.camelKCRDsVersion = camelKCRDsVersion;
        return this;
    }

    public CamelCatalogGeneratorBuilder withOutputDirectory(File outputDirectory) {
        this.outputDirectory = outputDirectory;
        return this;
    }

    public CamelCatalogGeneratorBuilder withVerbose(boolean verbose) {
        this.verbose = verbose;
        return this;
    }

    public CamelCatalogGeneratorBuilder withResolvedVersions(ResolvedVersions resolvedVersions) {
        this.resolvedVersions = resolvedVersions;
        return this;
    }

    public CatalogRuntime getRuntime() {
        return runtime;
    }

    public boolean isVerbose() {
        return verbose;
    }

    @Override
    public CatalogGenerator build() {
        var catalogGenerator = new CamelCatalogGenerator(new CamelCatalogVersionLoader(runtime, verbose), outputDirectory);
        catalogGenerator.setCamelCatalogVersion(catalogVersion);
        catalogGenerator.setKameletsVersion(kameletsVersion);
        catalogGenerator.setCamelKCRDsVersion(camelKCRDsVersion);
        catalogGenerator.setResolvedVersions(resolvedVersions);
        return catalogGenerator;
    }

}
