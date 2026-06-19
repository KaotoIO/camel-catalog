# Kaoto Camel Catalog AGENTS.md

This file provides guidance to agents when working with code in this repository.

## Project Overview

This is the Kaoto Camel Catalog Generator - a hybrid Java/TypeScript project that generates comprehensive Apache Camel catalogs for the Kaoto visual integration editor. The project uses Maven to build a Java-based catalog generator JAR, then uses Node.js/Yarn scripts to execute the generator and produce TypeScript type definitions from JSON schemas.

**Relationship to Kaoto**: This project generates the catalog data consumed by the [Kaoto visual editor](https://github.com/KaotoIO/kaoto). When Camel versions are updated here, the Kaoto UI can discover and use new components, EIPs, and Kamelets.

---

## AI Agent Contribution Guidelines

AI agents are welcome contributors to Kaoto Camel Catalog. When contributing code via AI assistance:

### Human Oversight Required

- AI agents **cannot** submit PRs independently
- A human must review, approve, and sign all AI-generated code
- The human reviewer is responsible for:
  - Code quality and correctness
  - Responding to maintainer feedback
  - Following up on PR comments

### Git Workflow

- **Never push to branches you didn't create** - If a contributor's PR needs changes, suggest them via review comments, but do not push to their branch without explicit permission
- **Use forks** - Prefer pushing branches to your fork rather than the main repository to avoid cluttering the branch list with uncleaned branches
- **Branch naming** - Use descriptive names with issue numbers when possible: `fix/issue-123-catalog-generation` or `feat/camel-4.16-support`
- **Branch cleanup** - Delete branches after the PR is merged or rejected

### PR Guidelines

- **Volume limit** - Do not open more than 10 PRs per day per operator to ensure human reviewers can keep up
- **Quality over quantity** - Fewer well-tested PRs are better than many shallow ones
- **Active follow-up required** - PRs without response after **2 weeks** will be closed
- If you need more time, communicate with maintainers

### Quality Standards

Before submitting AI-generated PRs:

```bash
yarn lint
yarn lint:fix
./mvnw test
yarn build  # Ensure full catalog generation works
```

- Ensure all tests pass
- Fix any linter errors
- Verify catalog generation completes successfully
- Test TypeScript type generation works
- Avoid introducing new static code analysis issues: code smells, maintainability regressions, security flows, deprecated code usage
- Changes should aim to preserve or improve overall code quality

### Disclosure and Documentation

- Disclosing AI tool usage is **optional but recommended**
- Mentioning your AI tool helps us improve AGENTS.md
- If you discover gaps in our agent documentation, please suggest improvements

For complete contribution guidelines, see [CONTRIBUTING.md](CONTRIBUTING.md) (if available).

---

## Quick Start

```bash
# Install dependencies
yarn install

# Full build (Maven JAR + catalog generation + TypeScript types)
yarn build

# Before committing
yarn lint
./mvnw test
yarn build  # Verify catalog generation works
```

---

## Requirements

- **Java**: 17+
- **Node.js**: >= 18.x
- **Maven**: 3.8+ (or use included `./mvnw`)
- **Yarn**: 4.x

Verify your setup:
```bash
java -version
node -version
./mvnw -version
```

---

## Quick Reference

| Task | Command |
|------|---------|
| Full build | `yarn build` |
| Java JAR only | `yarn build:mvn` or `./mvnw clean package` |
| Catalog generation | `yarn build:catalog` |
| TypeScript types | `yarn build:ts` |
| Run all tests | `./mvnw test` |
| Run specific test | `./mvnw test -Dtest=ClassName` |
| Lint check | `yarn lint` |
| Lint fix | `yarn lint:fix` |

---

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

---

## Architecture Overview

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

---

## Development Workflow

### Before Committing

Always run before opening PRs:

```bash
yarn lint           # Check code style
yarn lint:fix       # Fix auto-fixable issues
./mvnw test         # Run Java tests
yarn build          # Verify full build works
git add catalog     # Commit generated catalogs (required!)
```

### Updating Camel Runtime Version

When updating to a new Camel version:

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

### Testing Changes

```bash
# Run all tests
./mvnw test

# Run specific test class
./mvnw test -Dtest=MainTest

# Run specific test method
./mvnw test -Dtest=CatalogGeneratorTest#testGenerateCatalog

# Test catalog generation without full build
./mvnw clean package -DskipTests
java -jar target/catalog-generator-0.0.1-SNAPSHOT.jar \
  -o ./test-output \
  -n "Test Catalog" \
  -k 4.15.0 \
  -m 4.15.0 \
  -q 3.27.0 \
  -s 4.14.1
```

### Testing a Specific Runtime

To test catalog generation for a specific runtime:

```bash
# Build JAR first
./mvnw clean package

# Generate Quarkus catalog only
yarn build:catalog -- --runtime quarkus --version 3.27.0

# Generate Main runtime catalog
yarn build:catalog -- --runtime main --version 4.15.0
```

---

## Common Development Scenarios

### Scenario: Adding Support for Camel 4.16.0

1. Update `pom.xml`:
   ```xml
   <version.camel>4.16.0</version.camel>
   ```

2. Update `index.js`:
   ```javascript
   CATALOGS.Main = [
     // ... existing versions
     { name: 'Apache Camel 4.16.0', version: '4.16.0', runtime: 'Main' }
   ]
   ```

3. Build and test:
   ```bash
   yarn build
   ./mvnw test
   ```

4. Verify catalog was generated:
   ```bash
   ls -la catalog/Main/4.16.0/
   ```

### Scenario: Debugging Missing Component

1. Check if component exists in upstream Camel:
   ```bash
   # Search in target directory after build
   grep -r "component-name" target/
   ```

2. Run generator with verbose output:
   ```bash
   java -jar target/catalog-generator-*.jar --debug -o ./debug-output ...
   ```

3. Check generated catalog JSON:
   ```bash
   cat dist/camel-catalog/Main/4.15.0/camel-catalog-*.json | jq '.components'
   ```

### Scenario: TypeScript Generation Fails

1. Ensure Java build completed:
   ```bash
   ls -la target/catalog-generator-*.jar
   ```

2. Ensure catalogs were generated:
   ```bash
   ls -la dist/camel-catalog/
   ```

3. Run TypeScript generation separately:
   ```bash
   yarn build:ts
   ```

4. Check for JSON schema validation errors in the output

---

## Troubleshooting

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

### Maven Resolution Failures

If Maven cannot resolve Camel artifacts:
1. Check your internet connection
2. Verify the version exists in Maven Central or Red Hat repositories
3. Try clearing Maven cache:
   ```bash
   rm -rf ~/.m2/repository/org/apache/camel
   ./mvnw clean package
   ```

### TypeScript Generation Fails

Ensure the Java JAR has been built and catalogs generated before running `yarn build:ts`. The TypeScript generator depends on JSON schema files in `dist/camel-catalog/`.

### Build Hangs or Times Out

The catalog generator downloads many Maven artifacts. First build may take 5-10 minutes. Subsequent builds are faster due to Maven cache.

---

## Publishing

The project uses Lerna for version management and NPM publishing:

```bash
yarn publish
```

This is automated on the `main` branch via GitHub Actions (`.github/workflows/build-publish.yml`).

---

## Advanced Architecture Details

For deep technical details on the following topics, see the sections below. Most developers won't need this level of detail for typical contributions.

<details>
<summary><strong>Maven Artifact Loading and Resource Management</strong> (click to expand)</summary>

The project uses a dynamic Maven artifact resolution system to load versioned Camel catalogs at runtime without requiring them to be compile-time dependencies. This allows generating catalogs for multiple Camel versions from a single JAR.

### KaotoMavenVersionManager

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

### ResourceLoader

`ResourceLoader` provides utilities to extract resources from the dynamically loaded JARs:

- **Single Resource Loading** (`getResourceAsString`): Loads individual files like `camel-yaml-dsl.json` from the classpath
- **Bulk Folder Loading** (`loadResourcesFromFolderAsString`): Loads all files with a specific suffix from a folder in JARs or filesystem
  - Example: Load all `.kamelet.yaml` files from the `kamelets/` folder inside the kamelets JAR
  - Handles both JAR protocol (`jar:file:`) and file protocol (`file:`) resources
  - Returns a map of filename → content for processing

**How ResourceLoader handles JAR vs filesystem**:
- For JAR resources: Uses `JarURLConnection` to iterate `JarEntry` objects and extract matching files
- For file resources: Uses `Files.walk()` to traverse directory structure (useful during development)

### CamelCatalogVersionLoader

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

</details>

<details>
<summary><strong>Complete Flow: Version to Catalog Generation</strong> (click to expand)</summary>

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

</details>

---

## Repository Structure

```
camel-catalog/
├── src/
│   └── main/java/io/kaoto/camelcatalog/
│       ├── generator/          # Core catalog generation logic
│       ├── generators/         # Specific generators (Component, EIP, CRD, etc.)
│       ├── maven/              # Maven artifact loading and resource management
│       ├── model/              # Data model classes (→ JSON → TypeScript)
│       └── Main.java           # CLI entry point
├── scripts/
│   ├── build-catalogs.mjs      # Orchestrates Java JAR execution
│   └── json-schema-to-typescript.mjs  # TypeScript type generation
├── catalog/                    # Generated catalogs (committed to repo)
├── dist/                       # Build output (not committed)
├── target/                     # Maven build output
├── pom.xml                     # Maven configuration
├── index.js                    # Version configuration
└── package.json                # Node.js/Yarn configuration
```

---

## Code Style and Conventions

- **Java**: Standard Java code style, enforced by Maven Checkstyle (if configured)
- **JavaScript/TypeScript**: Prettier + ESLint
- **Prettier**: semicolons required, single quotes, width 120, 2-space indent, trailing commas

---

## Additional Resources

- [Kaoto Main Repository](https://github.com/KaotoIO/kaoto) - The visual editor that consumes these catalogs
- [Apache Camel Documentation](https://camel.apache.org/) - Upstream Camel documentation
- [Kamelets Catalog](https://camel.apache.org/camel-kamelets/) - Kamelet connector documentation
