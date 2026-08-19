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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CatalogRuntimeTest {

    private final ObjectMapper mapper = new ObjectMapper();

    /**
     * The serialized value ends up in the generated catalogs, therefore renaming
     * the enum constants must not change it.
     */
    @ParameterizedTest
    @CsvSource({
            "MAIN, Main",
            "QUARKUS, Quarkus",
            "SPRING_BOOT, SpringBoot",
            "CITRUS, Citrus",
            "XSLT, XSLT"
    })
    void serializesToStableId(CatalogRuntime runtime, String expectedId) throws Exception {
        assertEquals("\"" + expectedId + "\"", mapper.writeValueAsString(runtime));
        assertEquals(runtime, mapper.readValue("\"" + expectedId + "\"", CatalogRuntime.class));
    }

    @ParameterizedTest
    @CsvSource({
            "MAIN, camel-main",
            "QUARKUS, camel-quarkus",
            "SPRING_BOOT, camel-springboot",
            "CITRUS, citrus",
            "XSLT, xslt"
    })
    void buildsStableRuntimeFolder(CatalogRuntime runtime, String expectedFolder) {
        assertEquals(expectedFolder, runtime.getRuntimeFolder());
    }

    @Test
    void parsesLegacyAndConstantNames() {
        assertEquals(CatalogRuntime.SPRING_BOOT, CatalogRuntime.fromString("SpringBoot"));
        assertEquals(CatalogRuntime.SPRING_BOOT, CatalogRuntime.fromString("springboot"));
        assertEquals(CatalogRuntime.SPRING_BOOT, CatalogRuntime.fromString("SPRING_BOOT"));
        assertEquals(CatalogRuntime.MAIN, CatalogRuntime.fromString("main"));
    }
}
