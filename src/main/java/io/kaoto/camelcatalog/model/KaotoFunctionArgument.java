/*
 * Copyright (C) 2025 Red Hat, Inc.
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

/**
 * Base model for function argument entries. Captures type information and occurrence
 * constraints shared across all function catalogs (Camel languages, XPath, XSLT).
 */
public class KaotoFunctionArgument {
    private String name;
    private String type;
    private String displayName;
    private String description;
    private Integer minOccurs;
    private Integer maxOccurs;

    /** @return the argument identifier */
    public String getName() {
        return name;
    }

    /** @param name the argument identifier */
    public void setName(String name) {
        this.name = name;
    }

    /** @return the argument's base data type */
    public String getType() {
        return type;
    }

    /** @param type the argument's base data type */
    public void setType(String type) {
        this.type = type;
    }

    /** @return human-readable label for UI display */
    public String getDisplayName() {
        return displayName;
    }

    /** @param displayName human-readable label for UI display */
    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    /** @return summary of the argument's purpose */
    public String getDescription() {
        return description;
    }

    /** @param description summary of the argument's purpose */
    public void setDescription(String description) {
        this.description = description;
    }

    /** @return minimum number of occurrences, or null if unspecified */
    public Integer getMinOccurs() {
        return minOccurs;
    }

    /** @param minOccurs minimum number of occurrences */
    public void setMinOccurs(Integer minOccurs) {
        this.minOccurs = minOccurs;
    }

    /** @return maximum number of occurrences, or null if unspecified ({@code Integer.MAX_VALUE} for unbounded) */
    public Integer getMaxOccurs() {
        return maxOccurs;
    }

    /** @param maxOccurs maximum number of occurrences ({@code Integer.MAX_VALUE} for unbounded) */
    public void setMaxOccurs(Integer maxOccurs) {
        this.maxOccurs = maxOccurs;
    }

}
