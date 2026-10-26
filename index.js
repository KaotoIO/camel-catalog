/**
 * Catalog and Kamelet versions consumed by the generator.
 *
 * Quarkus entries are Quarkus *platform* versions (what `camel run
 * --quarkus-version` takes); every other list holds the runtime's own version.
 * Provenance (community vs. Red Hat) is inferred from the `redhat-` marker.
 *
 * How versions are sourced, the Quarkus platform -> camel-quarkus mapping, and
 * the registry URLs for verifying them: see ./docs/CATALOG_VERSIONS.md
 */

const CATALOGS = {
  Main: [
    '4.23.0',
    '4.22.2', // LTS
    '4.18.4', // LTS
    '4.14.9', // LTS
    '4.10.7', // LTS
    '4.18.3.redhat-00005',
    // '4.14.4.redhat-00008', // removed because there is a bug which prevents the version to be used with --console flag of Camel CLI / Camel Launcher
    '4.10.7.redhat-00009',
  ],
  Quarkus: [
    '3.40.1', // 4.22.1 LTS
    '3.33.4', // 4.18.4 LTS
    '3.27.5', // 4.14.8 LTS
    '3.20.6', // 4.10.8 LTS
    '3.33.3.redhat-00001', // 4.18
    // '3.27.3.redhat-00003', // removed because there is a bug which prevents the version to be used with --console flag of Camel CLI / Camel Launcher
    '3.20.6.redhat-00004', // 4.10
  ],
  SpringBoot: [
    '4.23.0',
    '4.22.2', // LTS
    '4.18.5', // LTS
    '4.14.9', // LTS
    '4.10.9', // LTS
    '4.18.3.redhat-00006',
    // '4.14.4.redhat-00010', // removed because there is a bug which prevents the version to be used with --console flag of Camel CLI / Camel Launcher
    '4.10.7.redhat-00013',
  ],
  Citrus: ['4.10.3', '5.0.0'],
  XSLT: ['3.0'],
};

const KAMELETS_VERSION = '4.22.2';

const CAMEL_CLI_VERSION = '4.22.2';

module.exports = { CATALOGS, KAMELETS_VERSION, CAMEL_CLI_VERSION };
