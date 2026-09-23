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
package io.kaoto.camelcatalog.commands;

import io.kaoto.camelcatalog.beans.ConfigBean;
import io.kaoto.camelcatalog.generator.camel.CamelCatalogGenerator;
import io.kaoto.camelcatalog.generator.camel.CamelCatalogGeneratorBuilder;
import io.kaoto.camelcatalog.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.util.ArrayList;

import com.fasterxml.jackson.databind.ObjectMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

class GenerateCommandTest {
    @TempDir
    File tempDir;

    private GenerateCommand generateCommand;
    private CatalogDefinition catalogDefinition;

    @BeforeEach
    void setUp() {
        catalogDefinition = new CatalogDefinition();
        catalogDefinition.setFileName("index.json");
        catalogDefinition.setName("test-camel-catalog");
        catalogDefinition.setVersion("4.8.0");
        catalogDefinition.setRuntime(CatalogRuntime.MAIN);
        // Simulate what generate() stamps for a Main-runtime entry: camelCatalogVersion
        // equals the catalog version; runtimeProviderVersion and frameworkVersion are null.
        catalogDefinition.setCamelCatalogVersion("4.8.0");

        CatalogCliArgument catalogCliArg = new CatalogCliArgument();
        catalogCliArg.setRuntime(CatalogRuntime.MAIN);
        catalogCliArg.setCatalogVersion("4.8.0");

        ConfigBean configBean = new ConfigBean();
        configBean.setOutputFolder(tempDir.toString());
        configBean.setCatalogsName("test-camel-catalog");
        configBean.addCatalogVersion(catalogCliArg);
        configBean.addXsltVersion("3.0");
        configBean.setKameletsVersion("1.0.0");

        generateCommand = new GenerateCommand(configBean);
    }

    @Test
    void testGeneratorCalledWithCorrectParameters() {
        try (var mockedBuilder = mockConstruction(CamelCatalogGeneratorBuilder.class, (mockBuilder, context) -> {
            when(mockBuilder.withRuntime(any(CatalogRuntime.class))).thenCallRealMethod().thenReturn(mockBuilder);
            when(mockBuilder.withCatalogVersion(anyString())).thenCallRealMethod().thenReturn(mockBuilder);
            when(mockBuilder.withKameletsVersion(anyString())).thenCallRealMethod().thenReturn(mockBuilder);
            when(mockBuilder.withCamelKCRDsVersion(anyString())).thenCallRealMethod().thenReturn(mockBuilder);
            when(mockBuilder.withVerbose(anyBoolean())).thenCallRealMethod().thenReturn(mockBuilder);
            when(mockBuilder.withResolvedVersions(any())).thenReturn(mockBuilder);
            when(mockBuilder.withDefaultCliVersion(any())).thenReturn(mockBuilder);

            when(mockBuilder.withOutputDirectory(any(File.class))).thenReturn(mockBuilder);
            when(mockBuilder.build()).thenAnswer(invocation -> {
                CamelCatalogGenerator catalogGenerator = mock(CamelCatalogGenerator.class);
                when(catalogGenerator.generate()).thenReturn(catalogDefinition);
                return catalogGenerator;
            });
        })) {
            generateCommand.run();

            CamelCatalogGeneratorBuilder builder = mockedBuilder.constructed().get(0);

            verify(builder, times(1)).withRuntime(CatalogRuntime.MAIN);
            verify(builder, times(1)).withCatalogVersion("4.8.0");
            verify(builder, times(1)).withKameletsVersion("1.0.0");
            verify(builder, times(1)).withCamelKCRDsVersion("2.3.1");

            File expectedFolder = new File(tempDir, "camel-main/4.8.0");
            verify(builder, times(1)).withOutputDirectory(expectedFolder);

            /* This path will be used to relatively load the subsequent files, it always needs to use `/` */
            String expectedFile = "camel-main/4.8.0/index.json";
            assertEquals(expectedFile, catalogDefinition.getFileName());
        }
    }

