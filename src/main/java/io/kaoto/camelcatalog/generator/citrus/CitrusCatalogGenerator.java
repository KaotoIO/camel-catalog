package io.kaoto.camelcatalog.generator.citrus;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
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

    private static final String ONE_OF = "oneOf";
    private static final String ANY_OF = "anyOf";
    private static final String REQUIRED = "required";
    private static final String PROPERTIES = "properties";
    private static final String ITEMS = "items";

    private final String catalogVersion;
    private final File outputDirectory;
    private final KaotoMavenVersionManager kaotoMavenVersionManager;
    private final ResourceLoader resourceLoader;
    private final ObjectMapper objectMapper;
    private ResolvedVersions resolvedVersions;

    /**
     * Creates a new Citrus catalog generator for the given version.
     * @param catalogVersion the Citrus version to generate catalogs for
     * @param outputDirectory the directory where generated catalog files are written
     * @param verbose whether to enable verbose Maven logging
     */
    public CitrusCatalogGenerator(String catalogVersion, File outputDirectory, boolean verbose) {
        this.catalogVersion = catalogVersion;
        this.outputDirectory = outputDirectory;

        this.kaotoMavenVersionManager = new KaotoMavenVersionManager();
        this.kaotoMavenVersionManager.setLog(verbose);
        this.kaotoMavenVersionManager.addMavenRepository("central", "https://repo1.maven.org/maven2/");

        this.resourceLoader = new ResourceLoader(kaotoMavenVersionManager, verbose);
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Sets the resolved dependency versions to include in the catalog definition metadata.
     * @param resolvedVersions the resolved Camel, runtime provider and framework versions
     */
    public void setResolvedVersions(ResolvedVersions resolvedVersions) {
        this.resolvedVersions = resolvedVersions;
    }

    /**
     * Generates the Citrus catalog by resolving the catalog schema artifact from Maven
     * and writing all JSON and XSD catalog files to the output directory.
     * @return the catalog definition describing the generated catalog
     */
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

    /**
     * Builds the catalog definition metadata for the generated Citrus catalog.
     * @return a non-null catalog definition with name, runtime, version and resolved dependency versions
     */
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

    /**
     * Loads catalog resource files with the given suffix from the resolved artifact and writes them
     * to the output directory. JSON test action and container schemas are post-processed before writing.
     * @param fileSuffix the file extension to load (e.g. ".json" or ".xsd")
     */
    private void loadAndWriteCatalogFiles(String fileSuffix) {
        final Map<String, String> catalogFiles = new HashMap<>();
        resourceLoader.loadResourcesFromFolderAsString("org/citrusframework/schema/citrus/%s".formatted(catalogVersion),
                catalogFiles, fileSuffix);
        catalogFiles.forEach((file, schema) -> {
            try {
                String content = schema;
                // Process JSON files to remove "actions" property from test action schemas
                if (fileSuffix.equals(".json") && (file.contains("test-actions") || file.contains("test-containers"))) {
                    content = postProcessActionSchema(schema);
                }
                Files.writeString(Paths.get(outputDirectory.getAbsolutePath(), file + fileSuffix), content);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }

    /**
     * Post process Citrus test action/container aggregate schemas.
     * Performs several steps to prepare the schemas for proper form generation and validation in Kaoto.
     * Removes the "actions" property from Citrus test action/container aggregate schemas.
     * The "actions" property is equivalent to "steps" in Camel and should be rendered on
     * the Kaoto canvas, not in the config form.
     * Removes too complex required declarations from oneOf arrays.
     * @param jsonContent the JSON content to process
     * @return the processed JSON content prepared for Kaoto forms generation.
     */
    private String postProcessActionSchema(String jsonContent) {
        try {
            JsonNode rootNode = objectMapper.readTree(jsonContent);

            if (!rootNode.isObject()) {
                return jsonContent;
            }

            ObjectNode rootObject = (ObjectNode) rootNode;

            // Process property schemas defined in citrus-catalog-aggregate-test-actions and citrus-catalog-aggregate-test-containers
            rootObject.properties().forEach(entry -> {
                JsonNode actionNode = entry.getValue();
                if (actionNode.isObject() && actionNode.path("propertiesSchema").isObject()) {
                    ObjectNode propertiesSchema = (ObjectNode) actionNode.get("propertiesSchema");

                    if (propertiesSchema.path(PROPERTIES).isObject()) {
                        ObjectNode properties = (ObjectNode) propertiesSchema.get(PROPERTIES);

                        // Each top-level key is a test action/container entry; remove "actions" from its propertiesSchema.
                        properties.remove("actions");
                    }

                    // Remove "actions" from the top-level required definitions
                    if (propertiesSchema.has(REQUIRED) && propertiesSchema.get(REQUIRED).isArray()) {
                        var requiredArray = propertiesSchema.withArray(REQUIRED);
                        for (int i = requiredArray.size() - 1; i >= 0; i--) {
                            if (requiredArray.get(i).isTextual() && "actions".equals(requiredArray.get(i).asText())) {
                                requiredArray.remove(i);
                            }
                        }
                    }

                    postProcessSchemaNode(propertiesSchema);
                }
            });

            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(rootNode);
        } catch (Exception e) {
            // If processing fails, return original content
            return jsonContent;
        }
    }

    /**
     * Post process given schema object node.
     * Searches for anyOf/oneOf arrays and applies special processing logic such as removing required definitions.
     * Removes required-only oneOf branches to simplify form generation and recursively
     * processes nested schema structures in oneOf, anyOf and properties.
     * @param schema the schema node to process
     */
    private void postProcessSchemaNode(ObjectNode schema) {
        if (schema.has(ONE_OF)) {
            ArrayNode oneOf = schema.withArray(ONE_OF);
            cleanupOneOfArray(oneOf);
            if (oneOf.isEmpty()) {
                schema.remove(ONE_OF);
            }
        }

        if (schema.has(ANY_OF)) {
            ArrayNode anyOf = schema.withArray(ANY_OF);
            for (JsonNode anyOfItem : anyOf) {
                if (anyOfItem.isObject()) {
                    postProcessSchemaNode((ObjectNode) anyOfItem);
                }
            }
        }

        postProcessSchemaProperties(schema);
    }

    /**
     * Traverse through all properties in given schema node and apply the schema post-processing logic for each finding.
     * @param schema the schema node to process
     */
    private void postProcessSchemaProperties(JsonNode schema) {
        if (schema.has(PROPERTIES) && schema.get(PROPERTIES).isObject()) {
            ObjectNode properties = (ObjectNode) schema.get(PROPERTIES);
            properties.fieldNames().forEachRemaining(fieldName -> {
                if (properties.get(fieldName).isObject()) {
                    ObjectNode property = (ObjectNode) properties.get(fieldName);
                    postProcessSchemaNode(property);
                }
            });
        }

        if (schema.has(ITEMS) && schema.get(ITEMS).isObject()) {
            ObjectNode items = (ObjectNode) schema.get(ITEMS);
            postProcessSchemaNode(items);
        }

    }

    /**
     * Removes required-only branches from a oneOf array and recursively processes
     * nested properties within remaining branches.
     * @param oneOf the oneOf array node to clean up
     */
    private void cleanupOneOfArray(ArrayNode oneOf) {
        for (int i = oneOf.size() - 1; i >= 0; i--) {
            JsonNode oneOfItem = oneOf.get(i);
            if (oneOfItem.has(REQUIRED) && oneOfItem.get(REQUIRED).isArray()) {
                if (oneOfItem.size() == 1) {
                    oneOf.remove(i);
                    continue;
                } else {
                    ((ObjectNode) oneOfItem).remove(REQUIRED);
                }
            }

            postProcessSchemaProperties(oneOfItem);
        }
    }

}
