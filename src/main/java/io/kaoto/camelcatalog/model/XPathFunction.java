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
 * XPath/XSLT-specific function model extending {@link KaotoFunction} with XPath namespace
 * prefix, XPath Data Model (XDM) return cardinality, and per-arity signature overloads.
 *
 * <p>The base class's {@code arguments} field carries a merged view across all overloads
 * (union of parameters, with optional markers for arity-specific ones) intended for UI display.
 * The {@code signatures} field captures each overload's exact specifications separately.</p>
 */
public class XPathFunction extends KaotoFunction<XPathFunctionArgument> {
    private String prefix;
    private String returnCardinality;
    private List<KaotoFunctionSignature> signatures = List.of();

    /** @return XPath namespace prefix (e.g. "fn", "math", "map", "array") */
    public String getPrefix() {
        return prefix;
    }

    /** @param prefix XPath namespace prefix (e.g. "fn", "math", "map", "array") */
    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }

    /** @return XDM cardinality suffix for the return type: "" (exactly one), "?" (optional), "*" (zero or more), "+" (one or more) */
    public String getReturnCardinality() {
        return returnCardinality;
    }

    /** @param returnCardinality XDM cardinality suffix for the return type */
    public void setReturnCardinality(String returnCardinality) {
        this.returnCardinality = returnCardinality;
    }

    /** @return per-arity overload definitions, each with exact argument types, return type, and XPath properties */
    public List<KaotoFunctionSignature> getSignatures() {
        return signatures;
    }

    /** @param signatures per-arity overload definitions */
    public void setSignatures(List<KaotoFunctionSignature> signatures) {
        this.signatures = signatures;
    }
}
