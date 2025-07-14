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

public class KaotoFunction {
    private String name;
    private String displayName;
    private String description;
    private String returnType;
    private boolean returnCollection = false;
    private List<KaotoFunctionArgument> arguments = List.of();

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getReturnType() {
        return returnType;
    }

    public void setReturnType(String returnType) {
        this.returnType = returnType;
    }

    public boolean isReturnCollection() {
        return returnCollection;
    }

    public void setReturnCollection(boolean returnCollection) {
        this.returnCollection = returnCollection;
    }

    public List<KaotoFunctionArgument> getArguments() {
        return arguments;
    }

    public void setArguments(List<KaotoFunctionArgument> arguments) {
        this.arguments = arguments;
    }
}
