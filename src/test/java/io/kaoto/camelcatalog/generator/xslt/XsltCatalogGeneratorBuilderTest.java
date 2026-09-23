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
import java.util.Arrays;

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

        assertEquals("XSLT Catalogs", catalogDefinition.getName());
        assertEquals("1", catalogDefinition.getVersion());
        assertEquals(CatalogRuntime.XSLT, catalogDefinition.getRuntime());
        assertTrue(catalogDefinition.getCatalogs().containsKey("3.0"));
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

        assertTrue(new File(outputDir, "3.0/xslt-xpath-functions.json").exists());
        assertTrue(Arrays.stream(outputDir.list()).anyMatch(f -> f.startsWith("index-") && f.endsWith(".json")),
                "Expected a hashed index-*.json file in " + outputDir);
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

        CatalogDefinition rootDef = generator.generate();

        assertEquals("4.15.0", rootDef.getCamelCatalogVersion());
        assertEquals("4.15.0", rootDef.getRuntimeProviderVersion());
        assertEquals("3.27.0", rootDef.getFrameworkVersion());
    }

    @Test
    void testBuilderWithNullResolvedVersions() {
        CatalogGenerator generator = new XsltCatalogGeneratorBuilder()
                .withCatalogVersion("3.0")
                .withOutputDirectory(tempDir.toFile())
                .withVerbose(false)
                .build();

        CatalogDefinition rootDef = generator.generate();

        assertNull(rootDef.getCamelCatalogVersion());
        assertNull(rootDef.getRuntimeProviderVersion());
        assertNull(rootDef.getFrameworkVersion());
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
    void testBuilderWithMultipleVersions() {
        File outputDir = tempDir.resolve("multi-version").toFile();
        outputDir.mkdirs();

        CatalogGenerator generator = new XsltCatalogGeneratorBuilder()
                .withCatalogVersions(java.util.List.of("3.0"))
                .withOutputDirectory(outputDir)
                .withVerbose(false)
                .build();

        CatalogDefinition rootDef = generator.generate();

        assertNotNull(rootDef);
        assertEquals("XSLT Catalogs", rootDef.getName());
        assertEquals("1", rootDef.getVersion());
        assertTrue(rootDef.getCatalogs().containsKey("3.0"));
        assertTrue(new File(outputDir, "3.0/xslt-xpath-functions.json").exists());
        assertTrue(new File(outputDir, rootDef.getFileName()).exists());
    }

    @Test
    void testBuilderFluentChaining() {
        var builder = new XsltCatalogGeneratorBuilder();

        assertSame(builder, builder.withCatalogVersion("3.0"));
        assertSame(builder, builder.withCatalogVersions(java.util.List.of("3.0")));
        assertSame(builder, builder.withOutputDirectory(tempDir.toFile()));
        assertSame(builder, builder.withVerbose(false));
        assertSame(builder, builder.withResolvedVersions(null));
    }
}
