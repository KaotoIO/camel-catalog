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
package io.kaoto.camelcatalog.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class CatalogSerializationTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void definitionExposesVersionTriple() throws Exception {
        CatalogDefinition def = new CatalogDefinition();
        def.setName("Camel Quarkus 3.33.0.redhat-00007");
        def.setRuntime(CatalogRuntime.Quarkus);
        def.setVersion("3.33.0.redhat-00007");
        def.setCliVersion("4.20.0");
        def.setCamelCatalogVersion("4.18.1.redhat-00020");
        def.setRuntimeProviderVersion("3.33.0.redhat-00007");
        def.setFrameworkVersion("3.33.1.redhat-00006");

        String json = mapper.writeValueAsString(def);

        assertTrue(json.contains("\"cliVersion\":\"4.20.0\""), json);
        assertTrue(json.contains("\"camelCatalogVersion\":\"4.18.1.redhat-00020\""), json);
        assertTrue(json.contains("\"runtimeProviderVersion\":\"3.33.0.redhat-00007\""), json);
        assertTrue(json.contains("\"frameworkVersion\":\"3.33.1.redhat-00006\""), json);
    }

    @Test
    void libraryEntryCarriesVersionTriple() throws Exception {
        CatalogDefinition def = new CatalogDefinition();
        def.setName("Camel Quarkus 3.33.0.redhat-00007");
        def.setRuntime(CatalogRuntime.Quarkus);
        def.setVersion("3.33.0.redhat-00007");
        def.setFileName("camel-quarkus/3.33.0.redhat-00007/index-abc.json");
        def.setExecutorVersion("4.18.1.redhat-00020");
        def.setCliVersion("4.20.0");
        def.setCamelCatalogVersion("4.18.1.redhat-00020");
        def.setRuntimeProviderVersion("3.33.0.redhat-00007");
        def.setFrameworkVersion("3.33.1.redhat-00006");

        CatalogLibrary library = new CatalogLibrary(3, "Test catalog");
        library.addDefinition(def);

        String json = mapper.writeValueAsString(library.getDefinitions().get(0));

        assertTrue(json.contains("\"camelCatalogVersion\":\"4.18.1.redhat-00020\""), json);
        assertTrue(json.contains("\"runtimeProviderVersion\":\"3.33.0.redhat-00007\""), json);
        assertTrue(json.contains("\"frameworkVersion\":\"3.33.1.redhat-00006\""), json);
        assertTrue(json.contains("\"cliVersion\":\"4.20.0\""), json);
    }
}
