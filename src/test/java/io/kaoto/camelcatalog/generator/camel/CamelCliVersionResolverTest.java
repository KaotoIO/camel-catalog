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
package io.kaoto.camelcatalog.generator.camel;

import io.kaoto.camelcatalog.model.CatalogRuntime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CamelCliVersionResolverTest {

    private CamelCliVersionResolver resolver;

    @BeforeEach
    void setUp() {
        resolver = new CamelCliVersionResolver();
    }

    @Test
    void defaultForMainRuntime() {
        assertEquals("4.20.0", resolver.resolve("4.18.0", CatalogRuntime.Main));
    }

    @Test
    void defaultForQuarkusRuntime() {
        assertEquals("4.20.0", resolver.resolve("3.15.0", CatalogRuntime.Quarkus));
    }

    @Test
    void defaultForCitrusRuntime() {
        assertEquals("4.20.0", resolver.resolve("4.10.0", CatalogRuntime.Citrus));
    }

    @ParameterizedTest(name = "SpringBoot community {0} -> cliVersion {1}")
    @CsvSource({
            "4.20.0, 4.20.0",
            "4.19.0, 4.20.0",
            "4.19.1, 4.20.0",
            "4.18.0, 4.18.2",
            "4.18.2, 4.18.2",
            "4.14.0, 4.18.2",
            "4.8.0,  4.18.2",
    })
    void springBootCommunityUsesRules(String camelVersion, String expectedCliVersion) {
        assertEquals(expectedCliVersion, resolver.resolve(camelVersion, CatalogRuntime.SpringBoot));
    }

    @ParameterizedTest(name = "SpringBoot productized {0} -> default")
    @CsvSource({
            "4.18.0.redhat-00001",
            "4.20.0.redhat-00005",
            "4.14.2.redhat-00019",
    })
    void springBootProductizedUsesDefault(String camelVersion) {
        assertEquals("4.20.0", resolver.resolve(camelVersion, CatalogRuntime.SpringBoot));
    }

    @Test
    void nullVersionReturnsDefault() {
        assertEquals("4.20.0", resolver.resolve(null, CatalogRuntime.SpringBoot));
    }

    @Test
    void nullRuntimeReturnsDefault() {
        assertEquals("4.20.0", resolver.resolve("4.18.0", null));
    }
}
