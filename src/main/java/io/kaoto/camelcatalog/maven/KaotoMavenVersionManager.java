package io.kaoto.camelcatalog.maven;

import org.apache.camel.catalog.maven.MavenVersionManager;
import org.apache.camel.tooling.maven.MavenArtifact;
import org.apache.camel.tooling.maven.MavenDownloader;
import org.apache.camel.tooling.maven.MavenDownloaderImpl;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This class is a copy of the MavenVersionManager class from the Apache Camel
 * Catalog project.
 * This is needed because the `resolve` method doesn't resolve transitive
 * dependencies, and we need to load the underlying Camel YAML DSL from Quarkus and Spring
 * Boot runtime providers.
 */
public class KaotoMavenVersionManager extends MavenVersionManager {
    private static final Logger LOGGER = Logger.getLogger(KaotoMavenVersionManager.class.getName());

    protected final MavenDownloader downloader;
    protected final Map<String, String> repositories = new LinkedHashMap<>();
    private String version;
    private String runtimeProviderVersion;
    private boolean log;

    private KaotoMavenVersionManager(MavenDownloaderImpl downloader) {
        this.downloader = downloader;
        downloader.build();
    }

    public KaotoMavenVersionManager() {
        this(new MavenDownloaderImpl());
        this.setClassLoader(new KaotoOpenURLClassLoader());
    }

    public boolean getLog() {
        return log;
    }

    public void setLog(boolean log) {
        this.log = log;
    }

    /**
     * To add a 3rd party Maven repository.
     *
     * @param name the repository name
     * @param url  the repository url
     */
    public void addMavenRepository(String name, String url) {
        super.addMavenRepository(name, url);
        repositories.put(name, url);
    }

    @Override
    public String getLoadedVersion() {
        return version;
    }

    @Override
    public String getRuntimeProviderLoadedVersion() {
        return runtimeProviderVersion;
    }

    @Override
    public boolean loadRuntimeProviderVersion(String groupId, String artifactId, String version) {
        try {
            String gav = String.format("%s:%s:%s", groupId, artifactId, version);
            boolean shouldFetchTransitive = true;
            boolean shouldUseSnapshots = version.endsWith("SNAPSHOT");

            resolve(downloader, gav, shouldUseSnapshots, shouldFetchTransitive);
            this.runtimeProviderVersion = version;

            if (artifactId.contains("catalog")) {
                this.version = version;
            } else {
                this.runtimeProviderVersion = version;
            }

            return true;
        } catch (Exception e) {
            if (getLog()) {
                LOGGER.log(Level.WARNING,
                        String.format("Cannot load runtime provider version %s due %s", version, e.getMessage()), e);
            }
            return false;
        }
    }

    /**
     * Resolves Maven artifact using passed coordinates and use downloaded artifact
     * as one of the URLs in the
     * helperClassLoader, so further Catalog access may load resources from it.
     */
    public void resolve(MavenDownloader mavenDownloader, String gav, boolean useSnapshots, boolean transitive) {
        try {
            Set<String> extraRepositories = new LinkedHashSet<>(repositories.values());

            List<MavenArtifact> artifacts =
                    mavenDownloader.resolveArtifacts(Collections.singletonList(gav), extraRepositories, transitive,
                            useSnapshots);

            if (getLog()) {
                LOGGER.log(Level.INFO, () -> "Artifacts: " + artifacts);
            }

            for (MavenArtifact ma : artifacts) {
                ((KaotoOpenURLClassLoader) getClassLoader()).addURL(ma.getFile().toURI().toURL());
            }
        } catch (Throwable e) {
            if (getLog()) {
                LOGGER.log(Level.WARNING, String.format("Error resolving artifact %s due to %s", gav, e.getMessage()),
                        e);
            }
        }

    }

    @Override
    public InputStream getResourceAsStream(String name) {
        InputStream is = null;

        if (runtimeProviderVersion != null) {
            is = doGetResourceAsStream(name, runtimeProviderVersion);
        }
        if (is == null && version != null) {
            is = doGetResourceAsStream(name, version);
        }

        // Only use fallback for non-versioned resources (like JSON schemas files)
        // DO NOT use fallback for version-specific JSON files as this would return resources
        // from the wrong version (e.g., 4.15.0 instead of 4.8.5.redhat-00008)
        boolean isVersionedResource = name.contains("/models/")
                || name.contains("/components/") || name.contains("/languages/")
                || name.contains("/dataformats/") || name.contains("/beans/")
                || name.contains("/others/") || name.contains("/transformers/");
        if (getClassLoader() != null && is == null && !isVersionedResource) {
            is = getClassLoader().getResourceAsStream(name);
        } else if (is == null && isVersionedResource && getLog()) {
            LOGGER.log(Level.WARNING, String.format("Versioned resource '%s' not found for version '%s', returning null instead of falling back to runtime", name, version));
        }

        return is;
    }

    private InputStream doGetResourceAsStream(String name, String version) {
        if (version != null) {
            try {
                Enumeration<URL> urls = getClassLoader().getResources(name);
                boolean found = false;
                while (urls.hasMoreElements()) {
                    URL url = urls.nextElement();
                    if (getLog() && name.contains("bean.json")) {
                        LOGGER.log(Level.WARNING, String.format("Found resource '%s' at URL: %s", name, url.getPath()));
                    }
                    if (url.getPath().contains(version)) {
                        if (getLog() && name.contains("bean.json")) {
                            LOGGER.log(Level.WARNING, String.format("URL path contains version '%s', using this resource", version));
                        }
                        found = true;
                        return url.openStream();
                    }
                }
                if (getLog() && !found && name.contains("bean.json")) {
                    LOGGER.log(Level.WARNING, String.format("No URL found containing version '%s' for resource '%s'", version, name));
                }

            } catch (IOException e) {
                if (getLog()) {
                    LOGGER.log(Level.WARNING,
                            String.format("Cannot open resource %s and version %s due %s", name, version,
                                    e.getMessage()), e);

                }
            }
        }
        return null;
    }
}
