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

package io.kaoto.camelcatalog.generator.templates;

import io.kaoto.camelcatalog.generator.CatalogGenerator;
import io.kaoto.camelcatalog.generator.CatalogGeneratorBuilder;

import java.io.File;

public class StarterTemplatesGeneratorBuilder implements CatalogGeneratorBuilder {

    private File outputDirectory;

    public StarterTemplatesGeneratorBuilder withOutputDirectory(File outputDirectory) {
        this.outputDirectory = outputDirectory;
        return this;
    }

    @Override
    public CatalogGenerator build() {
        return new StarterTemplatesGenerator(outputDirectory);
    }
}