    @Test
    void testCatalogLibraryOutput() {
        var ref = new Object() {
            Integer version = null;
            String name = null;
        };

        try (
                var mockedBuilder = mockConstruction(CamelCatalogGeneratorBuilder.class, (mockBuilder, context) -> {
                    when(mockBuilder.withRuntime(any(CatalogRuntime.class))).thenCallRealMethod().thenReturn(mockBuilder);
                    when(mockBuilder.withCatalogVersion(anyString())).thenCallRealMethod().thenReturn(mockBuilder);
                    when(mockBuilder.withKameletsVersion(anyString())).thenCallRealMethod().thenReturn(mockBuilder);
                    when(mockBuilder.withCamelKCRDsVersion(anyString())).thenCallRealMethod().thenReturn(mockBuilder);
                    when(mockBuilder.withVerbose(anyBoolean())).thenCallRealMethod().thenReturn(mockBuilder);
                    when(mockBuilder.withResolvedVersions(any())).thenReturn(mockBuilder);
                    when(mockBuilder.withDefaultCliVersion(any())).thenReturn(mockBuilder);

                    when(mockBuilder.withOutputDirectory(any(File.class))).thenReturn(mockBuilder);
                    when(mockBuilder.build()).thenAnswer(invocation -> {
                        CamelCatalogGenerator catalogGenerator = mock(CamelCatalogGenerator.class);
                        when(catalogGenerator.generate()).thenReturn(catalogDefinition);
                        return catalogGenerator;
                    });
                });
                var mockedLibrary = mockConstruction(CatalogLibrary.class, (mockLibrary, context) -> {
                    ref.version = (Integer) context.arguments().get(0);
                    ref.name = (String) context.arguments().get(1);
                    mockLibrary.definitions = new ArrayList<>();
                    doCallRealMethod().when(mockLibrary).getName();
                    doCallRealMethod().when(mockLibrary).getDefinitions();
                    doCallRealMethod().when(mockLibrary).addDefinition(any(CatalogDefinition.class));
                    doCallRealMethod().when(mockLibrary).setXsltCatalogs(anyString());
                    doCallRealMethod().when(mockLibrary).getXsltCatalogs();
                })
        ) {
            generateCommand.run();

            CatalogLibrary library = mockedLibrary.constructed().get(0);

            assertEquals(1, mockedLibrary.constructed().size());
            assertEquals(3, ref.version);
            assertEquals("test-camel-catalog", ref.name);

            // XSLT must NOT be in definitions
            assertEquals(1, library.getDefinitions().size());

            CatalogLibraryEntry catalogLibraryEntry = library.getDefinitions().get(0);
            assertEquals("test-camel-catalog", catalogLibraryEntry.name());
            assertEquals("4.8.0", catalogLibraryEntry.version());
            assertEquals("Main", catalogLibraryEntry.runtime());

            /* This path will be used to relatively load the subsequent files, it always needs to use `/` */
            String expectedFile = "camel-main/4.8.0/index.json";
            assertEquals(expectedFile, catalogLibraryEntry.fileName());

            // For a Main-runtime entry the version triple stamped by generate() has
            // camelCatalogVersion equal to the catalog version, while runtimeProviderVersion
            // and frameworkVersion are null (Main has no separate runtime/framework artifact).
            assertEquals("4.8.0", catalogDefinition.getCamelCatalogVersion());
            assertNull(catalogDefinition.getRuntimeProviderVersion());
            assertNull(catalogDefinition.getFrameworkVersion());

            // XSLT must be stored as a dedicated top-level path string, not inside definitions
            String xsltCatalogs = library.getXsltCatalogs();
            assertNotNull(xsltCatalogs, "xsltCatalogs must be set on the library");
            assertTrue(xsltCatalogs.startsWith("xslt/index-"),
                    "xsltCatalogs path must point into xslt/ with a hashed filename: " + xsltCatalogs);
            assertTrue(xsltCatalogs.endsWith(".json"),
                    "xsltCatalogs path must end with .json: " + xsltCatalogs);

            // Verify the XSLT root index file and resolution of the 3.0 version entry
            File xsltRootIndexFile = new File(tempDir, xsltCatalogs);
            assertTrue(xsltRootIndexFile.exists(), "XSLT root index file must exist at " + xsltRootIndexFile);

            ObjectMapper mapper = new ObjectMapper();
            try {
                CatalogDefinition xsltRootDef = mapper.readValue(xsltRootIndexFile, CatalogDefinition.class);
                assertEquals("XSLT Catalogs", xsltRootDef.getName());
                assertEquals(CatalogRuntime.XSLT, xsltRootDef.getRuntime());
                assertTrue(xsltRootDef.getCatalogs().containsKey("3.0"), "XSLT root index must contain '3.0' version entry");

                CatalogDefinitionEntry entry30 = xsltRootDef.getCatalogs().get("3.0");
                assertEquals("3.0", entry30.name());
                assertEquals("3.0", entry30.version());
                assertEquals("3.0/xslt-xpath-functions.json", entry30.file());

                // Verify the resolved 3.0 function catalog file exists
                File xpathFuncsFile = new File(xsltRootIndexFile.getParentFile(), entry30.file());
                assertTrue(xpathFuncsFile.exists(), "xslt-xpath-functions.json file must exist at " + xpathFuncsFile);
            } catch (Exception e) {
                throw new RuntimeException("Failed to verify XSLT catalog files", e);
            }
        }
    }
    @Test
    void testXsltNotGeneratedWhenNotSpecified() {
        ConfigBean noXsltConfig = new ConfigBean();
        noXsltConfig.setOutputFolder(tempDir.toString());
        noXsltConfig.setCatalogsName("test-camel-catalog");
        CatalogCliArgument catalogCliArg = new CatalogCliArgument();
        catalogCliArg.setRuntime(CatalogRuntime.MAIN);
        catalogCliArg.setCatalogVersion("4.8.0");
        noXsltConfig.addCatalogVersion(catalogCliArg);
        noXsltConfig.setKameletsVersion("1.0.0");

        GenerateCommand cmd = new GenerateCommand(noXsltConfig);

        try (
                var mockedBuilder = mockConstruction(CamelCatalogGeneratorBuilder.class, (mockBuilder, context) -> {
                    when(mockBuilder.withRuntime(any(CatalogRuntime.class))).thenCallRealMethod().thenReturn(mockBuilder);
                    when(mockBuilder.withCatalogVersion(anyString())).thenCallRealMethod().thenReturn(mockBuilder);
                    when(mockBuilder.withKameletsVersion(anyString())).thenCallRealMethod().thenReturn(mockBuilder);
                    when(mockBuilder.withCamelKCRDsVersion(anyString())).thenCallRealMethod().thenReturn(mockBuilder);
                    when(mockBuilder.withVerbose(anyBoolean())).thenCallRealMethod().thenReturn(mockBuilder);
                    when(mockBuilder.withResolvedVersions(any())).thenReturn(mockBuilder);
                    when(mockBuilder.withDefaultCliVersion(any())).thenReturn(mockBuilder);
                    when(mockBuilder.withOutputDirectory(any(File.class))).thenReturn(mockBuilder);
                    when(mockBuilder.build()).thenAnswer(invocation -> {
                        CamelCatalogGenerator catalogGenerator = mock(CamelCatalogGenerator.class);
                        when(catalogGenerator.generate()).thenReturn(catalogDefinition);
                        return catalogGenerator;
                    });
                });
                var mockedLibrary = mockConstruction(CatalogLibrary.class, (mockLibrary, context) -> {
                    mockLibrary.definitions = new ArrayList<>();
                    doCallRealMethod().when(mockLibrary).getName();
                    doCallRealMethod().when(mockLibrary).getDefinitions();
                    doCallRealMethod().when(mockLibrary).addDefinition(any(CatalogDefinition.class));
                    doCallRealMethod().when(mockLibrary).setStarterTemplates(any());
                    doCallRealMethod().when(mockLibrary).getStarterTemplates();
                    doCallRealMethod().when(mockLibrary).setXsltCatalogs(any());
                    doCallRealMethod().when(mockLibrary).getXsltCatalogs();
                })) {
            cmd.run();

            CatalogLibrary library = mockedLibrary.constructed().get(0);
            assertNull(library.getXsltCatalogs(), "xsltCatalogs must be null when xslt versions are not specified");
        }
    }

