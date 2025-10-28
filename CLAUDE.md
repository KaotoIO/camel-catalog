# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

This is the Kaoto Camel Catalog Generator - a hybrid Java/TypeScript project that generates comprehensive Apache Camel catalogs for the Kaoto visual integration editor. The project uses Maven to build a Java-based catalog generator JAR, then uses Node.js/Yarn scripts to execute the generator and produce TypeScript type definitions from JSON schemas.

## Build Commands

### Full Build
```bash
yarn build
```
This runs the complete build pipeline:
1. Cleans dist and catalog directories
2. Builds the Maven JAR (`./mvnw clean package`)
3. Executes the catalog generator (Java JAR)
4. Generates TypeScript type definitions from JSON schemas
5. Copies generated catalog to the root `catalog/` folder

### Java-Only Build
```bash
yarn build:mvn
# or
./mvnw clean package
```
Builds the Java catalog generator JAR without running the full pipeline.

### Catalog Generation Only
```bash
yarn build:catalog
```
Runs the Java JAR to generate catalogs (requires JAR to be built first).

### TypeScript Type Generation
```bash
yarn build:ts
```
Generates TypeScript `.d.ts` files from the JSON schemas in `dist/camel-catalog/`.

### Linting
```bash
yarn lint          # Check for issues
yarn lint:fix      # Auto-fix issues
```

### Testing
```bash
./mvnw test                    # Run all tests
./mvnw test -Dtest=ClassName   # Run specific test class
```

## Architecture

### Two-Phase Build System

The build process is split into two distinct phases:

1. **Java Phase**: Maven builds a shaded JAR containing the catalog generator with all dependencies. This JAR is a command-line tool that:
   - Loads Apache Camel catalog metadata from Maven dependencies
   - Processes Kamelets (Camel connectors) from YAML definitions
   - Generates JSON schemas for Camel YAML DSL, CRDs, and XSD schemas
   - Produces a catalog index file referencing all generated artifacts
   - Main entry point: `io.kaoto.camelcatalog.Main`

2. **Node.js Phase**: Yarn scripts orchestrate post-processing:
   - Execute the Java JAR with configured Camel/Kamelets versions (via `scripts/build-catalogs.mjs`)
   - Generate TypeScript type definitions from JSON schemas (via `scripts/json-schema-to-typescript.mjs`)
   - Copy artifacts to the publishable `dist/` folder

### Catalog Generation Flow

The catalog generator (`CatalogGenerator.java`) follows this pipeline:
1. Load Camel catalog, Kamelets, Kubernetes schemas, and Camel-K CRDs
2. Process Camel YAML DSL schema (JSON Schema Draft-07)
3. Generate aggregated catalog JSON files for components, EIPs, and data formats
4. Process and aggregate Kamelet definitions
5. Generate schemas (YAML DSL, CRDs, XSD)
6. Create catalog index file with hash-based filenames for cache-busting

### Maven Artifact Loading and Resource Management

The project uses a dynamic Maven artifact resolution system to load versioned Camel catalogs at runtime without requiring them to be compile-time dependencies. This allows generating catalogs for multiple Camel versions from a single JAR.

#### KaotoMavenVersionManager

`KaotoMavenVersionManager` extends Apache Camel's `MavenVersionManager` and provides:

- **Dynamic Artifact Resolution**: Downloads Maven artifacts (JARs) from remote repositories at runtime
- **Transitive Dependency Support**: Unlike the base class, this resolves transitive dependencies needed for Quarkus and Spring Boot runtime providers
- **Custom ClassLoader**: Uses `KaotoOpenURLClassLoader` to dynamically add downloaded JARs to the classpath
- **Repository Configuration**: Automatically configures Maven Central and Red Hat GA repositories based on version string (detects `-redhat` suffix)
- **Version-Aware Resource Loading**: Ensures resources are loaded from the correct version when multiple Camel versions are on the classpath

Example artifact resolution (from CamelCatalogVersionLoader.java:252):
```java
// Downloads org.apache.camel.quarkus:camel-quarkus-catalog:3.27.0 and all dependencies
camelCatalog.loadRuntimeProviderVersion("org.apache.camel.quarkus",
                                        "camel-quarkus-catalog",
                                        "3.27.0");
```

This internally calls `KaotoMavenVersionManager.resolve()` which:
1. Uses `MavenDownloader` to fetch the artifact from configured repositories
2. Resolves transitive dependencies
3. Adds all JAR files to the custom URLClassLoader
4. Makes resources from those JARs available for loading

