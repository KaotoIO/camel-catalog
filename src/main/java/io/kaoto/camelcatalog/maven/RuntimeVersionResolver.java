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

import io.kaoto.camelcatalog.generator.CamelLauncherVersionResolver;
import io.kaoto.camelcatalog.model.CatalogRuntime;
import io.kaoto.camelcatalog.model.ResolvedVersions;
import org.apache.camel.tooling.maven.MavenArtifact;
import org.apache.camel.tooling.maven.MavenDownloader;
import org.apache.camel.tooling.maven.MavenGav;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Resolves the full version triple (Apache Camel, runtime provider, framework) starting from the
 * versions/artifacts that {@code camel run} consumes.
 *
 * <p>Mirrors the structure of Apache Camel's {@code dsl/camel-jbang}
 * {@code org.apache.camel.dsl.jbang.core.common.CatalogLoader} so the logic can be contributed
 * upstream. The only difference is the Maven download mechanism: this class uses
 * {@code org.apache.camel.tooling.maven.MavenDownloader} (already on the generator classpath)
 * instead of {@code org.apache.camel.main.download.MavenDependencyDownloader}.
 *
 * <p>Provenance (platform groupId + Maven repositories) is inferred per version from the
 * {@code .redhat-} suffix, matching {@code ResourceLoader.configureRepositories} and
 * {@code CamelLauncherVersionResolver}.
 */
public class RuntimeVersionResolver {
    private static final Logger LOGGER = Logger.getLogger(RuntimeVersionResolver.class.getName());

    static final String COMMUNITY_QUARKUS_GROUP_ID = "io.quarkus.platform";
    static final String REDHAT_QUARKUS_GROUP_ID = "com.redhat.quarkus.platform";
    static final String MAVEN_CENTRAL = "https://repo1.maven.org/maven2";
    static final String REDHAT_GA = "https://maven.repository.redhat.com/ga";

    private final PomFetcher pomFetcher;
    private final MavenDownloader downloader;
    private final List<String> customRepositories;
    private final CamelLauncherVersionResolver camelFromQuarkusResolver = new CamelLauncherVersionResolver();

    public RuntimeVersionResolver(PomFetcher pomFetcher, MavenDownloader downloader,
            List<String> customRepositories) {
        this.pomFetcher = pomFetcher;
        this.downloader = downloader;
        this.customRepositories = customRepositories == null ? List.of() : customRepositories;
    }

    /**
     * Resolves the full version triple for one catalog entry, starting from the value
     * {@code camel run} consumes for the given runtime.
     *
     * @param runtime      the target runtime
     * @param inputVersion for Quarkus the platform version; for Spring Boot the Camel Spring Boot
     *                     version; for Main the Apache Camel version
     */
    public ResolvedVersions resolve(CatalogRuntime runtime, String inputVersion) {
        return switch (runtime) {
            case Quarkus -> resolveQuarkus(inputVersion);
            case SpringBoot -> resolveSpringBoot(inputVersion);
            default -> new ResolvedVersions(inputVersion, null, null, inputVersion);
        };
    }

    private ResolvedVersions resolveQuarkus(String platformVersion) {
        String groupId = quarkusPlatformGroupId(platformVersion);
        List<String> repos = repositoriesFor(platformVersion, customRepositories);

        String bomPom = pomFetcher.fetchPom(groupId, "quarkus-camel-bom", platformVersion, repos);
        String camelQuarkusVersion = extractCamelQuarkusVersionFromPlatformBom(bomPom);
        if (camelQuarkusVersion == null) {
            LOGGER.warning("Could not resolve Camel Quarkus version from platform "
                    + groupId + ":" + platformVersion + "; falling back to platform version");
            camelQuarkusVersion = platformVersion;
        }

        // Note: this second-hop BOM fetch uses CamelLauncherVersionResolver's own repository
        // selection (Maven Central + Red Hat GA inferred from the version) and does not receive
        // the additive customRepositories passed to this resolver. A corporate mirror supplied via
        // --repos is therefore not consulted for this hop. Acceptable while mirrors proxy both
        // central + GA; revisit if that assumption breaks.
        String camelVersion = camelFromQuarkusResolver
                .resolveCamelVersionFromQuarkusBom(camelQuarkusVersion);
        if (camelVersion == null) {
            LOGGER.warning("Could not resolve Apache Camel version from camel-quarkus-bom "
                    + camelQuarkusVersion + "; falling back to camelQuarkus version");
            camelVersion = camelQuarkusVersion;
        }

        return new ResolvedVersions(camelVersion, camelQuarkusVersion, platformVersion,
                camelQuarkusVersion);
    }

