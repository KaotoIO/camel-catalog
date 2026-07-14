package io.kaoto.camelcatalog.generator.citrus;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.kaoto.camelcatalog.generator.CatalogGenerator;
import io.kaoto.camelcatalog.maven.KaotoMavenVersionManager;
import io.kaoto.camelcatalog.maven.ResourceLoader;
import io.kaoto.camelcatalog.model.CatalogDefinition;
import io.kaoto.camelcatalog.model.CatalogRuntime;
import io.kaoto.camelcatalog.model.ResolvedVersions;
import jakarta.annotation.Nonnull;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

/**
 * Citrus catalog generator loads citrus-catalog-schema.jar artifact with respective version from Maven central and reads all
 * catalog files from resource classpath.
 */
public class CitrusCatalogGenerator implements CatalogGenerator {

    private final String catalogVersion;
    private final File outputDirectory;
    private final KaotoMavenVersionManager kaotoMavenVersionManager;
    private final ResourceLoader resourceLoader;
    private final ObjectMapper objectMapper;
    private ResolvedVersions resolvedVersions;

    public CitrusCatalogGenerator(String catalogVersion, File outputDirectory, boolean verbose) {
        this.catalogVersion = catalogVersion;
        this.outputDirectory = outputDirectory;

        this.kaotoMavenVersionManager = new KaotoMavenVersionManager();
        this.kaotoMavenVersionManager.setLog(verbose);
        this.kaotoMavenVersionManager.addMavenRepository("central", "https://repo1.maven.org/maven2/");

        this.resourceLoader = new ResourceLoader(kaotoMavenVersionManager, verbose);
        this.objectMapper = new ObjectMapper();
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

        return getCatalogDefinition();
    }

    @Nonnull
    private CatalogDefinition getCatalogDefinition() {
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
        resourceLoader.loadResourcesFromFolderAsString("org/citrusframework/schema/citrus/%s".formatted(catalogVersion),
                catalogFiles, fileSuffix);
        catalogFiles.forEach((k, v) -> {
            try {
                String content = v;
                // Process JSON files to remove "actions" property from test action schemas
                if (fileSuffix.equals(".json") && (k.contains("test-actions") || k.contains("test-containers"))) {
                    content = removeActionsProperty(v, k);
                }
                Files.writeString(Paths.get(outputDirectory.getAbsolutePath(), k + fileSuffix), content);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }

    /**
     * Removes the "actions" property from Citrus test action/container aggregate schemas.
     * The "actions" property is equivalent to "steps" in Camel and should be rendered on
     * the Kaoto canvas, not in the config form.
     *
     * @param jsonContent the JSON content to process
     * @param fileName    the name of the file being processed
     * @return the processed JSON content with "actions" properties removed
     */
    private String removeActionsProperty(String jsonContent, String fileName) {
        try {
            JsonNode rootNode = objectMapper.readTree(jsonContent);

            if (!rootNode.isObject()) {
                return jsonContent;
            }

            ObjectNode rootObject = (ObjectNode) rootNode;

            // Process citrus-catalog-aggregate-test-actions.json and citrus-catalog-aggregate-test-containers.json.
            // Each top-level key is a test action/container entry; remove "actions" from its propertiesSchema.
            rootObject.properties().forEach(entry -> {
                JsonNode actionNode = entry.getValue();
                if (actionNode.isObject() && actionNode.has("propertiesSchema")) {
                    ObjectNode propertiesSchema = (ObjectNode) actionNode.get("propertiesSchema");

                    // Remove "actions" property from the schema properties
                    if (propertiesSchema.has("properties")) {
                        ObjectNode properties = (ObjectNode) propertiesSchema.get("properties");
                        properties.remove("actions");
                    }

                    // Remove "actions" from the required array
                    if (propertiesSchema.has("required") && propertiesSchema.get("required").isArray()) {
                        var requiredArray = propertiesSchema.withArray("required");
                        for (int i = requiredArray.size() - 1; i >= 0; i--) {
                            if (requiredArray.get(i).isTextual() && "actions".equals(requiredArray.get(i).asText())) {
                                requiredArray.remove(i);
                            }
                        }
                    }
                }
            });

            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(rootNode);
        } catch (Exception e) {
            // If processing fails, return original content
            return jsonContent;
        }
    }

}
