package io.kaoto.camelcatalog.generator.citrus;

import java.io.File;

import io.kaoto.camelcatalog.generator.CatalogGenerator;
import io.kaoto.camelcatalog.generator.CatalogGeneratorBuilder;
import io.kaoto.camelcatalog.model.ResolvedVersions;

public class CitrusCatalogGeneratorBuilder implements CatalogGeneratorBuilder {

    private String catalogVersion;
    private File outputDirectory;
    private boolean verbose = false;
    private ResolvedVersions resolvedVersions;

    public CitrusCatalogGeneratorBuilder withCatalogVersion(String catalogVersion) {
        this.catalogVersion = catalogVersion;
        return this;
    }

    public CitrusCatalogGeneratorBuilder withOutputDirectory(File outputDirectory) {
        this.outputDirectory = outputDirectory;
        return this;
    }

    public CitrusCatalogGeneratorBuilder withVerbose(boolean verbose) {
        this.verbose = verbose;
        return this;
    }

    public CitrusCatalogGeneratorBuilder withResolvedVersions(ResolvedVersions resolvedVersions) {
        this.resolvedVersions = resolvedVersions;
        return this;
    }

    @Override
    public CatalogGenerator build() {
        CitrusCatalogGenerator generator = new CitrusCatalogGenerator(catalogVersion, outputDirectory, verbose);
        generator.setResolvedVersions(resolvedVersions);
        return generator;
    }
}
