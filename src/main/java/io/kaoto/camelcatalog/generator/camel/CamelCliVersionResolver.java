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
package io.kaoto.camelcatalog.generator.camel;

import io.kaoto.camelcatalog.model.CatalogRuntime;

import java.util.List;
import java.util.logging.Logger;

/**
 * Resolves the Camel CLI version to annotate in the catalog index.
 *
 * <p>The default CLI version is {@value #DEFAULT_CLI_VERSION}. For community (non-productized)
 * Spring Boot catalogs, the version is determined by comparing the Camel catalog version
 * against a configurable rule set. Productized (Red Hat) catalogs always use the default.
 */
public class CamelCliVersionResolver {
    private static final Logger LOGGER = Logger.getLogger(CamelCliVersionResolver.class.getName());

    static final String DEFAULT_CLI_VERSION = "4.20.0";

    private static final List<VersionRule> SPRING_BOOT_RULES = List.of(
            new VersionRule(4, 19, 0, DEFAULT_CLI_VERSION),
            new VersionRule(0, 0, 0, "4.18.2")
    );

    /**
     * Resolves the CLI version for a given catalog version and runtime.
     *
     * @param camelVersion the Camel catalog version string (e.g. "4.18.0", "4.20.0.redhat-00001")
     * @param runtime      the catalog runtime
     * @return the CLI version to embed in the index
     */
    public String resolve(String camelVersion, CatalogRuntime runtime) {
        if (camelVersion == null || runtime == null) {
            return DEFAULT_CLI_VERSION;
        }

        if (runtime == CatalogRuntime.SpringBoot && !isRedhat(camelVersion)) {
            return resolveFromRules(camelVersion);
        }

        return DEFAULT_CLI_VERSION;
    }

    private String resolveFromRules(String camelVersion) {
        int[] parsed = parseMajorMinorPatch(camelVersion);
        if (parsed.length == 0) {
            LOGGER.warning(() -> "Cannot parse version '" + camelVersion + "' for CLI version resolution; using default");
            return DEFAULT_CLI_VERSION;
        }

        for (VersionRule rule : SPRING_BOOT_RULES) {
            if (compareMajorMinorPatch(parsed, rule.minMajor, rule.minMinor, rule.minPatch) >= 0) {
                return rule.cliVersion;
            }
        }

        return DEFAULT_CLI_VERSION;
    }

    /**
     * Compares a parsed version triple against a threshold.
     *
     * @return negative if version &lt; threshold, 0 if equal, positive if version &gt; threshold
     */
    private static int compareMajorMinorPatch(int[] version, int major, int minor, int patch) {
        int cmp = Integer.compare(version[0], major);
        if (cmp != 0) return cmp;
        cmp = Integer.compare(version[1], minor);
        if (cmp != 0) return cmp;
        return Integer.compare(version[2], patch);
    }

    private static final int[] EMPTY = new int[0];

    static int[] parseMajorMinorPatch(String version) {
        if (version == null || version.isEmpty()) {
            return EMPTY;
        }
        String[] parts = version.split("\\.");
        if (parts.length < 3) {
            return EMPTY;
        }
        try {
            return new int[]{
                    Integer.parseInt(parts[0]),
                    Integer.parseInt(parts[1]),
                    Integer.parseInt(parts[2])
            };
        } catch (NumberFormatException e) {
            return EMPTY;
        }
    }

    private static boolean isRedhat(String version) {
        return version != null && version.contains("redhat-");
    }

    /**
     * A version rule that maps a minimum Camel version (inclusive) to a CLI version.
     * Rules are evaluated in order; the first rule whose minimum version is &lt;= the
     * catalog version wins. List rules from highest threshold to lowest.
     */
    record VersionRule(int minMajor, int minMinor, int minPatch, String cliVersion) {
    }
}
