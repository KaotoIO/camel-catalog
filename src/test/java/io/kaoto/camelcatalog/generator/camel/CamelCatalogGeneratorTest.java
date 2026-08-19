/*
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

import io.kaoto.camelcatalog.maven.CamelCatalogVersionLoader;
import io.kaoto.camelcatalog.model.CatalogDefinition;
import io.kaoto.camelcatalog.model.CatalogRuntime;
import org.apache.camel.catalog.CamelCatalog;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.lang.reflect.InvocationTargetException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CamelCatalogGeneratorTest {
    @TempDir
    File outputDirectory;

    @Test
    void processCatalogPropagatesHandlerFailure() throws Exception {
        var camelCatalog = mock(CamelCatalog.class);
        var handlerFailure = new IllegalStateException("handler failure");
        when(camelCatalog.findComponentNames()).thenThrow(handlerFailure);

        var loader = mock(CamelCatalogVersionLoader.class);
        when(loader.getCamelCatalog()).thenReturn(camelCatalog);
        when(loader.getRuntime()).thenReturn(CatalogRuntime.Main);

        var generator = new CamelCatalogGenerator(loader, outputDirectory);
        var processCatalog = CamelCatalogGenerator.class.getDeclaredMethod(
                "processCatalog", CamelYamlDslSchemaProcessor.class, CatalogDefinition.class);
        processCatalog.setAccessible(true);

        var thrown = assertThrows(InvocationTargetException.class,
                () -> processCatalog.invoke(generator, null, new CatalogDefinition()));
        var cause = assertInstanceOf(IllegalStateException.class, thrown.getCause());
        assertEquals("Failed to generate Camel aggregate catalogs", cause.getMessage());
        assertSame(handlerFailure, cause.getCause());
    }
}
