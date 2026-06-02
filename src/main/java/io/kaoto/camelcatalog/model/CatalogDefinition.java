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

import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class CatalogDefinition {
    private String name;
    private String version;
    private String executorVersion;
    private String camelCatalogVersion;
    private String runtimeProviderVersion;
    private String frameworkVersion;
    private CatalogRuntime runtime;
    private final Map<String, CatalogDefinitionEntry> catalogs = new HashMap<>();
    private final Map<String, CatalogDefinitionEntry> schemas = new HashMap<>();
    private String fileName;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getExecutorVersion() {
        return executorVersion;
    }

    public void setExecutorVersion(String executorVersion) {
        this.executorVersion = executorVersion;
    }

    public String getCamelCatalogVersion() {
        return camelCatalogVersion;
    }

    public void setCamelCatalogVersion(String camelCatalogVersion) {
        this.camelCatalogVersion = camelCatalogVersion;
    }

    public String getRuntimeProviderVersion() {
        return runtimeProviderVersion;
    }

    public void setRuntimeProviderVersion(String runtimeProviderVersion) {
        this.runtimeProviderVersion = runtimeProviderVersion;
    }

    public String getFrameworkVersion() {
        return frameworkVersion;
    }

    public void setFrameworkVersion(String frameworkVersion) {
        this.frameworkVersion = frameworkVersion;
    }

    public CatalogRuntime getRuntime() {
        return runtime;
    }

    public void setRuntime(CatalogRuntime runtime) {
        this.runtime = runtime;
    }

    public Map<String, CatalogDefinitionEntry> getCatalogs() {
        return catalogs;
    }

    public Map<String, CatalogDefinitionEntry> getSchemas() {
        return schemas;
    }

    @JsonIgnore
    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }
}
