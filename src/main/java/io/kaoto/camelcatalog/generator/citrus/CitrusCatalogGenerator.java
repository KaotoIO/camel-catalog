package io.kaoto.camelcatalog.generator.citrus;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

import io.kaoto.camelcatalog.generator.CatalogGenerator;
import io.kaoto.camelcatalog.maven.KaotoMavenVersionManager;
import io.kaoto.camelcatalog.maven.ResourceLoader;
import io.kaoto.camelcatalog.model.CatalogDefinition;
import io.kaoto.camelcatalog.model.CatalogRuntime;
import io.kaoto.camelcatalog.model.ResolvedVersions;

/**
 * Citrus catalog generator loads citrus-catalog-schema.jar artifact with respective version from Maven central and reads all
 * catalog files from resource classpath.
 */
public class CitrusCatalogGenerator implements CatalogGenerator {

    private final String catalogVersion;
    private final File outputDirectory;
    private final KaotoMavenVersionManager kaotoMavenVersionManager;
    private final ResourceLoader resourceLoader;
    private ResolvedVersions resolvedVersions;

    public CitrusCatalogGenerator(String catalogVersion, File outputDirectory, boolean verbose) {
        this.catalogVersion = catalogVersion;
        this.outputDirectory = outputDirectory;

        this.kaotoMavenVersionManager = new KaotoMavenVersionManager();
        this.kaotoMavenVersionManager.setLog(verbose);
        this.kaotoMavenVersionManager.addMavenRepository("central", "https://repo1.maven.org/maven2/");

        this.resourceLoader = new ResourceLoader(kaotoMavenVersionManager, verbose);
    }

    public void setResolvedVersions(ResolvedVersions resolvedVersions) {
        this.resolvedVersions = resolvedVersions;
    }

    @Override
    public CatalogDefinition generate() {
        String gav = String.format("org.citrusframework:citrus-catalog-schema:%s", catalogVersion);
        boolean shouldFetchTransitive = false;
        boolean shouldUseSnapshots = catalogVersion.endsWith("SNAPSHOT");

        kaotoMavenVersionManager.resolve(gav, shouldUseSnapshots, shouldFetchTransitive);
        loadAndWriteCatalogFiles(".json");
        loadAndWriteCatalogFiles(".xsd");

        var catalogDefinition = new CatalogDefinition();
        catalogDefinition.setName("Citrus " + catalogVersion);
        catalogDefinition.setRuntime(CatalogRuntime.Citrus);
        catalogDefinition.setVersion(catalogVersion);
        catalogDefinition.setFileName("index.json");
        if (resolvedVersions != null) {
            catalogDefinition.setCamelCatalogVersion(resolvedVersions.camelCatalogVersion());
            catalogDefinition.setRuntimeProviderVersion(resolvedVersions.runtimeProviderVersion());
            catalogDefinition.setFrameworkVersion(resolvedVersions.frameworkVersion());
        }
        return catalogDefinition;
    }

    private void loadAndWriteCatalogFiles(String fileSuffix) {
        final Map<String, String> catalogFiles = new HashMap<>();
        resourceLoader.loadResourcesFromFolderAsString("org/citrusframework/schema/citrus/%s".formatted(catalogVersion), catalogFiles, fileSuffix);
        catalogFiles.forEach((k, v) -> {
            try {
                Files.writeString(Paths.get(outputDirectory.getAbsolutePath(), k + fileSuffix), v);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }
}
