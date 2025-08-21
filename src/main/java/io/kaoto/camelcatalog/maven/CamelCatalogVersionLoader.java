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
package io.kaoto.camelcatalog.maven;

import io.kaoto.camelcatalog.model.CatalogRuntime;
import io.kaoto.camelcatalog.model.Constants;
import io.kaoto.camelcatalog.model.MavenCoordinates;
import org.apache.camel.catalog.CamelCatalog;
import org.apache.camel.catalog.DefaultCamelCatalog;
import org.apache.camel.catalog.DefaultRuntimeProvider;
import org.apache.camel.catalog.quarkus.QuarkusRuntimeProvider;
import org.apache.camel.springboot.catalog.SpringBootRuntimeProvider;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class CamelCatalogVersionLoader {
    private static final Logger LOGGER = Logger.getLogger(CamelCatalogVersionLoader.class.getName());
    private final ResourceLoader resourceLoader;
    private final CamelCatalog camelCatalog = new DefaultCamelCatalog(true);
    private final Map<String, String> kameletBoundaries = new HashMap<>();
    private final Map<String, String> kamelets = new HashMap<>();
    private final List<String> camelKCRDs = new ArrayList<>();
    private final Map<String, String> localSchemas = new HashMap<>();
    private final Map<String, String> kaotoPatterns = new HashMap<>();
    private final CatalogRuntime runtime;
    private final KaotoMavenVersionManager kaotoVersionManager;
    private String camelYamlDSLSchema;
    private String kubernetesSchema;

    public CamelCatalogVersionLoader(CatalogRuntime runtime, boolean verbose) {
        kaotoVersionManager = new KaotoMavenVersionManager();
        kaotoVersionManager.setLog(verbose);

        this.resourceLoader = new ResourceLoader(kaotoVersionManager, verbose);
        this.runtime = runtime;
        camelCatalog.setVersionManager(kaotoVersionManager);
    }

    public CatalogRuntime getRuntime() {
        return runtime;
    }

    public CamelCatalog getCamelCatalog() {
        return camelCatalog;
    }

    public String getCamelYamlDslSchema() {
        return camelYamlDSLSchema;
    }

    public List<String> getKameletBoundaries() {
        return kameletBoundaries.values().stream().toList();
    }

    public List<String> getKamelets() {
        return kamelets.values().stream().toList();
    }

    public String getKubernetesSchema() {
        return kubernetesSchema;
    }

    public List<String> getCamelKCRDs() {
        return camelKCRDs;
    }

    public Map<String, String> getLocalSchemas() {
        return localSchemas;
    }

    public Map<String, String> getKaotoPatterns() {
        return kaotoPatterns;
    }

    public ResourceLoader getResourceLoader() {
        return resourceLoader;
    }

    public ClassLoader getClassLoader() {
        return kaotoVersionManager.getClassLoader();
    }

    public boolean loadCamelCatalog(String version) {
        if (version != null) {
            resourceLoader.configureRepositories(version);
            MavenCoordinates mavenCoordinates = getCatalogMavenCoordinates(runtime, version);
            loadDependencyInClasspath(mavenCoordinates);
        }

        /*
         * Check the current runtime, so we can apply the corresponding RuntimeProvider
         * to the catalog
         */
        switch (runtime) {
            case Quarkus:
                camelCatalog.setRuntimeProvider(new QuarkusRuntimeProvider());
                break;
            case SpringBoot:
                camelCatalog.setRuntimeProvider(new SpringBootRuntimeProvider());
                break;
            default:
                camelCatalog.setRuntimeProvider(new DefaultRuntimeProvider());
                break;
        }

        return camelCatalog.getCatalogVersion() != null;
    }

    public boolean loadCamelYamlDsl(String version) {
        MavenCoordinates mavenCoordinates = getYamlDslMavenCoordinates(runtime, version);
        loadDependencyInClasspath(mavenCoordinates);

        ClassLoader classLoader = resourceLoader.getKaotoVersionManager().getClassLoader();
        URL resourceURL = classLoader.getResource(Constants.CAMEL_YAML_DSL_ARTIFACT);
        if (resourceURL == null) {
            LOGGER.log(Level.SEVERE, "No " + Constants.CAMEL_YAML_DSL_ARTIFACT + " file found in the classpath");
            return false;
        }

        try (InputStream inputStream = resourceURL.openStream()) {
            try (Scanner scanner = new Scanner(inputStream)) {
                scanner.useDelimiter("\\A");
                camelYamlDSLSchema = scanner.hasNext() ? scanner.next() : "";
            }
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, e.toString(), e);
            return false;
        }

        return camelYamlDSLSchema != null;
    }

    public boolean loadKameletBoundaries() {
        resourceLoader.loadResourcesFromFolderAsString("kamelet-boundaries", kameletBoundaries, ".kamelet.yaml");
        return !kameletBoundaries.isEmpty();
    }

    public boolean loadKamelets(String version) {
        if (version != null) {
            // If the version is null, we load the installed version
            MavenCoordinates mavenCoordinates =
                    new MavenCoordinates(Constants.APACHE_CAMEL_KAMELETS_ORG, Constants.KAMELETS_PACKAGE, version);
            loadDependencyInClasspath(mavenCoordinates);
        }

        resourceLoader.loadResourcesFromFolderAsString("kamelets", kamelets, ".kamelet.yaml");

        return !kamelets.isEmpty();
    }

    public boolean loadKubernetesSchema() {
        String url =
                "https://raw.githubusercontent.com/kubernetes/kubernetes/master/api/openapi-spec/v3/api__v1_openapi.json";

        try (InputStream in = new URI(url).toURL().openStream(); Scanner scanner = new Scanner(in,
                StandardCharsets.UTF_8)) {
            scanner.useDelimiter("\\A");
            kubernetesSchema = scanner.hasNext() ? scanner.next() : "";
        } catch (IOException | URISyntaxException e) {
            LOGGER.log(Level.SEVERE, e.toString(), e);
            return false;
        }

        return true;
    }

    public boolean loadCamelKCRDs(String version) {
        MavenCoordinates mavenCoordinates =
                new MavenCoordinates(Constants.APACHE_CAMEL_K_ORG, Constants.CAMEL_K_CRDS_PACKAGE, version);
        boolean areCamelKCRDsLoaded = loadDependencyInClasspath(mavenCoordinates);

        ClassLoader classLoader = resourceLoader.getKaotoVersionManager().getClassLoader();

        for (String crd : Constants.CAMEL_K_CRDS_ARTIFACTS) {
            URL resourceURL = classLoader.getResource(crd);
            if (resourceURL == null) {
                return false;
            }

            try (InputStream inputStream = resourceURL.openStream()) {
                try (Scanner scanner = new Scanner(inputStream)) {
                    scanner.useDelimiter("\\A");
                    camelKCRDs.add(scanner.hasNext() ? scanner.next() : "");
                }
            } catch (IOException e) {
                LOGGER.log(Level.SEVERE, e.toString(), e);
                return false;
            }
        }

        return areCamelKCRDsLoaded;
    }

    public void loadLocalSchemas() {
        resourceLoader.loadResourcesFromFolderAsString("schemas", localSchemas, ".json");
    }

    public void loadKaotoPatterns() {
        resourceLoader.loadResourcesFromFolderAsString("kaoto-patterns", kaotoPatterns, ".json");
    }

    MavenCoordinates getCatalogMavenCoordinates(CatalogRuntime runtime, String version) {
        return switch (runtime) {
            case Quarkus ->
                    new MavenCoordinates(Constants.APACHE_CAMEL_ORG + ".quarkus", "camel-quarkus-catalog", version);
            case SpringBoot -> new MavenCoordinates(Constants.APACHE_CAMEL_ORG + ".springboot",
                    "camel-catalog-provider-springboot", version);
            default -> new MavenCoordinates(Constants.APACHE_CAMEL_ORG, "camel-catalog", version);
        };
    }

    MavenCoordinates getYamlDslMavenCoordinates(CatalogRuntime runtime, String version) {
        return switch (runtime) {
            case Quarkus ->
                    new MavenCoordinates(Constants.APACHE_CAMEL_ORG + ".quarkus", "camel-quarkus-yaml-dsl", version);
            case SpringBoot ->
                    new MavenCoordinates(Constants.APACHE_CAMEL_ORG + ".springboot", "camel-yaml-dsl-starter",
                            version);
            default -> new MavenCoordinates(Constants.APACHE_CAMEL_ORG, Constants.CAMEL_YAML_DSL_PACKAGE, version);
        };
    }

    /*
     * This method is used to load a dependency in the classpath. This is a
     * workaround
     * to load dependencies that are not in the classpath, while the Camel Catalog
     * exposes a method to load dependencies in the classpath.
     */
    private boolean loadDependencyInClasspath(MavenCoordinates mavenCoordinates) {
        return camelCatalog.loadRuntimeProviderVersion(mavenCoordinates.getGroupId(), mavenCoordinates.getArtifactId(),
                mavenCoordinates.getVersion());
    }
}
