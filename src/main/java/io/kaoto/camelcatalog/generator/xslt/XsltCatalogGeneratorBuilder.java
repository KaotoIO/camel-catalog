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

import java.io.File;

import io.kaoto.camelcatalog.generator.CatalogGenerator;
import io.kaoto.camelcatalog.generator.CatalogGeneratorBuilder;
import io.kaoto.camelcatalog.model.ResolvedVersions;

/**
 * Fluent builder for {@link XsltCatalogGenerator}.
 * Follows the same pattern as {@link io.kaoto.camelcatalog.generator.citrus.CitrusCatalogGeneratorBuilder}.
 */
public class XsltCatalogGeneratorBuilder implements CatalogGeneratorBuilder {

    private String catalogVersion;
    private File outputDirectory;
    private boolean verbose = false;
    private ResolvedVersions resolvedVersions;

    public XsltCatalogGeneratorBuilder withCatalogVersion(String catalogVersion) {
        this.catalogVersion = catalogVersion;
        return this;
    }

    public XsltCatalogGeneratorBuilder withOutputDirectory(File outputDirectory) {
        this.outputDirectory = outputDirectory;
        return this;
    }

    public XsltCatalogGeneratorBuilder withVerbose(boolean verbose) {
        this.verbose = verbose;
        return this;
    }

    public XsltCatalogGeneratorBuilder withResolvedVersions(ResolvedVersions resolvedVersions) {
        this.resolvedVersions = resolvedVersions;
        return this;
    }

    @Override
    public CatalogGenerator build() {
        XsltCatalogGenerator generator = new XsltCatalogGenerator(catalogVersion, outputDirectory, verbose);
        generator.setResolvedVersions(resolvedVersions);
        return generator;
    }
}