#### ResourceLoader

`ResourceLoader` provides utilities to extract resources from the dynamically loaded JARs:

- **Single Resource Loading** (`getResourceAsString`): Loads individual files like `camel-yaml-dsl.json` from the classpath
- **Bulk Folder Loading** (`loadResourcesFromFolderAsString`): Loads all files with a specific suffix from a folder in JARs or filesystem
  - Example: Load all `.kamelet.yaml` files from the `kamelets/` folder inside the kamelets JAR
  - Handles both JAR protocol (`jar:file:`) and file protocol (`file:`) resources
  - Returns a map of filename → content for processing

**How ResourceLoader handles JAR vs filesystem**:
- For JAR resources: Uses `JarURLConnection` to iterate `JarEntry` objects and extract matching files
- For file resources: Uses `Files.walk()` to traverse directory structure (useful during development)

#### CamelCatalogVersionLoader

`CamelCatalogVersionLoader` orchestrates loading all Camel artifacts for a specific version and runtime:

**Loading sequence** (typically called from CatalogGenerator.java):

1. **Load Camel Catalog** (`loadCamelCatalog`):
   - Determines Maven coordinates based on runtime (Main/Quarkus/SpringBoot)
   - Downloads catalog artifact: `camel-catalog`, `camel-quarkus-catalog`, or `camel-catalog-provider-springboot`
   - Configures runtime provider (QuarkusRuntimeProvider, SpringBootRuntimeProvider, etc.)

2. **Load Camel YAML DSL** (`loadCamelYamlDsl`):
   - Downloads YAML DSL artifact: `camel-yaml-dsl`, `camel-quarkus-yaml-dsl`, or `camel-yaml-dsl-starter`
   - Extracts `camel-yaml-dsl.json` schema file using version-aware loading

3. **Load Kamelets** (`loadKamelets`):
   - Downloads `org.apache.camel.kamelets:camel-kamelets:VERSION`
   - Uses ResourceLoader to bulk-load all `.kamelet.yaml` files from the `kamelets/` folder

4. **Load Kamelet Boundaries** (`loadKameletBoundaries`):
   - Loads built-in kamelet boundary definitions from local resources

5. **Load Kubernetes Schema** (`loadKubernetesSchema`):
   - Fetches Kubernetes OpenAPI schema directly from GitHub

6. **Load Camel-K CRDs** (`loadCamelKCRDs`):
   - Downloads `org.apache.camel.k:camel-k-crds:VERSION`
   - Extracts specific CRD YAML files (Integration, Pipe, Kamelet, etc.)

7. **Load Local Schemas & Patterns**:
   - Loads JSON schemas and Kaoto-specific patterns from local classpath resources

### Complete Flow: Version to Catalog Generation

Here's the complete flow from specifying a Camel version to generating Kaoto catalogs:

1. **User invokes build** (`yarn build`)
   - Executes `scripts/build-catalogs.mjs`
   - Calls Java JAR with version parameters from `index.js`

2. **Java Main class** (`io.kaoto.camelcatalog.Main`)
   - Parses CLI arguments (catalog version, kamelets version, output directory, etc.)
   - Creates `GenerateCommand` with configuration

3. **GenerateCommand.run()**
   - For each catalog version configuration:
     - Creates `CatalogGeneratorBuilder`
     - Specifies runtime (Main/Quarkus/SpringBoot) and versions

4. **CatalogGeneratorBuilder.build()**
   - Instantiates `CamelCatalogVersionLoader` with runtime
   - Creates `KaotoMavenVersionManager` with custom URLClassLoader
   - Creates `ResourceLoader` wrapping the version manager
   - Returns configured `CatalogGenerator`

5. **CatalogGenerator.generate()**
   - Calls `CamelCatalogVersionLoader` methods to load all artifacts:
     - Downloads Camel catalog JARs via Maven
     - Downloads YAML DSL JARs
     - Downloads Kamelets JAR
     - Downloads CRDs JAR
     - Fetches Kubernetes schema
   - All resources now available in custom classloader

6. **Process and aggregate** (via processors):
   - `CamelCatalogProcessor`: Extracts component/EIP metadata from loaded catalog
   - `CamelYamlDslSchemaProcessor`: Processes YAML DSL JSON schema
   - `KameletProcessor`: Parses and aggregates all Kamelet YAML definitions
   - Various generators create output JSON files

7. **Write output**
   - Generates catalog JSON files (components, EIPs, patterns, etc.)
   - Generates schemas (YAML DSL, CRDs, XSD)
   - Creates catalog index with hash-based filenames
   - Writes to `dist/camel-catalog/<runtime>/<version>/`

