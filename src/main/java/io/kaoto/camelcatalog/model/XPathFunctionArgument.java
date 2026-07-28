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
 * XPath-specific function argument extending {@link KaotoFunctionArgument} with XPath Data Model (XDM)
 * cardinality and optional default value. The base class's {@code type} carries the bare
 * XDM type (e.g. "xs:string") while {@code cardinality} holds the suffix separately,
 * allowing the UI to reason about optionality without parsing type strings.
 *
 * <p><strong>Type cardinality vs positional optionality.</strong>
 * {@code cardinality} encodes the XDM type annotation — what values the parameter accepts
 * (e.g. {@code "?"} means the value may be an empty sequence). The inherited
 * {@link KaotoFunctionArgument#getMinOccurs() minOccurs} /
 * {@link KaotoFunctionArgument#getMaxOccurs() maxOccurs} encode positional optionality — whether
 * the argument can be omitted from the function call entirely (determined by which
 * overloads exist). These two dimensions are independent:</p>
 * <ul>
 *   <li>{@code fn:abs($arg as xs:numeric?)} — {@code cardinality="?"}, {@code minOccurs=1}:
 *       the value may be an empty sequence, but the argument is positionally required.</li>
 *   <li>{@code fn:substring($source, $start, $length as xs:double)} — {@code cardinality=""},
 *       {@code minOccurs=0}: the type is exactly-one, but the argument is positionally optional
 *       (2-arg overload exists).</li>
 * </ul>
 */
public class XPathFunctionArgument extends KaotoFunctionArgument {
    private String cardinality;
    @JsonProperty("default")
    private String defaultValue;
    private String usage;

    /** @return XDM cardinality suffix: "" (exactly one), "?" (optional), "*" (zero or more), "+" (one or more) */
    public String getCardinality() {
        return cardinality;
    }

    /** @param cardinality XDM cardinality suffix */
    public void setCardinality(String cardinality) {
        this.cardinality = cardinality;
    }

    /** @return default value expression if the argument is optional, or null */
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
