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

/**
 * The fully resolved set of versions for one catalog entry.
 *
 * @param camelCatalogVersion    the Apache Camel version (what {@code catalog.getCatalogVersion()} reports)
 * @param runtimeProviderVersion the Camel runtime provider version (Camel Quarkus / Camel Spring Boot), null for Main
 * @param frameworkVersion       the underlying framework version (Quarkus platform / Spring Boot), null for Main
 * @param catalogDownloadVersion the version used to download the catalog artifact for this runtime
 */
public record ResolvedVersions(String camelCatalogVersion, String runtimeProviderVersion,
        String frameworkVersion, String catalogDownloadVersion) {
}
