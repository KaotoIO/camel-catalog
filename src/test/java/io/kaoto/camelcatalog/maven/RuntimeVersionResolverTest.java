package io.kaoto.camelcatalog.maven;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class RuntimeVersionResolverTest {

    private String fixture(String name) throws Exception {
        try (var in = getClass().getResourceAsStream("/version-resolution/" + name)) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    @Test
    void extractsCamelQuarkusVersionFromPlatformBom() throws Exception {
        String pom = fixture("quarkus-camel-bom-sample.xml");
        String result = RuntimeVersionResolver.extractCamelQuarkusVersionFromPlatformBom(pom);
        assertEquals("3.33.0.redhat-00007", result);
    }

    @Test
    void extractsCamelVersionFromCamelSpringBootDependencies() throws Exception {
        String pom = fixture("camel-spring-boot-dependencies-sample.xml");
        String result = RuntimeVersionResolver.extractCamelVersionFromCamelSpringBootDependencies(pom);
        assertEquals("4.18.3.redhat-00005", result);
    }

    @Test
    void resolvesCamelVersionOfRedhatCamelSpringBoot() throws Exception {
        String pom = fixture("camel-spring-boot-dependencies-sample.xml");
        java.util.List<String> requestedRepositories = new java.util.ArrayList<>();
        PomFetcher pomFetcher = (groupId, artifactId, version, repositories) -> {
            requestedRepositories.addAll(repositories);
            boolean dependenciesPom = "camel-spring-boot-dependencies".equals(artifactId)
                    && "4.18.3.redhat-00006".equals(version);
            return dependenciesPom ? pom : null;
        };

        String result = new RuntimeVersionResolver(pomFetcher, null, java.util.List.of())
                .resolveCamelVersionFromCamelSpringBoot("4.18.3.redhat-00006");

        assertEquals("4.18.3.redhat-00005", result);
        org.junit.jupiter.api.Assertions.assertTrue(requestedRepositories.contains(RuntimeVersionResolver.REDHAT_GA));
    }

    @Test
    void fallsBackToCamelSpringBootVersionWhenDependenciesPomIsMissing() {
        PomFetcher pomFetcher = (groupId, artifactId, version, repositories) -> null;

        String result = new RuntimeVersionResolver(pomFetcher, null, java.util.List.of())
                .resolveCamelVersionFromCamelSpringBoot("4.23.0");

        assertEquals("4.23.0", result);
    }

    @Test
    void returnsNullWhenCatalogDependencyMissing() {
        String pom = "<project><dependencyManagement><dependencies></dependencies>"
                + "</dependencyManagement></project>";
        assertNull(RuntimeVersionResolver.extractCamelQuarkusVersionFromPlatformBom(pom));
    }

    @Test
    void infersCommunityGroupId() {
        assertEquals("io.quarkus.platform", RuntimeVersionResolver.quarkusPlatformGroupId("3.28.0"));
        org.junit.jupiter.api.Assertions.assertFalse(RuntimeVersionResolver.isRedhat("3.28.0"));
    }

    @Test
    void infersRedhatGroupId() {
        assertEquals("com.redhat.quarkus.platform",
                RuntimeVersionResolver.quarkusPlatformGroupId("3.33.1.redhat-00006"));
        org.junit.jupiter.api.Assertions.assertTrue(RuntimeVersionResolver.isRedhat("3.33.1.redhat-00006"));
    }

    @Test
    void buildsRepoListForRedhatWithCustomMirror() {
        java.util.List<String> repos = RuntimeVersionResolver.repositoriesFor(
                "3.33.1.redhat-00006", java.util.List.of("https://nexus.corp/repo"));
        assertEquals(java.util.List.of(
                "https://repo1.maven.org/maven2",
                "https://maven.repository.redhat.com/ga",
                "https://nexus.corp/repo"), repos);
    }

    @Test
    void buildsRepoListForCommunity() {
        java.util.List<String> repos = RuntimeVersionResolver.repositoriesFor("3.28.0", java.util.List.of());
        assertEquals(java.util.List.of("https://repo1.maven.org/maven2"), repos);
    }

    @Test
    void findsVersionInResolvedGavs() {
        java.util.List<org.apache.camel.tooling.maven.MavenGav> gavs = java.util.List.of(
                org.apache.camel.tooling.maven.MavenGav.parseGav(
                        "org.apache.camel.springboot:camel-core-starter:4.20.0"),
                org.apache.camel.tooling.maven.MavenGav.parseGav(
                        "org.springframework.boot:spring-boot-starter:3.5.6"));

        String result = RuntimeVersionResolver.findVersion(gavs,
                "org.springframework.boot", "spring-boot-starter");

        assertEquals("3.5.6", result);
    }

    @Test
    void findVersionReturnsNullWhenAbsent() {
        java.util.List<org.apache.camel.tooling.maven.MavenGav> gavs = java.util.List.of(
                org.apache.camel.tooling.maven.MavenGav.parseGav("org.apache.camel:camel-core:4.20.0"));
        assertNull(RuntimeVersionResolver.findVersion(gavs, "org.springframework.boot", "spring-boot-starter"));
    }

    @Test
    void infersRedhatForSpQualifiedPlatformVersion() {
        org.junit.jupiter.api.Assertions.assertTrue(
                RuntimeVersionResolver.isRedhat("3.27.3.SP2-redhat-00001"));
        org.junit.jupiter.api.Assertions.assertTrue(
                RuntimeVersionResolver.isRedhat("3.20.6.SP1-redhat-00002"));
        assertEquals("com.redhat.quarkus.platform",
                RuntimeVersionResolver.quarkusPlatformGroupId("3.27.3.SP2-redhat-00001"));
        // community SP-style version (no redhat) must still be community
        org.junit.jupiter.api.Assertions.assertFalse(RuntimeVersionResolver.isRedhat("3.20.6.1"));
    }
}
