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
package io.kaoto.camelcatalog.generator;

import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import io.kaoto.camelcatalog.model.CatalogRuntime;
import org.apache.commons.io.IOUtils;

import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Resolves the appropriate camel-launcher version for a given Camel catalog version.
 * This class queries Maven repositories to find the best matching launcher version
 * based on major.minor.patch version matching.
 */
public class CamelLauncherVersionResolver {
    private static final Logger LOGGER = Logger.getLogger(CamelLauncherVersionResolver.class.getName());
    private static final String MAVEN_CENTRAL = "https://repo1.maven.org/maven2";
    private static final String REDHAT_GA = "https://maven.repository.redhat.com/ga";
    private static final String CAMEL_LAUNCHER_GROUP_PATH = "org/apache/camel";
    private static final String CAMEL_LAUNCHER_ARTIFACT = "camel-launcher";
    private static final String CAMEL_QUARKUS_BOM_GROUP_PATH = "org/apache/camel/quarkus";
    private static final String CAMEL_QUARKUS_BOM_ARTIFACT = "camel-quarkus-bom";
    
    private final XmlMapper xmlMapper;
    
    public CamelLauncherVersionResolver() {
        this.xmlMapper = new XmlMapper();
        // Configure to handle standard Maven metadata XML format
        this.xmlMapper.configure(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }
    
    /**
     * Finds the closest matching camel-launcher version for the given Camel version.
     * For Quarkus versions, it first resolves the internal Camel version from the BOM.
     * 
     * @param camelVersion The Camel catalog version (e.g., "4.14.2.redhat-00001" or "4.18.0")
     * @param runtime The catalog runtime (Main, Quarkus, SpringBoot)
     * @return The matching camel-launcher version, or null if no match is found
     */
    public String findLauncherVersion(String camelVersion, CatalogRuntime runtime) {
        if (camelVersion == null || camelVersion.isEmpty()) {
            LOGGER.warning("Camel version is null or empty");
            return null;
        }
        
        try {
            String resolvedCamelVersion = camelVersion;
            boolean isOriginalRedhat = camelVersion.contains(".redhat-");
            
            // For Quarkus, resolve the internal Camel version from the BOM
            if (runtime == CatalogRuntime.Quarkus) {
                LOGGER.info("Resolving internal Camel version from Quarkus BOM " + camelVersion);
                resolvedCamelVersion = resolveCamelVersionFromQuarkusBom(camelVersion);
                if (resolvedCamelVersion == null) {
                    LOGGER.warning("Failed to resolve Camel version from Quarkus BOM: " + camelVersion);
                    return null;
                }
                LOGGER.info("Resolved internal Camel version: " + resolvedCamelVersion);
            }
            
            VersionInfo inputVersion = parseVersion(resolvedCamelVersion);
            // For Quarkus Red Hat versions, preserve the Red Hat repository even if extracted Camel version doesn't have .redhat suffix
            boolean useRedhatRepo = inputVersion.isRedhat || (runtime == CatalogRuntime.Quarkus && isOriginalRedhat);
            String repository = useRedhatRepo ? REDHAT_GA : MAVEN_CENTRAL;
            
            LOGGER.info("Resolving camel-launcher version for Camel " + resolvedCamelVersion + 
                       " from repository: " + repository);
            
            List<String> availableVersions = fetchAvailableVersions(repository);
            String matchedVersion = findBestMatch(inputVersion, availableVersions);
            
            if (matchedVersion != null) {
                LOGGER.info("Matched camel-launcher version: " + matchedVersion);
            } else {
                LOGGER.warning("No matching camel-launcher version found for: " + resolvedCamelVersion);
            }
            
            return matchedVersion;
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to resolve launcher version for: " + camelVersion, e);
            return null;
        }
    }
    
    /**
     * Resolves the internal Apache Camel version from a Camel Quarkus BOM.
     * 
     * @param quarkusVersion The Camel Quarkus version (e.g., "3.15.0" or "3.15.0.redhat-00010")
     * @return The internal Camel version, or null if resolution fails
     */
    public String resolveCamelVersionFromQuarkusBom(String quarkusVersion) {
        try {
            VersionInfo versionInfo = parseVersion(quarkusVersion);
            String repository = versionInfo.isRedhat ? REDHAT_GA : MAVEN_CENTRAL;
            
            String pomUrl = String.format("%s/%s/%s/%s/%s-%s.pom", 
                                         repository, 
                                         CAMEL_QUARKUS_BOM_GROUP_PATH, 
                                         CAMEL_QUARKUS_BOM_ARTIFACT,
                                         quarkusVersion,
                                         CAMEL_QUARKUS_BOM_ARTIFACT,
                                         quarkusVersion);
            
            LOGGER.fine("Fetching Quarkus BOM from: " + pomUrl);
            
            try (InputStream is = new URI(pomUrl).toURL().openStream()) {
                String pomContent = IOUtils.toString(is, StandardCharsets.UTF_8);
                
                // Parse XML to find org.apache.camel dependency version
                // Look for pattern: <groupId>org.apache.camel</groupId> followed by <version>X.Y.Z</version>
                String camelVersion = extractCamelVersionFromPom(pomContent);
                
                if (camelVersion != null) {
                    LOGGER.fine("Extracted Camel version from Quarkus BOM: " + camelVersion);
                    return camelVersion;
                }
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to resolve Camel version from Quarkus BOM: " + quarkusVersion, e);
        }
        
        return null;
    }
    
    /**
     * Extracts the Apache Camel version from a Quarkus BOM POM content.
     * Parses the POM XML properly to find org.apache.camel dependencies regardless of element order.
     */
    private String extractCamelVersionFromPom(String pomContent) {
        try {
            // Parse POM XML using XmlMapper
            PomProject pom = xmlMapper.readValue(pomContent, PomProject.class);
            
            // Check dependencyManagement section first (typical for BOMs)
            if (pom.dependencyManagement != null && 
                pom.dependencyManagement.dependencies != null &&
                pom.dependencyManagement.dependencies.dependency != null) {
                
                for (PomDependency dep : pom.dependencyManagement.dependencies.dependency) {
                    if ("org.apache.camel".equals(dep.groupId) && dep.version != null) {
                        LOGGER.fine("Found Camel version in dependencyManagement: " + dep.version);
                        return dep.version;
                    }
                }
            }
            
            // Fallback to regular dependencies section
            if (pom.dependencies != null && pom.dependencies.dependency != null) {
                for (PomDependency dep : pom.dependencies.dependency) {
                    if ("org.apache.camel".equals(dep.groupId) && dep.version != null) {
                        LOGGER.fine("Found Camel version in dependencies: " + dep.version);
                        return dep.version;
                    }
                }
            }
            
            LOGGER.warning("No org.apache.camel dependency found in POM");
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to extract Camel version from POM content", e);
        }
        
        return null;
    }
    
    /**
     * Fetches available versions from the Maven repository's maven-metadata.xml
     */
    private List<String> fetchAvailableVersions(String repository) throws Exception {
        String metadataUrl = String.format("%s/%s/%s/maven-metadata.xml", 
                                          repository, CAMEL_LAUNCHER_GROUP_PATH, CAMEL_LAUNCHER_ARTIFACT);
        
        LOGGER.fine("Fetching metadata from: " + metadataUrl);
        
        try (InputStream is = new URI(metadataUrl).toURL().openStream()) {
            String xmlContent = IOUtils.toString(is, StandardCharsets.UTF_8);
            MavenMetadata metadata = xmlMapper.readValue(xmlContent, MavenMetadata.class);
            
            if (metadata.versioning == null || metadata.versioning.versions == null || 
                metadata.versioning.versions.version == null) {
                LOGGER.warning("No versions found in maven-metadata.xml");
                return Collections.emptyList();
            }
            
            List<String> versionList = metadata.versioning.versions.version;
            LOGGER.fine("Found " + versionList.size() + " available versions in repository");
            return versionList;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to parse maven-metadata.xml from: " + metadataUrl, e);
            throw e;
        }
    }
    
    /**
     * Finds the best matching version from available versions.
     * Matches based on major.minor.patch, selecting the highest build number.
     * When searching in a specific repository, matches versions from that repository.
     */
    private String findBestMatch(VersionInfo inputVersion, List<String> availableVersions) {
        LOGGER.fine("Searching for match. Input version: " + inputVersion + 
                   " (major=" + inputVersion.major + ", minor=" + inputVersion.minor + 
                   ", patch=" + inputVersion.patch + ", isRedhat=" + inputVersion.isRedhat + ")");
        
        String bestMatch = null;
        int highestBuildNumber = -1;
        
        for (String version : availableVersions) {
            try {
                VersionInfo candidate = parseVersion(version);
                
                // Match major.minor.patch only
                // The repository selection already ensures we're looking at the right type (redhat vs community)
                if (candidate.major == inputVersion.major &&
                    candidate.minor == inputVersion.minor &&
                    candidate.patch == inputVersion.patch) {
                    
                    LOGGER.fine("Found matching version: " + version);
                    
                    // Select the version with the highest build number
                    if (candidate.buildNumber > highestBuildNumber) {
                        highestBuildNumber = candidate.buildNumber;
                        bestMatch = version;
                    }
                }
            } catch (IllegalArgumentException e) {
                LOGGER.log(Level.FINE, "Skipping invalid version format: " + version);
            }
        }
        
        return bestMatch;
    }
    
    /**
     * Parses a version string into structured components.
     * Supports formats: major.minor.patch and major.minor.patch.redhat-buildNumber
     */
    private VersionInfo parseVersion(String version) {
        // Pattern: major.minor.patch[.redhat-buildNumber]
        Pattern pattern = Pattern.compile("(\\d+)\\.(\\d+)\\.(\\d+)(?:\\.redhat-(\\d+))?");
        Matcher matcher = pattern.matcher(version);
        
        if (matcher.find()) {
            int major = Integer.parseInt(matcher.group(1));
            int minor = Integer.parseInt(matcher.group(2));
            int patch = Integer.parseInt(matcher.group(3));
            boolean isRedhat = matcher.group(4) != null;
            int buildNumber = isRedhat ? Integer.parseInt(matcher.group(4)) : 0;
            
            return new VersionInfo(major, minor, patch, isRedhat, buildNumber);
        }
        
        throw new IllegalArgumentException("Invalid version format: " + version);
    }
    
    /**
     * Internal class to hold parsed version information
     */
    private static class VersionInfo {
        final int major;
        final int minor;
        final int patch;
        final boolean isRedhat;
        final int buildNumber;
        
        VersionInfo(int major, int minor, int patch, boolean isRedhat, int buildNumber) {
            this.major = major;
            this.minor = minor;
            this.patch = patch;
            this.isRedhat = isRedhat;
            this.buildNumber = buildNumber;
        }
        
        @Override
        public String toString() {
            return String.format("%d.%d.%d%s", major, minor, patch, 
                               isRedhat ? ".redhat-" + buildNumber : "");
        }
    }
    
    /**
     * Jackson XML mapping class for Maven metadata
     */
    private static class MavenMetadata {
        @JacksonXmlProperty(localName = "versioning")
        public Versioning versioning;
    }
    
    /**
     * Jackson XML mapping class for versioning section
     */
    private static class Versioning {
        @JacksonXmlProperty(localName = "versions")
        public Versions versions;
    }
    
    /**
     * Jackson XML mapping class for versions list
     */
    private static class Versions {
        @JacksonXmlElementWrapper(useWrapping = false)
        @JacksonXmlProperty(localName = "version")
        public List<String> version = new ArrayList<>();
    }
    
    /**
     * Jackson XML mapping class for POM project structure
     */
    private static class PomProject {
        @JacksonXmlProperty(localName = "dependencyManagement")
        public PomDependencyManagement dependencyManagement;
        
        @JacksonXmlProperty(localName = "dependencies")
        public PomDependencies dependencies;
    }
    
    /**
     * Jackson XML mapping class for POM dependencyManagement section
     */
    private static class PomDependencyManagement {
        @JacksonXmlProperty(localName = "dependencies")
        public PomDependencies dependencies;
    }
    
    /**
     * Jackson XML mapping class for POM dependencies container
     */
    private static class PomDependencies {
        @JacksonXmlElementWrapper(useWrapping = false)
        @JacksonXmlProperty(localName = "dependency")
        public List<PomDependency> dependency = new ArrayList<>();
    }
    
    /**
     * Jackson XML mapping class for individual POM dependency
     */
    private static class PomDependency {
        @JacksonXmlProperty(localName = "groupId")
        public String groupId;
        
        @JacksonXmlProperty(localName = "artifactId")
        public String artifactId;
        
        @JacksonXmlProperty(localName = "version")
        public String version;
    }
}
