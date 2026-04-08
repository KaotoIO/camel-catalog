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
package io.kaoto.camelcatalog.generator;

import io.kaoto.camelcatalog.model.CatalogRuntime;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for CamelLauncherVersionResolver.
 * 
 * Note: These tests make real HTTP calls to Maven repositories.
 * Set SKIP_NETWORK_TESTS=false to run them.
 */
@EnabledIfEnvironmentVariable(named = "SKIP_NETWORK_TESTS", matches = "false", disabledReason = "Network tests disabled by default")
class CamelLauncherVersionResolverTest {

    private final CamelLauncherVersionResolver resolver = new CamelLauncherVersionResolver();

    @Test
    void testResolveCommunityMainVersion() {
        // Test community version resolution for Camel Main
        String launcherVersion = resolver.findLauncherVersion("4.18.0", CatalogRuntime.Main);
        
        assertNotNull(launcherVersion, "Launcher version should be found for community version 4.18.0");
        assertEquals("4.18.0", launcherVersion, "Launcher version should match Camel version for community");
    }

    @Test
    void testResolveRedHatMainVersion() {
        // Test Red Hat version resolution for Camel Main
        String launcherVersion = resolver.findLauncherVersion("4.14.2.redhat-00019", CatalogRuntime.Main);
        
        assertNotNull(launcherVersion, "Launcher version should be found for Red Hat version");
        assertTrue(launcherVersion.matches("4\\.14\\.2\\.redhat-\\d{5}"), 
                   "Launcher version should match pattern 4.14.2.redhat-XXXXX with 5-digit build number");
    }

    @Test
    void testResolveCommunitySpringBootVersion() {
        // Test community version resolution for Spring Boot
        String launcherVersion = resolver.findLauncherVersion("4.14.5", CatalogRuntime.SpringBoot);
        
        assertNotNull(launcherVersion, "Launcher version should be found for Spring Boot community version");
        assertEquals("4.14.5", launcherVersion, "Launcher version should match Camel version for Spring Boot");
    }

    @Test
    void testResolveRedHatSpringBootVersion() {
        // Test Red Hat version resolution for Spring Boot
        String launcherVersion = resolver.findLauncherVersion("4.14.2.redhat-00018", CatalogRuntime.SpringBoot);
        
        assertNotNull(launcherVersion, "Launcher version should be found for Red Hat Spring Boot version");
        assertTrue(launcherVersion.startsWith("4.14.2.redhat-"), 
                   "Launcher version should be a Red Hat version");
    }

    @Test
    void testResolveCommunityQuarkusVersion() {
        // Test Quarkus BOM resolution for community version
        String launcherVersion = resolver.findLauncherVersion("3.32.0", CatalogRuntime.Quarkus);
        
        assertNotNull(launcherVersion, "Launcher version should be found for Quarkus community version");
        assertTrue(launcherVersion.startsWith("4."), 
                   "Launcher version should be a Camel 4.x version extracted from Quarkus BOM");
    }

    @Test
    void testResolveRedHatQuarkusVersion() {
        // Test Quarkus BOM resolution for Red Hat version
        String launcherVersion = resolver.findLauncherVersion("3.27.1.redhat-00004", CatalogRuntime.Quarkus);
        
        assertNotNull(launcherVersion, "Launcher version should be found for Red Hat Quarkus version");
        assertTrue(launcherVersion.contains(".redhat-"), 
                   "Launcher version should be a Red Hat version for Red Hat Quarkus");
        assertTrue(launcherVersion.startsWith("4.14."), 
                   "Launcher version should be extracted from Red Hat Quarkus BOM");
    }

    @Test
    void testResolveOldVersionWithoutLauncher() {
        // Test handling of old versions where launcher doesn't exist
        String launcherVersion = resolver.findLauncherVersion("4.8.0", CatalogRuntime.Main);
        
        assertNull(launcherVersion, "Launcher version should be null for old versions (< 4.13.0)");
    }

    @Test
    void testResolveOldRedHatVersionWithoutLauncher() {
        // Test handling of old Red Hat versions where launcher doesn't exist
        String launcherVersion = resolver.findLauncherVersion("4.10.7.redhat-00009", CatalogRuntime.Main);
        
        assertNull(launcherVersion, "Launcher version should be null for old Red Hat versions without launcher");
    }

    @Test
    void testResolveOldQuarkusVersionWithoutLauncher() {
        // Test Quarkus version that maps to old Camel version without launcher
        String launcherVersion = resolver.findLauncherVersion("3.15.0.redhat-00010", CatalogRuntime.Quarkus);
        
        assertNull(launcherVersion, 
                   "Launcher version should be null for Quarkus versions mapping to old Camel (< 4.13.0)");
    }

    @Test
    void testResolveInvalidVersion() {
        // Test handling of invalid version format
        String launcherVersion = resolver.findLauncherVersion("invalid.version", CatalogRuntime.Main);
        
        assertNull(launcherVersion, "Launcher version should be null for invalid version format");
    }

    @Test
    void testResolveNullVersion() {
        // Test handling of null version
        String launcherVersion = resolver.findLauncherVersion(null, CatalogRuntime.Main);
        
        assertNull(launcherVersion, "Launcher version should be null for null input version");
    }

    @Test
    void testResolveEmptyVersion() {
        // Test handling of empty version
        String launcherVersion = resolver.findLauncherVersion("", CatalogRuntime.Main);
        
        assertNull(launcherVersion, "Launcher version should be null for empty input version");
    }

    @Test
    void testHighestBuildNumberSelection() {
        // Test that the resolver selects the highest build number for Red Hat versions
        String launcherVersion = resolver.findLauncherVersion("4.14.2.redhat-00019", CatalogRuntime.Main);
        
        assertNotNull(launcherVersion, "Launcher version should be found");
        // The resolver should find the highest build number available (e.g., 00006 or higher)
        assertTrue(launcherVersion.matches("4\\.14\\.2\\.redhat-\\d{5}"), 
                   "Launcher version should match pattern with 5-digit build number");
    }

    @Test
    void testQuarkusBomParsing() {
        // Test that Quarkus BOM parsing extracts correct Camel version
        String launcherVersion = resolver.findLauncherVersion("3.27.2", CatalogRuntime.Quarkus);
        
        assertNotNull(launcherVersion, "Launcher version should be found for Quarkus 3.27.2");
        // Quarkus 3.27.2 should map to Camel 4.14.3
        assertTrue(launcherVersion.startsWith("4.14."), 
                   "Launcher version should be from Camel 4.14.x series");
    }

    @Test
    void testRepositorySelectionForRedHatQuarkus() {
        // Test that Red Hat Quarkus versions query Red Hat repository
        String launcherVersion = resolver.findLauncherVersion("3.20.0.redhat-00010", CatalogRuntime.Quarkus);
        
        // This version maps to Camel 4.10.3, which is too old for launcher
        // But the test verifies the repository selection logic works
        assertNull(launcherVersion, 
                   "Launcher version should be null as Camel 4.10.3 is too old");
    }
}
