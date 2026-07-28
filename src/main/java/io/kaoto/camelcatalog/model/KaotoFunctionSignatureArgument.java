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

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Argument within a specific function signature overload. Unlike {@link XPathFunctionArgument},
 * which decomposes the type into base type, cardinality, and min/max occurs for UI consumption,
 * this class preserves the full XPath Data Model (XDM) type string (e.g. "xs:string?") as it appears in the W3C
 * function catalog prototype.
 */
public class KaotoFunctionSignatureArgument {
    private String name;
    private String type;
    @JsonProperty("default")
    private String defaultValue;
    private String usage;

    /** @return the argument identifier */
    public String getName() {
        return name;
    }

    /** @param name the argument identifier */
    public void setName(String name) {
        this.name = name;
    }

    /** @return full XDM type string with cardinality (e.g. "xs:string?", "item()*") */
    public String getType() {
        return type;
    }

    /** @param type full XDM type string with cardinality */
    public void setType(String type) {
        this.type = type;
    }

    /** @return default value expression, or null */
    public String getDefaultValue() {
        return defaultValue;
    }

    /** @param defaultValue default value expression */
    public void setDefaultValue(String defaultValue) {
        this.defaultValue = defaultValue;
    }

    /** @return usage hint from the W3C spec, or null */
    public String getUsage() {
        return usage;
    }

    /** @param usage usage hint from the W3C spec */
    public void setUsage(String usage) {
        this.usage = usage;
    }
}
