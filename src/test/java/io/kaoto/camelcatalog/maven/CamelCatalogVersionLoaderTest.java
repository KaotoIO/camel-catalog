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
import io.kaoto.camelcatalog.model.MavenCoordinates;
import org.apache.camel.catalog.quarkus.QuarkusRuntimeProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.spy;

public class CamelCatalogVersionLoaderTest {
    private CamelCatalogVersionLoader camelCatalogVersionLoader;
    private KaotoMavenVersionManager kaotoVersionManager;

    @BeforeEach
    void setUp() {
        kaotoVersionManager = new KaotoMavenVersionManager();
        camelCatalogVersionLoader = new CamelCatalogVersionLoader(CatalogRuntime.Main, false);
    }

    @Test
    void testConstructorAndGetters() {
        assertNotNull(camelCatalogVersionLoader.getResourceLoader());
        assertNotNull(camelCatalogVersionLoader.getCamelCatalog());
        assertEquals(CatalogRuntime.Main, camelCatalogVersionLoader.getRuntime());
    }

    @Test
    void testGetKameletBoundariesInitiallyEmpty() {
        assertTrue(camelCatalogVersionLoader.getKameletBoundaries().isEmpty());
    }

    @Test
    void testGetKameletsInitiallyEmpty() {
        assertTrue(camelCatalogVersionLoader.getKamelets().isEmpty());
    }

    @Test
    void testGetCamelKCRDsInitiallyEmpty() {
        assertTrue(camelCatalogVersionLoader.getCamelKCRDs().isEmpty());
    }

    @Test
    void testGetLocalSchemasInitiallyEmpty() {
        assertTrue(camelCatalogVersionLoader.getLocalSchemas().isEmpty());
    }

    @Test
    void testGetKaotoPatternsInitiallyEmpty() {
        assertTrue(camelCatalogVersionLoader.getKaotoPatterns().isEmpty());
    }

    @Test
    void testLoadCamelCatalogSetRuntime() {
        camelCatalogVersionLoader = new CamelCatalogVersionLoader(CatalogRuntime.Quarkus, false);

        camelCatalogVersionLoader.loadCamelCatalog("3.20.0");

        assertEquals(CatalogRuntime.Quarkus, camelCatalogVersionLoader.getRuntime());
        assertInstanceOf(QuarkusRuntimeProvider.class,
                camelCatalogVersionLoader.getCamelCatalog().getRuntimeProvider());
    }

    @Test
    void testGetCatalogMavenCoordinatesMain() {
        MavenCoordinates coords = camelCatalogVersionLoader
                .getCatalogMavenCoordinates(CatalogRuntime.Main, "4.12.0");
        assertNotNull(coords);
        assertEquals("org.apache.camel", coords.getGroupId());
        assertEquals("camel-catalog", coords.getArtifactId());
        assertEquals("4.12.0", coords.getVersion());
    }

    @Test
    void testGetCatalogMavenCoordinatesQuarkus() {
        MavenCoordinates coords = camelCatalogVersionLoader
                .getCatalogMavenCoordinates(CatalogRuntime.Quarkus, "3.20.0");
        assertNotNull(coords);
        assertEquals("org.apache.camel.quarkus", coords.getGroupId());
        assertEquals("camel-quarkus-catalog", coords.getArtifactId());
        assertEquals("3.20.0", coords.getVersion());
    }

    @Test
    void testGetCatalogMavenCoordinatesSpringBoot() {
        MavenCoordinates coords = camelCatalogVersionLoader
                .getCatalogMavenCoordinates(CatalogRuntime.SpringBoot, "4.12.0");
        assertNotNull(coords);
        assertEquals("org.apache.camel.springboot", coords.getGroupId());
        assertEquals("camel-catalog-provider-springboot", coords.getArtifactId());
        assertEquals("4.12.0", coords.getVersion());
    }

    @Test
    void testGetYamlDslMavenCoordinatesMain() {
        MavenCoordinates coords = camelCatalogVersionLoader
                .getYamlDslMavenCoordinates(CatalogRuntime.Main, "4.12.0");
        assertNotNull(coords);
        assertEquals("org.apache.camel", coords.getGroupId());
        assertEquals("camel-yaml-dsl", coords.getArtifactId());
        assertEquals("4.12.0", coords.getVersion());
    }

    @Test
    void testGetYamlDslMavenCoordinatesQuarkus() {
        MavenCoordinates coords = camelCatalogVersionLoader
                .getYamlDslMavenCoordinates(CatalogRuntime.Quarkus, "3.20.0");
        assertNotNull(coords);
        assertEquals("org.apache.camel.quarkus", coords.getGroupId());
        assertEquals("camel-quarkus-yaml-dsl", coords.getArtifactId());
        assertEquals("3.20.0", coords.getVersion());
    }

    @Test
    void testGetYamlDslMavenCoordinatesSpringBoot() {
        MavenCoordinates coords = camelCatalogVersionLoader
                .getYamlDslMavenCoordinates(CatalogRuntime.SpringBoot, "4.12.0");
        assertNotNull(coords);
        assertEquals("org.apache.camel.springboot", coords.getGroupId());
        assertEquals("camel-yaml-dsl-starter", coords.getArtifactId());
        assertEquals("4.12.0", coords.getVersion());
    }
}
