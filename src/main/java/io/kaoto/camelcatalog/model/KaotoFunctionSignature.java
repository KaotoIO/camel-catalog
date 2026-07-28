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

import java.util.List;

/**
 * Represents a single arity overload of a function. While {@link KaotoFunction#getArguments()}
 * provides a merged view across all overloads for UI display, each signature captures the exact
 * argument types, return type, and XPath properties for one specific prototype. This distinction
 * matters for multi-arity functions like {@code fn:substring} (2 or 3 args) where each arity
 * may have a different return type or different properties.
 */
public class KaotoFunctionSignature {
    private String returnType;
    private List<KaotoFunctionSignatureArgument> arguments = List.of();
    private List<String> properties = List.of();

    /** @return full XPath Data Model (XDM) type string including cardinality (e.g. "xs:string?", "item()*") */
    public String getReturnType() {
        return returnType;
    }

    /** @param returnType full XDM type string including cardinality */
    public void setReturnType(String returnType) {
        this.returnType = returnType;
    }

    /** @return exact arguments for this particular arity */
    public List<KaotoFunctionSignatureArgument> getArguments() {
        return arguments;
    }

    /** @param arguments exact arguments for this particular arity */
    public void setArguments(List<KaotoFunctionSignatureArgument> arguments) {
        this.arguments = arguments;
    }

    /** @return XPath function properties for this arity (e.g. "deterministic", "context-independent", "focus-independent") */
    public List<String> getProperties() {
        return properties;
    }

    /** @param properties XPath function properties for this arity */
    public void setProperties(List<String> properties) {
        this.properties = properties;
    }
}