    @Test
    void testCatalogLibraryHasStarterTemplatesField() throws Exception {
        try (
            var mockedCamelBuilder = mockConstruction(CamelCatalogGeneratorBuilder.class, (mockBuilder, context) -> {
                when(mockBuilder.withRuntime(any(CatalogRuntime.class))).thenCallRealMethod().thenReturn(mockBuilder);
                when(mockBuilder.withCatalogVersion(anyString())).thenCallRealMethod().thenReturn(mockBuilder);
                when(mockBuilder.withKameletsVersion(anyString())).thenCallRealMethod().thenReturn(mockBuilder);
                when(mockBuilder.withCamelKCRDsVersion(anyString())).thenCallRealMethod().thenReturn(mockBuilder);
                when(mockBuilder.withVerbose(anyBoolean())).thenCallRealMethod().thenReturn(mockBuilder);
                when(mockBuilder.withResolvedVersions(any())).thenReturn(mockBuilder);
                when(mockBuilder.withDefaultCliVersion(any())).thenReturn(mockBuilder);
                when(mockBuilder.withOutputDirectory(any(File.class))).thenReturn(mockBuilder);
                when(mockBuilder.build()).thenAnswer(invocation -> {
                    CamelCatalogGenerator gen = mock(CamelCatalogGenerator.class);
                    when(gen.generate()).thenReturn(catalogDefinition);
                    return gen;
                });
            })
        ) {
            generateCommand.run();

            // index.json is written directly into tempDir (the output folder)
            File indexFile = new File(tempDir, "index.json");
            assertTrue(indexFile.exists(), "index.json not written to " + tempDir);

            ObjectMapper mapper = new ObjectMapper();
            CatalogLibrary library = mapper.readValue(indexFile, CatalogLibrary.class);
            assertNotNull(library.getStarterTemplates(), "starterTemplates must not be null");
            assertTrue(library.getStarterTemplates().startsWith("starter-templates/index-"),
                    "starterTemplates path must point into starter-templates/: " + library.getStarterTemplates());
        }
    }
}
