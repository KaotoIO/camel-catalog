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
package io.kaoto.camelcatalog.model;

import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Stable identifiers for the Kaoto starter templates published in the
 * {@code starter-templates/} catalog. Each value serialises to the
 * hyphenated string key used as the key in
 * {@link CatalogDefinition#getCatalogs()} for the starter-templates index.
 */
public enum StarterTemplateId {
    CAMEL_ROUTE_YAML("camel-route-yaml"),
    CAMEL_ROUTE_XML("camel-route-xml"),
    PIPE_YAML("pipe-yaml"),
    KAMELET_SOURCE_YAML("kamelet-source-yaml"),
    KAMELET_ACTION_YAML("kamelet-action-yaml"),
    KAMELET_SINK_YAML("kamelet-sink-yaml"),
    CITRUS_YAML("citrus-yaml");

    private final String id;

    StarterTemplateId(String id) {
        this.id = id;
    }

    @JsonValue
    public String getId() {
        return id;
    }

    @Override
    public String toString() {
        return id;
    }
}