    private ResolvedVersions resolveSpringBoot(String camelSpringBootVersion) {
        String springBootVersion = resolveSpringBootVersionFromCamelSpringBoot(camelSpringBootVersion);
        // Camel Spring Boot tracks the Apache Camel version 1:1.
        return new ResolvedVersions(camelSpringBootVersion, camelSpringBootVersion, springBootVersion,
                camelSpringBootVersion);
    }

    /**
     * Mirrors {@code CatalogLoader.resolveSpringBootVersionFromCamelSpringBoot}: resolves
     * camel-core-starter transitively and reads the resolved spring-boot-starter version.
     */
    public String resolveSpringBootVersionFromCamelSpringBoot(String camelSpringBootVersion) {
        try {
            String gav = "org.apache.camel.springboot:camel-core-starter:" + camelSpringBootVersion;
            boolean snapshots = camelSpringBootVersion.endsWith("SNAPSHOT");
            List<String> repos = repositoriesFor(camelSpringBootVersion, customRepositories);
            List<MavenArtifact> artifacts = downloader.resolveArtifacts(
                    Collections.singletonList(gav), new LinkedHashSet<>(repos), true, snapshots);
            List<MavenGav> gavs = artifacts.stream().map(MavenArtifact::getGav).toList();
            return findVersion(gavs, "org.springframework.boot", "spring-boot-starter");
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to resolve Spring Boot version from " + camelSpringBootVersion, e);
            return null;
        }
    }

    /** A productized (Red Hat) version is identified by the {@code redhat-} marker in the version string. */
    public static boolean isRedhat(String version) {
        return version != null && version.contains("redhat-");
    }

    /** Infers the Quarkus platform groupId from the platform version. */
    public static String quarkusPlatformGroupId(String platformVersion) {
        return isRedhat(platformVersion) ? REDHAT_QUARKUS_GROUP_ID : COMMUNITY_QUARKUS_GROUP_ID;
    }

    /**
     * Builds the ordered repository list for a version: Maven Central always; Red Hat GA when the
     * version is productized; then any additive custom repositories (corporate mirrors).
     */
    public static List<String> repositoriesFor(String version, List<String> customRepositories) {
        List<String> repos = new ArrayList<>();
        repos.add(MAVEN_CENTRAL);
        if (isRedhat(version)) {
            repos.add(REDHAT_GA);
        }
        if (customRepositories != null) {
            repos.addAll(customRepositories);
        }
        return repos;
    }

    /**
     * Parses a Quarkus platform BOM ({@code <quarkusGroupId>:quarkus-camel-bom}) and returns the
     * managed version of {@code org.apache.camel.quarkus:camel-quarkus-catalog} — i.e. the Camel
     * Quarkus (runtime provider) version.
     *
     * @return the camel-quarkus version, or {@code null} if not present.
     */
    public static String extractCamelQuarkusVersionFromPlatformBom(String pomXml) {
        return extractManagedVersion(pomXml, "org.apache.camel.quarkus", "camel-quarkus-catalog");
    }

    /**
     * Parses a POM and returns the version of the first {@code dependency} entry matching the given
     * groupId/artifactId.
     */
    static String extractManagedVersion(String pomXml, String groupId, String artifactId) {
        if (pomXml == null) {
            return null;
        }
        try {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            dbf.setNamespaceAware(false);
            Document doc = dbf.newDocumentBuilder()
                    .parse(new ByteArrayInputStream(pomXml.getBytes(StandardCharsets.UTF_8)));
            NodeList deps = doc.getElementsByTagName("dependency");
            for (int i = 0; i < deps.getLength(); i++) {
                Element dep = (Element) deps.item(i);
                String g = text(dep, "groupId");
                String a = text(dep, "artifactId");
                String v = text(dep, "version");
                if (groupId.equals(g) && artifactId.equals(a) && v != null && !v.isBlank()) {
                    return v.trim();
                }
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to parse POM for " + groupId + ":" + artifactId, e);
        }
        return null;
    }

    /**
     * Returns the version of the first GAV matching the given groupId/artifactId, or {@code null}.
     */
    public static String findVersion(List<MavenGav> gavs, String groupId, String artifactId) {
        if (gavs == null) {
            return null;
        }
        for (MavenGav gav : gavs) {
            if (groupId.equals(gav.getGroupId()) && artifactId.equals(gav.getArtifactId())) {
                return gav.getVersion();
            }
        }
        return null;
    }

    private static String text(Element parent, String tag) {
        NodeList nl = parent.getElementsByTagName(tag);
        if (nl.getLength() == 0) {
            return null;
        }
        return nl.item(0).getTextContent();
    }
}
