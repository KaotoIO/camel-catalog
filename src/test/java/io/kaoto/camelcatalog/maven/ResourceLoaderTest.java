package io.kaoto.camelcatalog.maven;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URL;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.net.JarURLConnection;
import java.nio.file.Path;
import java.nio.file.Files;
import java.util.List;
import java.util.Collections;
import java.util.Iterator;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ResourceLoaderTest {
    private KaotoMavenVersionManager versionManager;
    private ResourceLoader resourceLoader;

    @BeforeEach
    void setUp() {
        versionManager = mock(KaotoMavenVersionManager.class);
        resourceLoader = new ResourceLoader(versionManager, false);
    }

    @Test
    void testConstructorAndGetter() {
        assertEquals(versionManager, resourceLoader.getKaotoVersionManager());
    }

    @Test
    void testGetResourceAsStringReturnsContent() {
        ClassLoader cl = mock(ClassLoader.class);
        when(versionManager.getClassLoader()).thenReturn(cl);

        String content = "test-content";
        InputStream is = new ByteArrayInputStream(content.getBytes());
        when(cl.getResourceAsStream("test.txt")).thenReturn(is);

        String result = resourceLoader.getResourceAsString("test.txt");

        assertEquals(content, result);
    }

    @Test
    void testGetResourceAsStringReturnsNullIfNotFound() {
        ClassLoader cl = mock(ClassLoader.class);
        when(versionManager.getClassLoader()).thenReturn(cl);
        when(cl.getResourceAsStream("missing.txt")).thenReturn(null);

        String result = resourceLoader.getResourceAsString("missing.txt");

        assertNull(result);
    }

    @Test
    void testGetResourceAsStringReturnsNullOnIOException() {
        ClassLoader cl = mock(ClassLoader.class);
        when(versionManager.getClassLoader()).thenReturn(cl);

        InputStream is = mock(InputStream.class);
        when(cl.getResourceAsStream("ioerror.txt")).thenReturn(is);
        try {
            when(is.readAllBytes()).thenThrow(new java.io.IOException("fail"));
        } catch (Exception ignored) {
        }

        String result = resourceLoader.getResourceAsString("ioerror.txt");

        assertNull(result);
    }

    @Test
    void testConfigureRepositoriesAddsCentral() {
        KaotoMavenVersionManager kaotoVersionManager = new KaotoMavenVersionManager();
        resourceLoader = new ResourceLoader(kaotoVersionManager, true);

        resourceLoader.configureRepositories("1.0.0");

        assertTrue(kaotoVersionManager.repositories.containsKey("central"));
        assertFalse(kaotoVersionManager.repositories.containsKey("redhat"));
    }

    @Test
    void testLoadResourcesFromJar() {
        Map<String, String> schemas = new HashMap<>();

        resourceLoader = new ResourceLoader(new KaotoMavenVersionManager(), true);
        resourceLoader.loadResourcesFromFolderAsString("schema", schemas, ".json");

        assertFalse(schemas.isEmpty());
        assertTrue(schemas.containsKey("camelYamlDsl"));
    }

    @Test
    void testLoadResourcesFromFiles() {
        Map<String, String> kaotoPatterns = new HashMap<>();

        resourceLoader = new ResourceLoader(new KaotoMavenVersionManager(), true);
        resourceLoader.loadResourcesFromFolderAsString("kaoto-patterns", kaotoPatterns, ".json");

        assertFalse(kaotoPatterns.isEmpty());
        assertTrue(kaotoPatterns.containsKey("kaoto-datamapper"));
    }

    @Test
    void testConfigureRepositoriesAddsRedhat() {
        KaotoMavenVersionManager kaotoVersionManager = new KaotoMavenVersionManager();
        resourceLoader = new ResourceLoader(kaotoVersionManager, true);

        resourceLoader.configureRepositories("1.0.0-redhat-00001");

        assertTrue(kaotoVersionManager.repositories.containsKey("central"));
        assertTrue(kaotoVersionManager.repositories.containsKey("maven.redhat.ga"));
        assertEquals("https://maven.repository.redhat.com/ga/", kaotoVersionManager.repositories.get("maven.redhat.ga"));
    }

    @Test
    void testConfigureRepositoriesOmitCentralIfExist() {
        KaotoMavenVersionManager kaotoVersionManager = new KaotoMavenVersionManager();
        kaotoVersionManager.addMavenRepository("central", "original-url");
        resourceLoader = new ResourceLoader(kaotoVersionManager, true);

        resourceLoader.configureRepositories("1.0.0");

        assertTrue(kaotoVersionManager.repositories.containsKey("central"));
        assertEquals("original-url", kaotoVersionManager.repositories.get("central"));
    }

    @Test
    void testConfigureRepositoriesDoesNotAddRedhatIfNotRedhatVersion() {
        KaotoMavenVersionManager kaotoVersionManager = new KaotoMavenVersionManager();
        kaotoVersionManager.addMavenRepository("maven.redhat.ga", "original-url");
        resourceLoader = new ResourceLoader(kaotoVersionManager, true);

        resourceLoader.configureRepositories("1.0.0-redhat-00001");

        assertTrue(kaotoVersionManager.repositories.containsKey("maven.redhat.ga"));
        assertEquals("original-url", kaotoVersionManager.repositories.get("maven.redhat.ga"));
    }

}