8. **Post-processing** (back in Node.js)
   - `scripts/json-schema-to-typescript.mjs` generates TypeScript definitions
   - Files copied to `catalog/` for npm distribution

**Key insight**: This architecture allows the generator to produce catalogs for any Camel version without modifying the JAR's compile-time dependencies. The version manager downloads the exact artifacts for each requested version, loads them into an isolated classloader, and extracts all necessary metadata dynamically.

### Key Java Packages

- `io.kaoto.camelcatalog.generator`: Core catalog generation logic
  - `CatalogGenerator`: Main orchestration class
  - `CamelCatalogProcessor`: Processes Camel components and patterns
  - `CamelYamlDslSchemaProcessor`: Handles YAML DSL schema processing
  - `KameletProcessor`: Processes Kamelet YAML definitions
- `io.kaoto.camelcatalog.generators`: Specific generators for different catalog types
  - `ComponentGenerator`, `EIPGenerator`: Generate component and EIP catalogs
  - `CRDGenerator`: Generates Kubernetes CRD schemas
  - `SchemasGenerator`: Aggregates all schema generation
- `io.kaoto.camelcatalog.model`: Data model classes (serialized to JSON, TypeScript types auto-generated)
- `io.kaoto.camelcatalog.maven`: Maven catalog version loading and resource management
  - `KaotoMavenVersionManager`: Downloads and manages versioned Maven artifacts
  - `ResourceLoader`: Loads resources from JARs and local filesystem
  - `CamelCatalogVersionLoader`: Coordinates loading of all Camel artifacts for a specific version

### Version Configuration

Camel runtime versions are defined in `index.js`:
- `CATALOGS.Main`: Apache Camel versions (community + Red Hat)
- `CATALOGS.Quarkus`: Camel Quarkus versions
- `CATALOGS.SpringBoot`: Camel Spring Boot versions
- `KAMELETS_VERSION`: Kamelets version

To update Camel versions:
1. Modify `pom.xml` properties (`version.camel`, `version.camel.quarkus`, etc.)
2. Update `index.js` CATALOGS arrays if adding/removing runtime versions
3. Run `yarn build` to regenerate all catalogs

## Development Workflow

### Updating Camel Runtime Version

When updating to a new Camel version (like the current `feat/update-camel-runtime` branch):

1. Update `pom.xml`:
   - `<version.camel>` for main Camel version
   - `<version.camel.quarkus>` for Quarkus
   - `<version.camel-kamelets>` for Kamelets

2. Update `index.js`:
   - Add new version to appropriate CATALOGS array (Main/Quarkus/SpringBoot)
   - Update `KAMELETS_VERSION` if changed

3. Rebuild and commit catalog changes:
   ```bash
   yarn build
   git add catalog pom.xml index.js
   git commit -m "feat(catalog): Update Camel runtime to X.Y.Z"
   ```

### Important: Committing Generated Catalogs

The CI workflow (`build-publish.yml`) enforces that generated catalog files in `catalog/` must be committed. After running `yarn build`, always commit the changes to `catalog/`:

```bash
yarn build
git add catalog
git commit -m "chore: update generated catalog"
```

This ensures the npm package includes pre-built catalogs without requiring consumers to run the Java build.

### Running a Single Test

```bash
./mvnw test -Dtest=MainTest
./mvnw test -Dtest=CatalogGeneratorTest#testGenerateCatalog
```

### Testing Java Changes Without Full Build

```bash
./mvnw clean package -DskipTests
java -jar target/catalog-generator-0.0.1-SNAPSHOT.jar -o ./test-output -n "Test Catalog" -k 4.15.0 -m 4.15.0 -q 3.27.0 -s 4.14.1
```

## Publishing

The project uses Lerna for version management and NPM publishing:

```bash
yarn publish
```

This is automated on the `main` branch via GitHub Actions (`.github/workflows/build-publish.yml`).

## Common Issues

### "Catalog is out of date" CI Failure

If CI fails with uncommitted changes:
1. Run `yarn build` locally
2. Commit the changes to the `catalog/` folder
3. Push the updated catalog files

### Java Version Errors

This project requires Java 17+. Verify with:
```bash
java -version
./mvnw -version
```

### TypeScript Generation Fails

Ensure the Java JAR has been built and catalogs generated before running `yarn build:ts`. The TypeScript generator depends on JSON schema files in `dist/camel-catalog/`.
