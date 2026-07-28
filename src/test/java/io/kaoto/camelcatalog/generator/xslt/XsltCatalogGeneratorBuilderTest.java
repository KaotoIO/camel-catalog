/*
 * Copyright (C) 2026 Red Hat, Inc.
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
package io.kaoto.camelcatalog.generator.xslt;

import io.kaoto.camelcatalog.generator.CatalogGenerator;
import io.kaoto.camelcatalog.model.CatalogDefinition;
import io.kaoto.camelcatalog.model.CatalogRuntime;
import io.kaoto.camelcatalog.model.ResolvedVersions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class XsltCatalogGeneratorBuilderTest {

    @TempDir
    Path tempDir;

    @Test
    void testBuildReturnsCatalogGenerator() {
        CatalogGenerator generator = new XsltCatalogGeneratorBuilder()
                .withCatalogVersion("3.0")
                .withOutputDirectory(tempDir.toFile())
                .withVerbose(false)
                .build();

        assertNotNull(generator);
        assertInstanceOf(XsltCatalogGenerator.class, generator);
    }

    @Test
    void testBuilderPassesCatalogVersion() {
        CatalogGenerator generator = new XsltCatalogGeneratorBuilder()
                .withCatalogVersion("3.0")
                .withOutputDirectory(tempDir.toFile())
                .withVerbose(false)
                .build();

        CatalogDefinition catalogDefinition = generator.generate();

        assertEquals("XSLT 3.0", catalogDefinition.getName());
        assertEquals("3.0", catalogDefinition.getVersion());
        assertEquals(CatalogRuntime.XSLT, catalogDefinition.getRuntime());
    }

    @Test
    void testBuilderPassesOutputDirectory() {
        File outputDir = tempDir.resolve("custom-output").toFile();
        outputDir.mkdirs();

        CatalogGenerator generator = new XsltCatalogGeneratorBuilder()
                .withCatalogVersion("3.0")
                .withOutputDirectory(outputDir)
                .withVerbose(false)
                .build();

        generator.generate();

        assertTrue(new File(outputDir, "xslt-xpath-functions.json").exists());
        assertTrue(new File(outputDir, "index.json").exists());
    }

    @Test
    void testBuilderPassesResolvedVersions() {
        var resolvedVersions = new ResolvedVersions(
                "4.15.0",
                "4.15.0",
                "3.27.0",
                "4.15.0"
        );

        CatalogGenerator generator = new XsltCatalogGeneratorBuilder()
                .withCatalogVersion("3.0")
                .withOutputDirectory(tempDir.toFile())
                .withVerbose(false)
                .withResolvedVersions(resolvedVersions)
                .build();

        CatalogDefinition catalogDefinition = generator.generate();

        assertEquals("4.15.0", catalogDefinition.getCamelCatalogVersion());
        assertEquals("4.15.0", catalogDefinition.getRuntimeProviderVersion());
        assertEquals("3.27.0", catalogDefinition.getFrameworkVersion());
    }

    @Test
    void testBuilderWithNullResolvedVersions() {
        CatalogGenerator generator = new XsltCatalogGeneratorBuilder()
                .withCatalogVersion("3.0")
                .withOutputDirectory(tempDir.toFile())
                .withVerbose(false)
                .build();

        CatalogDefinition catalogDefinition = generator.generate();

        assertNull(catalogDefinition.getCamelCatalogVersion());
        assertNull(catalogDefinition.getRuntimeProviderVersion());
        assertNull(catalogDefinition.getFrameworkVersion());
    }

    @Test
    void testBuilderWithVerboseFlag() {
        CatalogGenerator generator = new XsltCatalogGeneratorBuilder()
                .withCatalogVersion("3.0")
                .withOutputDirectory(tempDir.toFile())
                .withVerbose(true)
                .build();

        assertDoesNotThrow(generator::generate);
    }

    @Test
    void testBuilderFluentChaining() {
        var builder = new XsltCatalogGeneratorBuilder();

        assertSame(builder, builder.withCatalogVersion("3.0"));
        assertSame(builder, builder.withOutputDirectory(tempDir.toFile()));
        assertSame(builder, builder.withVerbose(false));
        assertSame(builder, builder.withResolvedVersions(null));
    }
}
