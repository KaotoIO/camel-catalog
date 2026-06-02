# Catalog versions

The catalog versions the generator builds are declared in [`index.js`](../index.js)
(`CATALOGS` + `KAMELETS_VERSION`). This document explains where those versions come
from, how provenance is resolved, and how to verify them against the Maven registries.

## Input semantics per runtime

Each list holds the value `camel run` consumes for that runtime — **not** necessarily
the Apache Camel version:

| Runtime      | List entry is…                          | Notes |
|--------------|-----------------------------------------|-------|
| `Main`       | the Apache Camel version                | Used as-is. |
| `Quarkus`    | the **Quarkus platform** version        | What `camel run --quarkus-version` takes. The camel-quarkus and Apache Camel versions are derived from it (see mapping below). |
| `SpringBoot` | the Camel Spring Boot version           | Tracks the Apache Camel version 1:1; the Spring Boot framework version is resolved transitively. |
| `Citrus`     | the Citrus catalog version              | Used as-is. |

## Provenance: community vs. Red Hat

Provenance is **inferred per version** from the `redhat-` marker — no per-version flags:

- **Community** (no `redhat-`) → groupId `io.quarkus.platform`, repositories: Maven Central.
- **Red Hat / productized** (contains `redhat-`) → groupId `com.redhat.quarkus.platform`,
  repositories: Maven Central + Red Hat GA.

A single build run can therefore mix community and productized versions in one list.
Extra corporate-mirror repositories can be appended via `CATALOG_EXTRA_REPOS` (passed to
the generator as `--repos`); they are additive and never replace the inferred ones.

> The `redhat-` marker is matched anywhere in the version string, so every productized
> version format is detected.

## Quarkus platform → camel-quarkus → Apache Camel

A Quarkus platform version's `quarkus-camel-bom` pins the `camel-quarkus-catalog` version
(the runtime provider), which in turn resolves to an Apache Camel version. The mappings
below were verified against the registries:

| Platform version (input)  | camel-quarkus (provider) | Apache Camel          |
|---------------------------|--------------------------|-----------------------|
| `3.35.4`                  | `3.35.0`                 | `4.20.0`              |
| `3.33.2`                  | `3.33.1`                 | `4.18.2`              |
| `3.27.4`                  | `3.27.4`                 | `4.14.7`              |
| `3.20.6.1`                | `3.20.4`                 | `4.10.8`              |
| `3.33.1.redhat-00006`     | `3.33.0.redhat-00007`    | `4.18.1.redhat-00020` |
| `3.27.3.redhat-00003`     | `3.27.1.redhat-00004`    | `4.14.2.redhat-00020` |
| `3.20.6.redhat-00004`     | `3.20.0.redhat-00011`    | `4.10.3.redhat-00034` |

To re-verify a row, open that platform's `quarkus-camel-bom` POM inside the matching
registry directory listed below and read the managed
`org.apache.camel.quarkus:camel-quarkus-catalog` version — e.g.
<https://repo1.maven.org/maven2/io/quarkus/platform/quarkus-camel-bom/3.35.4/quarkus-camel-bom-3.35.4.pom>.

## Listing available versions

With Camel JBang:

```bash
camel version list --fresh --runtime=main         # add --repo=redhat for productized
camel version list --fresh --runtime=quarkus
camel version list --fresh --runtime=spring-boot
```

Or browse the registry directories below (append `maven-metadata.xml` to any of them for
the raw version list):

| Artifact                                   | Community (Maven Central)                                                                      | Red Hat (GA)                                                                                          |
|--------------------------------------------|-----------------------------------------------------------------------------------------------|------------------------------------------------------------------------------------------------------|
| Camel catalog (`Main`)                     | https://repo1.maven.org/maven2/org/apache/camel/camel-catalog/                                 | https://maven.repository.redhat.com/ga/org/apache/camel/camel-catalog/                                |
| Quarkus platform (`quarkus-camel-bom`)     | https://repo1.maven.org/maven2/io/quarkus/platform/quarkus-camel-bom/                          | https://maven.repository.redhat.com/ga/com/redhat/quarkus/platform/quarkus-camel-bom/                 |
| Spring Boot provider                       | https://repo1.maven.org/maven2/org/apache/camel/springboot/camel-catalog-provider-springboot/  | https://maven.repository.redhat.com/ga/org/apache/camel/springboot/camel-catalog-provider-springboot/ |
| Camel Spring Boot starter (framework src)  | https://repo1.maven.org/maven2/org/apache/camel/springboot/camel-core-starter/                 | https://maven.repository.redhat.com/ga/org/apache/camel/springboot/camel-core-starter/                |
| Kamelets                                   | https://repo1.maven.org/maven2/org/apache/camel/kamelets/camel-kamelets/                        | https://maven.repository.redhat.com/ga/org/apache/camel/kamelets/camel-kamelets/                      |
| Citrus catalog schema                      | https://repo1.maven.org/maven2/org/citrusframework/citrus-catalog-schema/                       | —                                                                                                    |

## Updating versions

1. Edit the relevant list in [`index.js`](../index.js) (and `KAMELETS_VERSION` if needed).
2. Update this document's mapping table when adding/removing Quarkus platform versions.
3. Run `yarn build` and commit the regenerated `catalog/`.
