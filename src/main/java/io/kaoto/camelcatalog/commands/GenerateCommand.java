package io.kaoto.camelcatalog.commands;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import io.kaoto.camelcatalog.beans.ConfigBean;
import io.kaoto.camelcatalog.generator.camel.CamelCatalogGeneratorBuilder;
import io.kaoto.camelcatalog.generator.Util;
import io.kaoto.camelcatalog.generator.citrus.CitrusCatalogGeneratorBuilder;
import io.kaoto.camelcatalog.generator.templates.StarterTemplatesGeneratorBuilder;
import io.kaoto.camelcatalog.generator.xslt.XsltCatalogGeneratorBuilder;
import io.kaoto.camelcatalog.maven.PomFetcher;
import io.kaoto.camelcatalog.maven.RuntimeVersionResolver;
import io.kaoto.camelcatalog.model.CatalogDefinition;
import io.kaoto.camelcatalog.model.CatalogLibrary;
import io.kaoto.camelcatalog.model.CatalogRuntime;
import io.kaoto.camelcatalog.model.ResolvedVersions;
import org.apache.camel.tooling.maven.MavenDownloaderImpl;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.logging.Logger;

public class GenerateCommand implements Runnable {
    private static final Logger LOGGER = Logger.getLogger(GenerateCommand.class.getName());
    private final ConfigBean configBean;

    public GenerateCommand(ConfigBean configBean) {
        this.configBean = configBean;
    }

    @Override
    public void run() {
        LOGGER.info("Output folder: " + configBean.getOutputFolder() + "\n" +
                "Catalog versions: " + configBean.getCatalogVersionSet() + "\n" +
                "Kamelets version: " + configBean.getKameletsVersion());

        CatalogLibrary library = new CatalogLibrary(3, configBean.getCatalogsName());

        MavenDownloaderImpl downloader = new MavenDownloaderImpl();
        downloader.build();
        RuntimeVersionResolver versionResolver = new RuntimeVersionResolver(
                PomFetcher.http(), downloader, configBean.getRepositories());

        FileUtils.deleteQuietly(configBean.getOutputFolder());
        File outputFolder = createSubFolder(configBean.getOutputFolder());

        configBean.getCatalogVersionSet()
                .forEach(catalogCliArg -> {
                    if (catalogCliArg.getRuntime() == CatalogRuntime.StarterTemplates) {
                        throw new IllegalArgumentException(
                                "StarterTemplates is not a versioned CLI runtime");
                    }

                    ResolvedVersions resolved = versionResolver.resolve(
                            catalogCliArg.getRuntime(), catalogCliArg.getCatalogVersion());

                    String downloadVersion = resolved.catalogDownloadVersion();
                    String runtimeFolderName = catalogCliArg.getRuntime().getRuntimeFolder();
                    File runtimeFolder = createSubFolder(outputFolder, runtimeFolderName);
                    File catalogDefinitionFolder = createSubFolder(runtimeFolder, downloadVersion);

                    LOGGER.info("-------------------------------------------\n");
                    LOGGER.info("Generating catalog: " + catalogCliArg.getRuntime() + " input="
                            + catalogCliArg.getCatalogVersion() + " -> camel=" + resolved.camelCatalogVersion()
                            + " provider=" + resolved.runtimeProviderVersion()
                            + " framework=" + resolved.frameworkVersion());

                    var catalogGenerator = switch (catalogCliArg.getRuntime()) {
                        case Main, Quarkus, SpringBoot -> new CamelCatalogGeneratorBuilder()
                                .withRuntime(catalogCliArg.getRuntime())
                                .withCatalogVersion(downloadVersion)
                                .withKameletsVersion(configBean.getKameletsVersion())
                                .withCamelKCRDsVersion("2.3.1")
                                .withOutputDirectory(catalogDefinitionFolder)
                                .withVerbose(configBean.isVerbose())
                                .withResolvedVersions(resolved)
                                .build();
                        case Citrus -> new CitrusCatalogGeneratorBuilder()
                                .withCatalogVersion(downloadVersion)
                                .withOutputDirectory(catalogDefinitionFolder)
                                .withVerbose(configBean.isVerbose())
                                .withResolvedVersions(resolved)
                                .build();
                        case XSLT -> new XsltCatalogGeneratorBuilder()
                                .withCatalogVersion(downloadVersion)
                                .withOutputDirectory(catalogDefinitionFolder)
                                .withVerbose(configBean.isVerbose())
                                .withResolvedVersions(resolved)
                                .build();
                        case StarterTemplates -> throw new IllegalStateException(
                                "StarterTemplates is not a versioned CLI runtime");
                    };

                    CatalogDefinition catalogDefinition = catalogGenerator.generate();
                    if (catalogDefinition == null) {
                        throw new IllegalStateException("Catalog generation returned no result for "
                                + catalogCliArg.getRuntime() + " " + downloadVersion);
                    }

                    File indexFile = catalogDefinitionFolder.toPath().resolve(catalogDefinition.getFileName()).toFile();
                    String relateIndexFile = outputFolder.toPath().relativize(indexFile.toPath()).toString().replace(File.separator, "/");

                    catalogDefinition.setFileName(relateIndexFile);

                    library.addDefinition(catalogDefinition);
                });

        // Generate starter templates once — runtime-agnostic, invoked after the runtime loop
        File starterTemplatesFolder = createSubFolder(outputFolder,
                CatalogRuntime.StarterTemplates.getRuntimeFolder());
        CatalogDefinition starterDef = new StarterTemplatesGeneratorBuilder()
                .withOutputDirectory(starterTemplatesFolder)
                .build()
                .generate();
        String relativeStarterIndex = outputFolder.toPath()
                .relativize(starterTemplatesFolder.toPath().resolve(starterDef.getFileName()))
                .toString().replace(File.separator, "/");
        library.setStarterTemplates(relativeStarterIndex);

        ObjectMapper jsonMapper = new ObjectMapper()
                .configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true);

        var indexFile = outputFolder.toPath().resolve("index.json").toFile();
        try {
            Util.createTabWriter(jsonMapper).writeValue(indexFile, library);
        } catch (IOException e) {
            throw new java.io.UncheckedIOException("Error writing index file", e);
        }
    }

    private File createSubFolder(File parentFolder, String folderName) {
        File newSubFolder = parentFolder.toPath().resolve(folderName).toFile();
        return createSubFolder(newSubFolder);
    }

    private File createSubFolder(File parentFolder) {
        File newSubFolder = parentFolder.toPath().toFile();
        if (!newSubFolder.exists()) {
            newSubFolder.mkdirs();
        }
        return newSubFolder;
    }
}
