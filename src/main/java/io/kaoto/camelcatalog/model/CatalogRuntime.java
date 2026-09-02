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
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
    
public enum CatalogRuntime {
    MAIN("Main", "Main"),
    QUARKUS("Quarkus", "Quarkus"),
    SPRING_BOOT("SpringBoot", "Spring Boot"),
    CITRUS("Citrus", "Citrus"),
    XSLT("XSLT", "XSLT"),
    STARTER_TEMPLATES("StarterTemplates", "Starter Templates");

    /**
     * Stable identifier written to the generated catalogs and used to build the
     * runtime folder names. It is part of the catalog contract, so it must not
     * follow renames of the enum constants.
     */
    private final String id;
    private final String label;

    CatalogRuntime(String id, String label) {
        this.id = id;
        this.label = label;
    }

    @JsonValue
    public String getId() {
        return id;
    }

    public String getLabel() {
        return label;
    }

    @JsonCreator
    public static CatalogRuntime fromString(String name) {
        for (CatalogRuntime runtime : CatalogRuntime.values()) {
            if (runtime.id.equalsIgnoreCase(name)
                    || runtime.name().equalsIgnoreCase(name)
                    || runtime.name().replace("_", "").equalsIgnoreCase(name)) {
                return runtime;
            }
        }

        throw new IllegalArgumentException("No enum found with name: " + name);
    }

    public String getRuntimeFolder() {
        return switch (this) {
            case MAIN, QUARKUS, SPRING_BOOT -> "camel-" + id.toLowerCase();
            case CITRUS, XSLT -> name().toLowerCase();
            case STARTER_TEMPLATES -> "starter-templates";
        };
    }
}
