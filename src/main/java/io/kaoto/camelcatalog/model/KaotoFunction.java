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

import java.util.List;

/**
 * Base model for function entries in the Kaoto catalog. Generic over argument type
 * to allow language-specific extensions (e.g. XPath arguments carry XPath Data Model (XDM) cardinality
 * that Camel language arguments do not).
 *
 * @param <A> the argument type, must extend {@link KaotoFunctionArgument}
 */
public class KaotoFunction<A extends KaotoFunctionArgument> {
    private String name;
    private String displayName;
    private String description;
    private String returnType;
    private boolean returnCollection = false;
    private List<A> arguments = List.of();

    /** @return the function identifier used as the catalog key */
    public String getName() {
        return name;
    }

    /** @param name the function identifier used as the catalog key */
    public void setName(String name) {
        this.name = name;
    }

    /** @return human-readable label for UI display */
    public String getDisplayName() {
        return displayName;
    }

    /** @param displayName human-readable label for UI display */
    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    /** @return summary of the function's behavior */
    public String getDescription() {
        return description;
    }

    /** @param description summary of the function's behavior */
    public void setDescription(String description) {
        this.description = description;
    }

    /** @return the base return type (e.g. "xs:string", "java.lang.String") */
    public String getReturnType() {
        return returnType;
    }

    /** @param returnType the base return type (e.g. "xs:string", "java.lang.String") */
    public void setReturnType(String returnType) {
        this.returnType = returnType;
    }

    /** @return whether the function returns a collection */
    public boolean isReturnCollection() {
        return returnCollection;
    }

    /** @param returnCollection whether the function returns a collection */
    public void setReturnCollection(boolean returnCollection) {
        this.returnCollection = returnCollection;
    }

    /** @return the function's formal parameters, merged across all overloads for UI display */
    public List<A> getArguments() {
        return arguments;
    }

    /** @param arguments the function's formal parameters */
    public void setArguments(List<A> arguments) {
        this.arguments = arguments;
    }
}
