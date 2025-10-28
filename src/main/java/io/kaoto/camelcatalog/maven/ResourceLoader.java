package io.kaoto.camelcatalog.maven;

import java.io.IOException;
import java.io.InputStream;
import java.net.JarURLConnection;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Map;
import java.util.Scanner;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ResourceLoader {
    private static final Logger LOGGER = Logger.getLogger(ResourceLoader.class.getName());
    private final KaotoMavenVersionManager kaotoVersionManager;
    private final boolean verbose;

    ResourceLoader(KaotoMavenVersionManager kaotoVersionManager, boolean verbose) {
        this.verbose = verbose;
        this.kaotoVersionManager = kaotoVersionManager;
    }

    public KaotoMavenVersionManager getKaotoVersionManager() {
        return kaotoVersionManager;
    }

    public String getResourceAsString(String resourceName) {
        ClassLoader classLoader = kaotoVersionManager.getClassLoader();
        try (InputStream inputStream = classLoader.getResourceAsStream(resourceName)) {
            if (inputStream == null) {
                LOGGER.log(Level.WARNING, "Resource not found: " + resourceName);
                return null;
            }
            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Error reading resource: " + resourceName, e);
            return null;
        }
    }

    public void loadResourcesFromFolderAsString(String resourceFolderName, Map<String, String> filesMap,
                                                String fileSuffix) {
        ClassLoader classLoader = kaotoVersionManager.getClassLoader();

        try {
            Iterator<URL> it = classLoader.getResources(resourceFolderName).asIterator();

            while (it.hasNext()) {
                URL resourceUrl = it.next();

                if ("jar".equals(resourceUrl.getProtocol())) {
                    JarURLConnection connection = (JarURLConnection) resourceUrl.openConnection();
                    JarFile jarFile = connection.getJarFile();
                    Enumeration<JarEntry> entries = jarFile.entries();

                    while (entries.hasMoreElements()) {
                        JarEntry entry = entries.nextElement();
                        if (entry.getName().startsWith(connection.getEntryName()) && !entry.isDirectory() &&
                                entry.getName().endsWith(fileSuffix)) {

                            if (verbose) {
                                LOGGER.log(Level.FINE, () -> "Parsing: " + entry.getName());
                            }

                            try (InputStream inputStream = jarFile.getInputStream(entry)) {
                                try (Scanner scanner = new Scanner(inputStream)) {
                                    scanner.useDelimiter("\\A");
                                    String filenameWithoutExtension =
                                            entry.getName().replace(resourceFolderName + "/", "")
                                                    .replace(fileSuffix, "");
                                    filesMap.put(filenameWithoutExtension, scanner.hasNext() ? scanner.next() : "");
                                }
                            } catch (IOException e) {
                                LOGGER.log(Level.SEVERE, e.toString(), e);
                            }
                        }
                    }
                } else if ("file".equals(resourceUrl.getProtocol())) {
                    try (var pathWalker = Files.walk(Paths.get(resourceUrl.toURI()))) {
                        pathWalker.filter(Files::isRegularFile).filter(path -> path.toString().endsWith(fileSuffix))
                                .forEach(path -> {
                                    if (verbose) {
                                        LOGGER.log(Level.INFO, () -> "Parsing: " + path);
                                    }

                                    try {
                                        String filenameWithoutExtension = path.toFile().getName()
                                                .substring(0, path.toFile().getName().lastIndexOf('.'));
                                        filesMap.put(filenameWithoutExtension, new String(Files.readAllBytes(path)));
                                    } catch (IOException e) {
                                        LOGGER.log(Level.SEVERE, e.toString(), e);
                                    }
                                });
                    } catch (IOException | URISyntaxException e) {
                        LOGGER.log(Level.SEVERE, e.toString(), e);
                    }
                }
            }
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, e.toString(), e);
        }
    }

    void configureRepositories(String version) {
        if (kaotoVersionManager.repositories.get("central") == null) {
            kaotoVersionManager.addMavenRepository("central", "https://repo1.maven.org/maven2/");
        }

        if (version.contains("redhat") && kaotoVersionManager.repositories.get("maven.redhat.ga") == null) {
            kaotoVersionManager.addMavenRepository("maven.redhat.ga", "https://maven.repository.redhat.com/ga/");
        }
    }
}
